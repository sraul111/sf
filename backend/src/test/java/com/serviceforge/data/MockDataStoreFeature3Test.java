package com.serviceforge.data;

import com.serviceforge.model.Job;
import com.serviceforge.model.Part;
import com.serviceforge.model.Technician;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MockDataStoreFeature3Test {

    @Autowired
    private MockDataStore dataStore;

    // DoD 4
    @Test
    void seededTechniciansHaveExactSkillsAndUnchangedIdentity() {
        List<Technician> technicians = dataStore.getAllTechnicians();
        assertEquals(3, technicians.size());

        assertTechnician(technicians.get(0), 1L, "Alex Rivera", "North", List.of("HVAC", "Electrical"));
        assertTechnician(technicians.get(1), 2L, "Jordan Blake", "South", List.of("Plumbing", "HVAC"));
        assertTechnician(technicians.get(2), 3L, "Sam Okonkwo", "East", List.of("Electrical", "Plumbing"));
    }

    // DoD 5
    @Test
    void seededJobsHaveRequiredSkillHeldByTheirTechnician() {
        Map<String, String> expected = Map.of(
                "Acme Corp", "HVAC",
                "Northwind Traders", "Electrical",
                "Globex", "Plumbing");
        Map<String, Long> expectedTechnician = Map.of(
                "Acme Corp", 1L,
                "Northwind Traders", 1L,
                "Globex", 2L);

        List<Job> seeded = dataStore.getAllTechnicians().stream()
                .flatMap(technician -> dataStore.getJobsForTechnician(technician.getId()).stream())
                .filter(job -> expected.containsKey(job.getCustomerName()))
                .collect(Collectors.toList());
        assertEquals(3, seeded.size());

        for (Job job : seeded) {
            assertEquals(expectedTechnician.get(job.getCustomerName()), job.getTechnicianId(), job.getCustomerName());
            assertEquals(expected.get(job.getCustomerName()), job.getRequiredSkill(), job.getCustomerName());
            Technician technician = dataStore.findTechnician(job.getTechnicianId()).orElseThrow();
            assertTrue(technician.getSkills().contains(job.getRequiredSkill()),
                    job.getCustomerName() + " requires a skill its technician lacks");
        }
    }

    // DoD 6
    @Test
    void seededPartsHaveUniqueNonBlankSkusAndAreFindableBySku() {
        List<Part> parts = dataStore.getAllParts();
        Set<String> skus = new HashSet<>();
        for (Part part : parts) {
            assertNotNull(part.getSku());
            assertFalse(part.getSku().isBlank());
            assertTrue(skus.add(part.getSku()), "duplicate SKU " + part.getSku());
        }
        assertTrue(skus.containsAll(Set.of("CMP-CLUTCH", "FLT-16", "BRK-20A", "VLV-BALL-075")));

        assertTrue(dataStore.findPartBySku("FLT-16").isPresent());
        assertTrue(dataStore.findPartBySku("NOPE").isEmpty());
    }

    private void assertTechnician(Technician technician, Long id, String name, String region, List<String> skills) {
        assertEquals(id, technician.getId());
        assertEquals(name, technician.getName());
        assertEquals(region, technician.getRegion());
        assertEquals(skills, technician.getSkills());
    }
}
