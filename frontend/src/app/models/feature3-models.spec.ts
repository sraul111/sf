import { BookJobRequest, Job } from './job.model';
import { Technician } from './technician.model';

// DoD 16 — these literals only compile if the optional Feature 3 fields exist on the interfaces
describe('Feature 3 frontend model fields', () => {
  it('Technician accepts optional skills and still accepts literals without them', () => {
    const withSkills: Technician = { id: 1, name: 'Alex Rivera', region: 'North', skills: ['HVAC', 'Electrical'] };
    const withoutSkills: Technician = { id: 2, name: 'Jordan Blake', region: 'South' };

    expect(withSkills.skills).toEqual(['HVAC', 'Electrical']);
    expect(withoutSkills.skills).toBeUndefined();
  });

  it('Job accepts optional requiredSkill as string or null', () => {
    const base: Job = {
      id: 1,
      technicianId: 1,
      customerName: 'Acme Corp',
      startTime: '2026-10-01T09:00:00',
      endTime: '2026-10-01T11:00:00',
      status: 'SCHEDULED'
    };
    const skilled: Job = { ...base, requiredSkill: 'HVAC' };
    const unskilled: Job = { ...base, requiredSkill: null };

    expect(skilled.requiredSkill).toBe('HVAC');
    expect(unskilled.requiredSkill).toBeNull();
    expect(base.requiredSkill).toBeUndefined();
  });

  it('BookJobRequest accepts optional requiredSkill and the legacy shape', () => {
    const legacy: BookJobRequest = {
      technicianId: 3,
      customerName: 'Legacy',
      startTime: '2026-10-01T09:00:00',
      endTime: '2026-10-01T10:00:00'
    };
    const skilled: BookJobRequest = { ...legacy, requiredSkill: 'Plumbing' };
    const explicitNull: BookJobRequest = { ...legacy, requiredSkill: null };

    expect(legacy.requiredSkill).toBeUndefined();
    expect(skilled.requiredSkill).toBe('Plumbing');
    expect(explicitNull.requiredSkill).toBeNull();
  });
});
