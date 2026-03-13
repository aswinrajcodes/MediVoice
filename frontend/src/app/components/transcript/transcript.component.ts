import { Component, inject, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-transcript',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './transcript.component.html',
  styles: [`
    :host { display: block; }
  `]
})
export class TranscriptComponent implements AfterViewChecked {
  readonly wsService = inject(WebSocketService);

  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    try {
      const el = this.scrollContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    } catch (err) {
      // ignore
    }
  }
}
