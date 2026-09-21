package com.serviceforge.service;

import com.serviceforge.data.MockDataStore;
import com.serviceforge.model.Job;
import com.serviceforge.model.JobStatus;
import com.serviceforge.model.Technician;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TechnicianAvailabilityService {

    public static final int TRAVEL_BUFFER_MINUTES = 45;

    private final MockDataStore dataStore;

    public TechnicianAvailabilityService(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Technician> getAllTechnicians() { return dataStore.getAllTechnicians(); }
    public Optional<Technician> findTechnician(Long technicianId) { return dataStore.findTechnician(technicianId); }
    public List<Job> getJobsForTechnician(Long technicianId) { return dataStore.getJobsForTechnician(technicianId); }

    public Job bookJob(Long technicianId, String customerName, LocalDateTime startTime, LocalDateTime endTime) {
        Technician technician = dataStore.findTechnician(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("No technician with id " + technicianId));
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Job start time must be before its end time");
        }

        boolean hasConflict = dataStore.getJobsForTechnician(technicianId).stream()
            .anyMatch(existing -> startTime.isBefore(existing.getEndTime().plusMinutes(TRAVEL_BUFFER_MINUTES))
                    && existing.getStartTime().minusMinutes(TRAVEL_BUFFER_MINUTES).isBefore(endTime));
        if (hasConflict) {
            throw new IllegalStateException("Technician " + technician.getName()
                    + " already has a conflicting job near " + startTime);
        }

        return dataStore.save(new Job(dataStore.nextJobId(), technicianId, customerName,
                startTime, endTime, JobStatus.SCHEDULED));
    }

}