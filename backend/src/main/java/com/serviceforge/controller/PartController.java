package com.serviceforge.controller;

import com.serviceforge.dto.ApiError;
import com.serviceforge.model.Part;
import com.serviceforge.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;
    private final AtomicLong reservationSequence = new AtomicLong(1);

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public ResponseEntity<List<Part>> getAllParts() {
        return ResponseEntity.ok(partService.getAllParts());
    }

    @GetMapping("/{sku}")
    public ResponseEntity<?> getPartBySku(@PathVariable String sku) {
        return partService.findPartBySku(sku)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(new ToolErrorResponse(
                        404,
                        "not_found",
                        "Part not found for SKU: " + sku,
                        Map.of("sku", sku))));
    }

    @PostMapping("/{sku}/reservations")
    public ResponseEntity<?> reservePart(
            @PathVariable String sku,
            @RequestBody(required = false) ReservationRequest request) {
        if (request == null || request.getQuantity() == null) {
            return ResponseEntity.status(400).body(new ToolErrorResponse(
                    400,
                    "invalid_input",
                    "Quantity must be a positive integer",
                    Map.of("field", "quantity")));
        }
        if (request.getQuantity() <= 0) {
            return ResponseEntity.status(400).body(new ToolErrorResponse(
                    400,
                    "invalid_input",
                    "Quantity must be a positive integer",
                    Map.of("field", "quantity")));
        }

        try {
            Part part = partService.reserve(sku, request.getQuantity());
            String reservationId = "resv-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "-" + String.format("%04d", reservationSequence.getAndIncrement());
            ReservationResult result = new ReservationResult(
                    reservationId,
                    request.getJobId(),
                    part.getSku(),
                    request.getQuantity());
            return ResponseEntity.ok(new ReservationSuccessResponse(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(new ToolErrorResponse(
                    400,
                    "invalid_input",
                    e.getMessage(),
                    Map.of("field", "quantity")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ToolErrorResponse(
                    404,
                    "not_found",
                    "Part not found for SKU: " + sku,
                    Map.of("sku", sku)));
        } catch (IllegalStateException e) {
            int total = partService.findPartBySku(sku).map(Part::getTotalQuantity).orElse(0);
            return ResponseEntity.status(409).body(new ToolErrorResponse(
                    409,
                    "conflict",
                    "Insufficient stock for SKU: " + sku,
                    Map.of("total", total, "requested", request.getQuantity(), "peakReserved", 0)));
        }
    }

    public static class ReservationRequest {
        private Long jobId;
        private Integer quantity;

        public ReservationRequest() {}

        public ReservationRequest(Long jobId, Integer quantity) {
            this.jobId = jobId;
            this.quantity = quantity;
        }

        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class ReservationSuccessResponse {
        private final boolean ok = true;
        private final ReservationResult result;

        public ReservationSuccessResponse(ReservationResult result) {
            this.result = result;
        }

        public boolean isOk() { return ok; }
        public ReservationResult getResult() { return result; }
    }

    public static class ReservationResult {
        private final String reservationId;
        private final Long jobId;
        private final String sku;
        private final int quantity;

        public ReservationResult(String reservationId, Long jobId, String sku, int quantity) {
            this.reservationId = reservationId;
            this.jobId = jobId;
            this.sku = sku;
            this.quantity = quantity;
        }

        public String getReservationId() { return reservationId; }
        public Long getJobId() { return jobId; }
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
    }

    public static class ToolErrorResponse {
        private final boolean ok = false;
        private final int status;
        private final String code;
        private final String message;
        private final Map<String, Object> details;

        public ToolErrorResponse(int status, String code, String message, Map<String, Object> details) {
            this.status = status;
            this.code = code;
            this.message = message;
            this.details = details != null ? details : Collections.emptyMap();
        }

        public boolean isOk() { return ok; }
        public int getStatus() { return status; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
    }
}
