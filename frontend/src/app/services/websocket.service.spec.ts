import { TestBed } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WebSocketService]
    });
    service = TestBed.inject(WebSocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize with disconnected status', () => {
    expect(service.connectionStatus()).toBe('disconnected');
  });

  it('should initialize with empty transcript messages', () => {
    expect(service.transcriptMessages()).toEqual([]);
  });

  it('should initialize with low triage level', () => {
    expect(service.triageLevel()).toBe('low');
  });

  it('should compute isEmergency as false for low triage', () => {
    expect(service.isEmergency()).toBe(false);
  });

  it('should compute isMediumAlert as false for low triage', () => {
    expect(service.isMediumAlert()).toBe(false);
  });

  it('should clear emergency and reset to low', () => {
    // Manually set high level to simulate an emergency
    (service as any).triageLevel.set('high');
    (service as any).emergencyReason.set('Chest pain detected');

    expect(service.isEmergency()).toBe(true);

    service.clearEmergency();

    expect(service.triageLevel()).toBe('low');
    expect(service.emergencyReason()).toBe('');
    expect(service.isEmergency()).toBe(false);
  });

  it('should compute isMediumAlert as true when triage is medium', () => {
    (service as any).triageLevel.set('medium');
    expect(service.isMediumAlert()).toBe(true);
    expect(service.isEmergency()).toBe(false);
  });
});
