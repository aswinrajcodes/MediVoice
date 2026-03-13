import { Component, inject, computed } from '@angular/core';
import { AudioService } from '../../services/audio.service';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-voice-panel',
  standalone: true,
  templateUrl: './voice-panel.component.html',
  styles: [`
    :host { display: block; }
  `]
})
export class VoicePanelComponent {
  readonly audioService = inject(AudioService);
  readonly wsService = inject(WebSocketService);

  readonly statusText = computed(() => {
    if (this.wsService.connectionStatus() === 'connecting') return 'Connecting to MediVoice...';
    if (this.wsService.connectionStatus() === 'disconnected') return 'Reconnecting...';
    if (this.audioService.isRecording()) return '🔴 Listening... (click to stop)';
    return 'Click the microphone to start speaking';
  });

  async toggleMic(): Promise<void> {
    if (this.audioService.isRecording()) {
      this.audioService.stopRecording();
    } else {
      try {
        await this.audioService.startRecording();
      } catch (e) {
        console.error('Microphone error:', e);
      }
    }
  }

  onInterrupt(): void {
    this.wsService.sendInterrupt();
    this.audioService.clearPlaybackQueue();
  }
}
