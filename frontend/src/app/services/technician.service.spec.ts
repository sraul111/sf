import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TechnicianService } from './technician.service';
import { Technician } from '../models/technician.model';
import { BookJobRequest } from '../models/job.model';

describe('TechnicianService', () => {
  let service: TechnicianService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TechnicianService]
    });
    service = TestBed.inject(TechnicianService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should fetch technicians from the backend', () => {
    const technicians: Technician[] = [{ id: 1, name: 'Alpha', region: 'North' }];

    service.getTechnicians().subscribe(result => {
      expect(result).toEqual(technicians);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/technicians');
    expect(req.request.method).toBe('GET');
    req.flush(technicians);
  });

  it('should book a new job via the backend', () => {
    const payload: BookJobRequest = {
      technicianId: 2,
      customerName: 'Acme',
      startTime: '2026-10-02T09:00:00',
      endTime: '2026-10-02T10:00:00'
    };

    service.bookJob(payload).subscribe(response => {
      expect(response).toEqual({ ...payload, id: 7, status: 'SCHEDULED' });
    });

    const req = httpMock.expectOne('http://localhost:8080/api/jobs');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ ...payload, id: 7, status: 'SCHEDULED' });
  });
});
