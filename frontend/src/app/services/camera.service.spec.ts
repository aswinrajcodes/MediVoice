import { TestBed } from '@angular/core/testing';
import { CameraService } from './camera.service';
import { WebSocketService } from './websocket.service';

describe('CameraService', () => {
  let service: CameraService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CameraService, WebSocketService]
    });
    service = TestBed.inject(CameraService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize with camera inactive', () => {
    expect(service.cameraActive()).toBe(false);
  });

  it('should initialize with empty camera error', () => {
    expect(service.cameraError()).toBe('');
  });

  it('should set cameraActive to false on stopCamera', () => {
    // Simulate active state
    (service as any).cameraActive.set(true);
    expect(service.cameraActive()).toBe(true);

    service.stopCamera();

    expect(service.cameraActive()).toBe(false);
  });
});
