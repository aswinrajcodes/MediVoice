import { Injectable, signal, inject } from '@angular/core';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class CameraService {
  private wsService = inject(WebSocketService);

  readonly cameraActive = signal(false);
  readonly cameraError = signal<string>('');

  private stream: MediaStream | null = null;
  private captureInterval: ReturnType<typeof setInterval> | null = null;
  private canvas = document.createElement('canvas');

  async startCamera(videoEl: HTMLVideoElement): Promise<void> {
    try {
      this.cameraError.set('');
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480, facingMode: 'user' }
      });
      videoEl.srcObject = this.stream;
      await videoEl.play();
      this.cameraActive.set(true);

      // Capture and send a frame every 2 seconds
      this.captureInterval = setInterval(() => this.captureFrame(videoEl), 2000);
    } catch (err) {
      console.error('Failed to start camera:', err);
      this.cameraError.set('Camera access denied or not available');
      throw err;
    }
  }

  stopCamera(): void {
    if (this.captureInterval) {
      clearInterval(this.captureInterval);
      this.captureInterval = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach(t => t.stop());
      this.stream = null;
    }
    this.cameraActive.set(false);
  }

  private captureFrame(videoEl: HTMLVideoElement): void {
    if (!this.cameraActive() || !videoEl.videoWidth) return;

    this.canvas.width = videoEl.videoWidth;
    this.canvas.height = videoEl.videoHeight;
    const ctx = this.canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(videoEl, 0, 0);
    const base64 = this.canvas.toDataURL('image/jpeg', 0.7).split(',')[1];
    this.wsService.sendJson({
      type: 'video_frame',
      data: base64,
      mimeType: 'image/jpeg'
    });
  }
}
