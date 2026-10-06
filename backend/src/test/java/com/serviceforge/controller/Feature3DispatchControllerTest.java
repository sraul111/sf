package com.serviceforge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceforge.service.TechnicianAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Feature3DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // DoD 7
    @Test
    void technicianListExposesSkillsArrays() throws Exception {
        String body = mockMvc.perform(get("/api/technicians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 1)].skills[*]", contains("HVAC", "Electrical")))
                .andReturn().getResponse().getContentAsString();

        JsonNode technicians = objectMapper.readTree(body);
        assertTrue(technicians.isArray() && technicians.size() > 0);
        for (JsonNode technician : technicians) {
            assertTrue(technician.path("skills").isArray(), "technician " + technician.path("id") + " has no skills array");
        }
    }

    // DoD 8
    @Test
    void singleTechnicianExposesSkillsAndMissingTechnicianStillReturns404() throws Exception {
        mockMvc.perform(get("/api/technicians/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[*]", contains("HVAC", "Electrical")));

        mockMvc.perform(get("/api/technicians/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("No technician with id 9999")));
    }

    // DoD 9
    @Test
    void technicianJobsExposeSeededRequiredSkill() throws Exception {
        mockMvc.perform(get("/api/technicians/1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerName == 'Acme Corp')].requiredSkill", contains("HVAC")))
                .andExpect(jsonPath("$[?(@.customerName == 'Northwind Traders')].requiredSkill", contains("Electrical")));

        mockMvc.perform(get("/api/technicians/2/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.customerName == 'Globex')].requiredSkill", contains("Plumbing")));
    }

    // DoD 10
    @Test
    void bookingWithRequiredSkillEchoesIt() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Skill Echo",
                                  "startTime": "2026-12-01T09:00:00",
                                  "endTime": "2026-12-01T10:00:00",
                                  "requiredSkill": "HVAC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredSkill").value("HVAC"));
    }

    // DoD 11
    @Test
    void bookingWithoutRequiredSkillReturnsNullRequiredSkill() throws Exception {
        String body = mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Legacy Shape",
                                  "startTime": "2026-12-02T09:00:00",
                                  "endTime": "2026-12-02T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredSkill").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        JsonNode job = objectMapper.readTree(body);
        assertTrue(job.has("requiredSkill"), "requiredSkill key missing from response");
        assertTrue(job.get("requiredSkill").isNull());
    }

    // DoD 12
    @Test
    void partsEndpointReturnsIdSkuAndName() throws Exception {
        String body = mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem("CMP-CLUTCH")))
                .andExpect(jsonPath("$[*].sku", hasItem("FLT-16")))
                .andReturn().getResponse().getContentAsString();

        JsonNode parts = objectMapper.readTree(body);
        assertTrue(parts.isArray());
        Set<String> skus = new HashSet<>();
        for (JsonNode part : parts) {
            assertTrue(part.has("id") && part.has("sku") && part.has("name"), "part missing id/sku/name: " + part);
            skus.add(part.get("sku").asText());
        }
        assertTrue(skus.containsAll(Set.of("CMP-CLUTCH", "FLT-16")));
    }

    // DoD 14 — reference job [16:23, 19:25) mirrors TechnicianAvailabilityControllerTest; tech 3 lacks HVAC on purpose
    private void bookReferenceJobWithSkill() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Existing Skilled Appointment",
                                  "startTime": "2026-11-01T16:23:00",
                                  "endTime": "2026-11-01T19:25:00",
                                  "requiredSkill": "Electrical"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    // DoD 14
    @Test
    void bookingWithRequiredSkillStartingInsideBufferAfterExistingIsRejected() throws Exception {
        bookReferenceJobWithSkill();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Skilled But Too Soon",
                                  "startTime": "2026-11-01T19:40:00",
                                  "endTime": "2026-11-01T20:30:00",
                                  "requiredSkill": "Electrical"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("conflicting job")));
    }

    // DoD 14
    @Test
    void bookingWithRequiredSkillEndingInsideBufferBeforeExistingIsRejected() throws Exception {
        bookReferenceJobWithSkill();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Skilled But Too Close Before",
                                  "startTime": "2026-11-01T15:00:00",
                                  "endTime": "2026-11-01T15:50:00",
                                  "requiredSkill": "Plumbing"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("conflicting job")));
    }

    // DoD 14 — 20:10 is exactly 45 min after 19:25, which Feature 1 decisions allow
    @Test
    void bookingWithRequiredSkillRespectingBufferIsAccepted() throws Exception {
        bookReferenceJobWithSkill();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Skilled And Buffered",
                                  "startTime": "2026-11-01T20:10:00",
                                  "endTime": "2026-11-01T21:00:00",
                                  "requiredSkill": "HVAC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredSkill").value("HVAC"));
    }

    // DoD 14 — skilled existing job still blocks an unskilled request, so skill plays no part in conflicts
    @Test
    void bookingWithoutRequiredSkillIsStillRejectedAgainstSkilledExistingJob() throws Exception {
        bookReferenceJobWithSkill();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Unskilled Identical Range",
                                  "startTime": "2026-11-01T16:23:00",
                                  "endTime": "2026-11-01T19:25:00"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    // DoD 15
    @Test
    void travelBufferIsStill45Minutes() {
        assertEquals(45, TechnicianAvailabilityService.TRAVEL_BUFFER_MINUTES);
    }

    @Test
    void reservationSuccessReturns200AndReservationResult() throws Exception {
        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 12,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.result.sku").value("CMP-CLUTCH"))
                .andExpect(jsonPath("$.result.quantity").value(1))
                .andExpect(jsonPath("$.result.jobId").value(12))
                .andExpect(jsonPath("$.result.reservationId").value(org.hamcrest.Matchers.startsWith("resv-")));
    }

    @Test
    void reservationInvalidQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 12,
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("invalid_input"));
    }

    @Test
    void reservationUnknownSkuReturns404() throws Exception {
        mockMvc.perform(post("/api/parts/UNKNOWN-SKU/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 12,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void reservationInsufficientStockReturns409() throws Exception {
        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": 12,
                                  "quantity": 999
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("conflict"));
    }
}
