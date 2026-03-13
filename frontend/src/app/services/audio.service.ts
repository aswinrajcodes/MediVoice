import { Injectable, signal, computed, inject } from '@angular/core';
import { WebSocketService } from './websocket.service';

@Injectable({ providedIn: 'root' })
export class AudioService {
  private wsService = inject(WebSocketService);

  // Signals
  readonly micState = signal<'idle' | 'requesting' | 'active' | 'error'>('idle');
  readonly isRecording = computed(() => this.micState() === 'active');
  readonly isPlaying = signal(false);

  private audioContext: AudioContext | null = null;
  private workletNode: AudioWorkletNode | null = null;
  private stream: MediaStream | null = null;

  // Audio playback queue for Gemini responses
  private playbackQueue: ArrayBuffer[] = [];
  private playbackContext: AudioContext | null = null;

  constructor() {
    // Register audio callback with WebSocket service
    this.wsService.setAudioCallback((data) => this.playAudioChunk(data));
  }

  async startRecording(): Promise<void> {
    this.micState.set('requesting');
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 48000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });

      this.audioContext = new AudioContext({ sampleRate: 48000 });

      // Load and register the AudioWorklet processor
      await this.audioContext.audioWorklet.addModule('/assets/audio-processor.worklet.js');

      const source = this.audioContext.createMediaStreamSource(this.stream);
      this.workletNode = new AudioWorkletNode(this.audioContext, 'medivoice-audio-processor');

      this.workletNode.port.onmessage = (event: MessageEvent) => {
        // Send PCM16 audio chunk to backend via WebSocket
        this.wsService.sendBinary(event.data as ArrayBuffer);
      };

      source.connect(this.workletNode);
      // Do NOT connect workletNode to destination — we don't want to hear our own mic
      this.micState.set('active');

    } catch (err) {
      console.error('Failed to start recording:', err);
      this.micState.set('error');
      throw err;
    }
  }

  stopRecording(): void {
    if (this.workletNode) {
      this.workletNode.disconnect();
      this.workletNode = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach(t => t.stop());
      this.stream = null;
    }
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    this.micState.set('idle');
  }

  async playAudioChunk(audioData: ArrayBuffer): Promise<void> {
    this.playbackQueue.push(audioData);
    if (!this.isPlaying()) {
      await this.drainPlaybackQueue();
    }
  }

  clearPlaybackQueue(): void {
    this.playbackQueue = [];
    this.isPlaying.set(false);
  }

  private async drainPlaybackQueue(): Promise<void> {
    this.isPlaying.set(true);

    // Create or reuse playback context at 24000Hz (Gemini output rate)
    if (!this.playbackContext || this.playbackContext.state === 'closed') {
      this.playbackContext = new AudioContext({ sampleRate: 24000 });
    }

    while (this.playbackQueue.length > 0) {
      const chunk = this.playbackQueue.shift()!;
      await this.playPCM16Chunk(chunk);
    }
    this.isPlaying.set(false);
  }

  private async playPCM16Chunk(buffer: ArrayBuffer): Promise<void> {
    if (!this.playbackContext) return;

    try {
      // Convert LINEAR16 PCM from Gemini (24000Hz) to playable AudioBuffer
      const int16 = new Int16Array(buffer);
      const float32 = new Float32Array(int16.length);
      for (let i = 0; i < int16.length; i++) {
        float32[i] = int16[i] / 32768;
      }

      const audioBuffer = this.playbackContext.createBuffer(1, float32.length, 24000);
      audioBuffer.copyToChannel(float32, 0);

      const source = this.playbackContext.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(this.playbackContext.destination);

      return new Promise<void>(resolve => {
        source.onended = () => resolve();
        source.start();
      });
    } catch (e) {
      console.error('Error playing audio chunk:', e);
    }
  }
}
