export interface Session {
  id: string;
  status: 'ACTIVE' | 'CLOSED' | 'ERROR';
  createdAt: string;
  closedAt?: string;
  transcript?: string;
  triageLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  triageReason?: string;
}
