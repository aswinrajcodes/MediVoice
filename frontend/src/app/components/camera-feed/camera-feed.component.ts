import { Component, inject, ViewChild, ElementRef } from '@angular/core';
import { CameraService } from '../../services/camera.service';

@Component({
  selector: 'app-camera-feed',
  standalone: true,
  templateUrl: './camera-feed.component.html',
  styles: [`
    :host { display: block; }
  `]
})
export class CameraFeedComponent {
  readonly cameraService = inject(CameraService);

  @ViewChild('videoElement') videoRef!: ElementRef<HTMLVideoElement>;

  async toggleCamera(): Promise<void> {
    if (this.cameraService.cameraActive()) {
      this.cameraService.stopCamera();
    } else {
      try {
        await this.cameraService.startCamera(this.videoRef.nativeElement);
      } catch (e) {
        console.error('Camera error:', e);
      }
    }
  }
}
