export type JobStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface Job {
  id: number;
  technicianId: number;
  customerName: string;
  startTime: string;
  endTime: string;
  status: JobStatus;
}

export interface BookJobRequest {
  technicianId: number;
  customerName: string;
  startTime: string;
  endTime: string;
}