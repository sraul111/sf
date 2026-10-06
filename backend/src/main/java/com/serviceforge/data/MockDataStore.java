package com.serviceforge.data;

import com.serviceforge.model.Job;
import com.serviceforge.model.JobStatus;
import com.serviceforge.model.Part;
import com.serviceforge.model.Technician;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class MockDataStore {

    private final List<Technician> technicians = new ArrayList<>();
    private final List<Job> jobs = new ArrayList<>();
    private final List<Part> parts = new ArrayList<>();
    private final AtomicLong jobIdSequence = new AtomicLong(1);

    @PostConstruct
    public void seed() {
        technicians.add(new Technician(1L, "Alex Rivera" ,"North", List.of("HVAC", "Electrical")));
        technicians.add(new Technician(2L, "Jordan Blake", "South", List.of("Plumbing", "HVAC")));
        technicians.add(new Technician(3L, "Sam Okonkwo", "East", List.of("Electrical", "Plumbing")));

        LocalDateTime today = LocalDateTime.now();
        jobs.add(new Job(nextJobId(), 1L, "Acme Corp",
                today.withHour(9).withMinute(0).withSecond(0).withNano(0),
                today.withHour(11).withMinute(0).withSecond(0).withNano(0), JobStatus.SCHEDULED, "HVAC"));
        jobs.add(new Job(nextJobId(), 1L, "Northwind Traders",
                today.withHour(14).withMinute(0).withSecond(0).withNano(0),
                today.withHour(15).withMinute(30).withSecond(0).withNano(0), JobStatus.SCHEDULED, "Electrical"));
        jobs.add(new Job(nextJobId(), 2L, "Globex",
                today.withHour(10).withMinute(0).withSecond(0).withNano(0),
                today.withHour(12).withMinute(0).withSecond(0).withNano(0), JobStatus.SCHEDULED, "Plumbing"));

        parts.add(new Part(1L, "CMP-CLUTCH", "AC Compressor Clutch", 3));
        parts.add(new Part(2L, "FLT-16", "16x25x1 Air Filter", 10));
        parts.add(new Part(3L, "BRK-20A", "20A Single-Pole Circuit Breaker", 5));
        parts.add(new Part(4L, "VLV-BALL-075", "3/4in Brass Ball Valve", 3));
    }

    public List<Technician> getAllTechnicians() { return Collections.unmodifiableList(technicians); }

    public Optional<Technician> findTechnician(Long id) {
        return technicians.stream().filter(technician -> technician.getId().equals(id)).findFirst();
    }

    public List<Job> getJobsForTechnician(Long technicianId) {
        return jobs.stream().filter(job -> job.getTechnicianId().equals(technicianId)).collect(Collectors.toList());
    }

    public Job save(Job job) {
        jobs.add(job);
        return job;
    }

    public Long nextJobId() { return jobIdSequence.getAndIncrement(); }

    public List<Part> getAllParts() { return Collections.unmodifiableList(parts); }

    public Optional<Part> findPartBySku(String sku) {
        if (sku == null) {
            return Optional.empty();
        }
        return parts.stream().filter(part -> part.getSku().equalsIgnoreCase(sku)).findFirst();
    }
}