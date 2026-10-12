package com.serviceforge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceforge.data.MockDataStore;
import com.serviceforge.model.Job;
import com.serviceforge.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Feature2PartsReservationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockDataStore dataStore;

    @Test
    void ac2bAggregateInsufficientStockReturnsConflictWithoutPartialMutation() throws Exception {
        int before = stock("CMP-CLUTCH");

        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 1, "quantity": 4}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));

        assertEquals(before, stock("CMP-CLUTCH"));
    }

    @Test
    void ec1NegativeIssueQuantityReturnsBadRequestWithoutMutation() throws Exception {
        int before = stock("CMP-CLUTCH");

        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 1, "quantity": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_input"));

        assertEquals(before, stock("CMP-CLUTCH"));
    }

    @Test
    void ec2LowercaseSkuIssueReturnsCanonicalSku() throws Exception {
        mockMvc.perform(post("/api/parts/cmp-clutch/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 1, "quantity": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sku").value("CMP-CLUTCH"));
    }

    @Test
    void ec3MissingJobIssueReturnsNotFoundWithoutChangingStock() throws Exception {
        int before = stock("CMP-CLUTCH");

        int responseStatus = mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 9999, "quantity": 1}
                                """))
            .andReturn().getResponse().getStatus();

        assertAll(
            () -> assertEquals(404, responseStatus),
            () -> assertEquals(before, stock("CMP-CLUTCH")));
    }

    @Test
    void ec5CompletedAndCancelledJobIssuesReturnConflictWithoutChangingStock() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 9, 0);
        Job completed = new Job(9001L, 1L, "Completed fixture", start, start.plusHours(1), JobStatus.COMPLETED);
        Job cancelled = new Job(9002L, 2L, "Cancelled fixture", start.plusDays(1), start.plusDays(1).plusHours(1), JobStatus.CANCELLED);
        dataStore.save(completed);
        dataStore.save(cancelled);
        int before = stock("CMP-CLUTCH");

        ArrayList<Integer> responseStatuses = new ArrayList<>();
        for (long jobId : new long[] { completed.getId(), cancelled.getId() }) {
            responseStatuses.add(mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"jobId": %d, "quantity": 1}
                                    """.formatted(jobId)))
                .andReturn().getResponse().getStatus());
        }

        assertAll(
            () -> assertEquals(java.util.List.of(409, 409), responseStatuses),
            () -> assertEquals(before, stock("CMP-CLUTCH")));
    }

    @Test
    void ec6ConcurrentAggregateReservationsCannotExceedSeededStock() throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = reserveAfterBarrier(startTogether, 101L);
            Callable<Integer> second = reserveAfterBarrier(startTogether, 102L);
            Future<Integer> firstStatus = executor.submit(first);
            Future<Integer> secondStatus = executor.submit(second);

            Set<Integer> statuses = Set.of(firstStatus.get(), secondStatus.get());
            assertEquals(Set.of(200, 409), statuses);
            assertEquals(0, stock("CMP-CLUTCH"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void ec7ExactAggregateCapacityExhaustsStockWithoutGoingNegative() throws Exception {
        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 1, "quantity": 3}
                                """))
                .andExpect(status().isOk());
        assertEquals(0, stock("CMP-CLUTCH"));

        mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"jobId": 2, "quantity": 1}
                                """))
                .andExpect(status().isConflict());
        assertEquals(0, stock("CMP-CLUTCH"));
    }

    private Callable<Integer> reserveAfterBarrier(CyclicBarrier barrier, long jobId) {
        return () -> {
            barrier.await();
            return mockMvc.perform(post("/api/parts/CMP-CLUTCH/reservations")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"jobId": %d, "quantity": 3}
                                    """.formatted(jobId)))
                    .andReturn().getResponse().getStatus();
        };
    }

    private int stock(String sku) throws Exception {
        String response = mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode parts = objectMapper.readTree(response);
        for (JsonNode part : parts) {
            if (sku.equals(part.path("sku").asText())) {
                return part.path("totalQuantity").asInt();
            }
        }
        throw new AssertionError("Missing part " + sku);
    }
}