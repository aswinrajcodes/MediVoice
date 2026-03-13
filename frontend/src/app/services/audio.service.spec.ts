import { TestBed } from '@angular/core/testing';
import { AudioService } from './audio.service';
import { WebSocketService } from './websocket.service';

describe('AudioService', () => {
  let service: AudioService;
  let wsService: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AudioService, WebSocketService]
    });
    service = TestBed.inject(AudioService);
    wsService = TestBed.inject(WebSocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize with idle mic state', () => {
    expect(service.micState()).toBe('idle');
  });

  it('should compute isRecording as false when idle', () => {
    expect(service.isRecording()).toBe(false);
  });

  it('should initialize with isPlaying as false', () => {
    expect(service.isPlaying()).toBe(false);
  });

  it('should clear playback queue and set isPlaying to false', () => {
    // Simulate playing state
    (service as any).isPlaying.set(true);
    (service as any).playbackQueue = [new ArrayBuffer(8)];

    service.clearPlaybackQueue();

    expect(service.isPlaying()).toBe(false);
    expect((service as any).playbackQueue.length).toBe(0);
  });

  it('should set mic state to idle after stopRecording', () => {
    // Start in active state
    (service as any).micState.set('active');
    expect(service.isRecording()).toBe(true);

    service.stopRecording();

    expect(service.micState()).toBe('idle');
    expect(service.isRecording()).toBe(false);
  });
});
