import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VoicePanelComponent } from './voice-panel.component';
import { AudioService } from '../../services/audio.service';
import { WebSocketService } from '../../services/websocket.service';

describe('VoicePanelComponent', () => {
  let component: VoicePanelComponent;
  let fixture: ComponentFixture<VoicePanelComponent>;
  let wsService: WebSocketService;
  let audioService: AudioService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VoicePanelComponent],
      providers: [AudioService, WebSocketService]
    }).compileComponents();

    fixture = TestBed.createComponent(VoicePanelComponent);
    component = fixture.componentInstance;
    wsService = TestBed.inject(WebSocketService);
    audioService = TestBed.inject(AudioService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show "Click the microphone" status text when connected and not recording', () => {
    (wsService as any).connectionStatus.set('connected');
    expect(component.statusText()).toContain('Click the microphone');
  });

  it('should show "Connecting" status text when connecting', () => {
    (wsService as any).connectionStatus.set('connecting');
    expect(component.statusText()).toContain('Connecting');
  });

  it('should show "Reconnecting" status text when disconnected', () => {
    (wsService as any).connectionStatus.set('disconnected');
    expect(component.statusText()).toContain('Reconnecting');
  });

  it('should show "Listening" status text when recording', () => {
    (wsService as any).connectionStatus.set('connected');
    (audioService as any).micState.set('active');
    expect(component.statusText()).toContain('Listening');
  });

  it('should render mic button', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.mic-button')).toBeTruthy();
  });

  it('should render connection status', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.connection-status')).toBeTruthy();
  });
});
