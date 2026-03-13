import { Injectable, signal, computed, inject } from '@angular/core';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class TriageService {
  private wsService = inject(WebSocketService);

  readonly currentLevel = computed(() => this.wsService.triageLevel());
  readonly currentReason = computed(() => this.wsService.emergencyReason());
  readonly isEmergency = computed(() => this.wsService.isEmergency());
  readonly isMediumAlert = computed(() => this.wsService.isMediumAlert());
  readonly alertDismissed = signal(false);

  dismiss(): void {
    this.alertDismissed.set(true);
    this.wsService.clearEmergency();
  }

  reset(): void {
    this.alertDismissed.set(false);
  }
}
