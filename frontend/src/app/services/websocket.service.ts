import { Injectable, signal, computed, inject } from '@angular/core';
import { TranscriptMessage } from '../models/transcript-message.model';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  // Signals for all state consumed by components
  readonly connectionStatus = signal<'connecting' | 'connected' | 'disconnected'>('disconnected');
  readonly transcriptMessages = signal<TranscriptMessage[]>([]);
  readonly triageLevel = signal<'low' | 'medium' | 'high'>('low');
  readonly emergencyReason = signal<string>('');
  readonly isEmergency = computed(() => this.triageLevel() === 'high');
  readonly isMediumAlert = computed(() => this.triageLevel() === 'medium');

  private ws: WebSocket | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  // Callbacks set by AudioService to handle incoming audio
  private onAudioReceived: ((data: ArrayBuffer) => Promise<void>) | null = null;

  setAudioCallback(cb: (data: ArrayBuffer) => Promise<void>): void {
    this.onAudioReceived = cb;
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN || this.ws?.readyState === WebSocket.CONNECTING) {
      return;
    }

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${protocol}://${location.host}/ws`;
    this.connectionStatus.set('connecting');

    try {
      this.ws = new WebSocket(wsUrl);
      this.ws.binaryType = 'arraybuffer';

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.connectionStatus.set('connected');
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
          this.reconnectTimer = null;
        }
      };

      this.ws.onmessage = async (event: MessageEvent) => {
        if (event.data instanceof ArrayBuffer) {
          // Binary = audio from Gemini — play it
          if (this.onAudioReceived) {
            await this.onAudioReceived(event.data);
          }
        } else if (typeof event.data === 'string') {
          // Text = JSON control message
          try {
            const msg = JSON.parse(event.data);
            this.handleMessage(msg);
          } catch (e) {
            console.error('Failed to parse WebSocket message:', e);
          }
        }
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        this.connectionStatus.set('disconnected');
        // Auto-reconnect after 3 seconds
        this.reconnectTimer = setTimeout(() => this.connect(), 3000);
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
      };
    } catch (e) {
      console.error('Failed to create WebSocket:', e);
      this.connectionStatus.set('disconnected');
      this.reconnectTimer = setTimeout(() => this.connect(), 3000);
    }
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.connectionStatus.set('disconnected');
  }

  sendBinary(data: ArrayBuffer): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(data);
    }
  }

  sendJson(payload: object): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload));
    }
  }

  sendInterrupt(): void {
    this.sendJson({ type: 'interrupt' });
  }

  clearEmergency(): void {
    this.triageLevel.set('low');
    this.emergencyReason.set('');
  }

  private handleMessage(msg: Record<string, unknown>): void {
    switch (msg['type']) {
      case 'transcript':
        this.transcriptMessages.update(msgs => [...msgs, {
          role: msg['role'] as 'user' | 'assistant',
          text: msg['text'] as string,
          timestamp: new Date()
        }]);
        break;

      case 'emergency':
        this.triageLevel.set(msg['level'] as 'low' | 'medium' | 'high');
        this.emergencyReason.set((msg['reason'] as string) ?? '');
        break;

      case 'status':
        console.log('MediVoice status:', msg['message']);
        // Add status message to transcript as a system message
        this.transcriptMessages.update(msgs => [...msgs, {
          role: 'assistant' as const,
          text: msg['message'] as string,
          timestamp: new Date()
        }]);
        break;

      default:
        console.log('Unknown message type:', msg['type']);
    }
  }
}
