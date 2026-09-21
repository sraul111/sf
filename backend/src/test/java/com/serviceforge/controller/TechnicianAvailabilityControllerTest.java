package com.serviceforge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TechnicianAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void technicianCanBeBookedForAJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Acme Field Office",
                                  "startTime": "2026-10-01T09:00:00",
                                  "endTime": "2026-10-01T11:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technicianId").value(3))
                .andExpect(jsonPath("$.customerName").value("Acme Field Office"));
    }

    @Test
    void techniciansJobsCanBeListed() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Northwind Site",
                                  "startTime": "2026-10-02T13:00:00",
                                  "endTime": "2026-10-02T14:00:00"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/technicians/3/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].technicianId").value(3))
                .andExpect(jsonPath("$[0].customerName").value("Northwind Site"));
    }

    @Test
    void overlappingBookingForSameTechnicianIsRejectedWithConflictError() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Existing Appointment",
                                  "startTime": "2026-10-03T10:00:00",
                                  "endTime": "2026-10-03T12:00:00"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Overlapping Appointment",
                                  "startTime": "2026-10-03T11:00:00",
                                  "endTime": "2026-10-03T13:00:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    private void bookExistingReferenceJob() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Existing Appointment",
                                  "startTime": "2026-11-01T16:23:00",
                                  "endTime": "2026-11-01T19:25:00"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void identicalRangeIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Identical Range",
                                  "startTime": "2026-11-01T16:23:00",
                                  "endTime": "2026-11-01T19:25:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    @Test
    void rangeStartingInsideExistingIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Starts Inside Existing",
                                  "startTime": "2026-11-01T17:24:00",
                                  "endTime": "2026-11-01T20:27:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    @Test
    void rangeFullyContainingExistingIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Fully Contains Existing",
                                  "startTime": "2026-11-01T15:00:00",
                                  "endTime": "2026-11-01T20:30:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    @Test
    void rangeEndingInsideExistingIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Ends Inside Existing",
                                  "startTime": "2026-11-01T15:00:00",
                                  "endTime": "2026-11-01T17:00:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    @Test
    void nonOverlappingRangeRespectingTravelBufferIsStillAccepted() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Genuinely Non-Overlapping",
                                  "startTime": "2026-11-01T20:10:00",
                                  "endTime": "2026-11-01T21:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Genuinely Non-Overlapping"));
    }

    @Test
    void rangeStartingLessThanBufferAfterExistingEndIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Starts Too Soon After Existing",
                                  "startTime": "2026-11-01T19:40:00",
                                  "endTime": "2026-11-01T20:30:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }

    @Test
    void rangeEndingLessThanBufferBeforeExistingStartIsRejectedWithConflictError() throws Exception {
        bookExistingReferenceJob();

        mockMvc.perform(post("/api/jobs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "technicianId": 3,
                                  "customerName": "Ends Too Close Before Existing",
                                  "startTime": "2026-11-01T15:00:00",
                                  "endTime": "2026-11-01T15:50:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("conflicting job")));
    }
}