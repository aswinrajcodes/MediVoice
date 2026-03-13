import { Component, inject, OnInit } from '@angular/core';
import { WebSocketService } from './services/websocket.service';
import { AudioService } from './services/audio.service';
import { VoicePanelComponent } from './components/voice-panel/voice-panel.component';
import { CameraFeedComponent } from './components/camera-feed/camera-feed.component';
import { TranscriptComponent } from './components/transcript/transcript.component';
import { EmergencyPanelComponent } from './components/emergency-panel/emergency-panel.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    VoicePanelComponent,
    CameraFeedComponent,
    TranscriptComponent,
    EmergencyPanelComponent
  ],
  templateUrl: './app.component.html',
  styles: [`
    :host { display: block; }
  `]
})
export class AppComponent implements OnInit {
  readonly wsService = inject(WebSocketService);
  private readonly audioService = inject(AudioService);

  ngOnInit(): void {
    this.wsService.connect();
  }

  onEmergencyDismissed(): void {
    this.wsService.clearEmergency();
  }
}
