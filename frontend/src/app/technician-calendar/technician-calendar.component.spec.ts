import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { TechnicianCalendarComponent } from './technician-calendar.component';
import { TechnicianService } from '../services/technician.service';

describe('TechnicianCalendarComponent', () => {
  let component: TechnicianCalendarComponent;
  let fixture: ComponentFixture<TechnicianCalendarComponent>;
  let technicianService: jasmine.SpyObj<TechnicianService>;

  beforeEach(async () => {
    technicianService = jasmine.createSpyObj<TechnicianService>('TechnicianService', ['getTechnicians', 'getJobsForTechnician', 'bookJob']);
    technicianService.getTechnicians.and.returnValue(of([
      { id: 1, name: 'Ada', region: 'North' },
      { id: 2, name: 'Grace', region: 'South' }
    ]));
    technicianService.getJobsForTechnician.and.returnValue(of([
      { id: 1, technicianId: 1, customerName: 'Alpha', startTime: '2026-10-01T09:00:00', endTime: '2026-10-01T10:00:00', status: 'SCHEDULED' }
    ]));
    technicianService.bookJob.and.returnValue(of({
      id: 2,
      technicianId: 1,
      customerName: 'New job',
      startTime: '2026-10-01T11:00:00',
      endTime: '2026-10-01T12:00:00',
      status: 'SCHEDULED'
    }));

    await TestBed.configureTestingModule({
      declarations: [TechnicianCalendarComponent],
      imports: [FormsModule],
      providers: [{ provide: TechnicianService, useValue: technicianService }]
    }).compileComponents();

    fixture = TestBed.createComponent(TechnicianCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render the technician list and select the first technician by default', () => {
    expect(component.technicians.length).toBe(2);
    expect(component.selectedTechnicianId).toBe(1);
    expect(component.jobs.length).toBe(1);
  });

  it('should show a validation error when booking without all fields', () => {
    component.selectedTechnicianId = 1;
    component.newCustomerName = '';
    component.newStartTime = '';
    component.newEndTime = '';

    component.bookJob();

    expect(component.errorMessage).toBe('Fill in customer name, start time, and end time.');
  });

  it('should handle backend booking errors', () => {
    component.selectedTechnicianId = 1;
    component.newCustomerName = 'Failing Job';
    component.newStartTime = '2026-10-01T13:00:00';
    component.newEndTime = '2026-10-01T14:00:00';
    technicianService.bookJob.and.returnValue(throwError(() => ({ error: { message: 'That slot is already booked.' } })));

    component.bookJob();

    expect(component.errorMessage).toBe('That slot is already booked.');
  });
});
