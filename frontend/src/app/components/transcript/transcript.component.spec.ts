import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranscriptComponent } from './transcript.component';
import { WebSocketService } from '../../services/websocket.service';

describe('TranscriptComponent', () => {
  let component: TranscriptComponent;
  let fixture: ComponentFixture<TranscriptComponent>;
  let wsService: WebSocketService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TranscriptComponent],
      providers: [WebSocketService]
    }).compileComponents();

    fixture = TestBed.createComponent(TranscriptComponent);
    component = fixture.componentInstance;
    wsService = TestBed.inject(WebSocketService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show welcome message when no transcript messages', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.transcript-empty')).toBeTruthy();
    expect(compiled.textContent).toContain('Welcome to MediVoice');
  });

  it('should render message bubbles when messages exist', () => {
    (wsService as any).transcriptMessages.set([
      { role: 'user', text: 'I have a headache', timestamp: new Date() },
      { role: 'assistant', text: 'Can you describe the headache?', timestamp: new Date() }
    ]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const bubbles = compiled.querySelectorAll('.bubble');
    expect(bubbles.length).toBe(2);
  });

  it('should style user and assistant bubbles differently', () => {
    (wsService as any).transcriptMessages.set([
      { role: 'user', text: 'Test', timestamp: new Date() },
      { role: 'assistant', text: 'Response', timestamp: new Date() }
    ]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.bubble.user')).toBeTruthy();
    expect(compiled.querySelector('.bubble.assistant')).toBeTruthy();
  });

  it('should display "You" label for user messages', () => {
    (wsService as any).transcriptMessages.set([
      { role: 'user', text: 'Hello', timestamp: new Date() }
    ]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const role = compiled.querySelector('.bubble.user .bubble-role');
    expect(role?.textContent).toContain('You');
  });

  it('should display "MediVoice" label for assistant messages', () => {
    (wsService as any).transcriptMessages.set([
      { role: 'assistant', text: 'Hello', timestamp: new Date() }
    ]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const role = compiled.querySelector('.bubble.assistant .bubble-role');
    expect(role?.textContent).toContain('MediVoice');
  });
});
