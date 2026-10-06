package com.serviceforge.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Feature3ModelTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 1, 11, 0);

    // DoD 1
    @Test
    void technicianFourArgConstructorAndSkillsAccessors() {
        Technician technician = new Technician(10L, "Pat Lee", "West", List.of("HVAC", "Electrical"));
        assertEquals(List.of("HVAC", "Electrical"), technician.getSkills());

        technician.setSkills(List.of("Plumbing"));
        assertEquals(List.of("Plumbing"), technician.getSkills());
    }

    // DoD 1
    @Test
    void technicianThreeArgConstructorProducesEmptySkillsNotNull() {
        Technician technician = new Technician(11L, "Pat Lee", "West");
        assertNotNull(technician.getSkills());
        assertTrue(technician.getSkills().isEmpty());
    }

    // DoD 2
    @Test
    void jobSevenArgConstructorCarriesRequiredSkill() {
        Job job = new Job(1L, 1L, "Acme", START, END, JobStatus.SCHEDULED, "HVAC");
        assertEquals("HVAC", job.getRequiredSkill());
    }

    // DoD 2
    @Test
    void jobSixArgConstructorProducesNullRequiredSkill() {
        Job job = new Job(1L, 1L, "Acme", START, END, JobStatus.SCHEDULED);
        assertNull(job.getRequiredSkill());
    }

    // DoD 3
    @Test
    void partHasIdSkuNameAccessorsAndThreeArgConstructor() {
        Part part = new Part(1L, "FLT-16", "16x25x1 Air Filter");
        assertEquals(1L, part.getId());
        assertEquals("FLT-16", part.getSku());
        assertEquals("16x25x1 Air Filter", part.getName());

        part.setSku("FLT-20");
        part.setName("20x25x1 Air Filter");
        assertEquals("FLT-20", part.getSku());
        assertEquals("20x25x1 Air Filter", part.getName());
    }
}
