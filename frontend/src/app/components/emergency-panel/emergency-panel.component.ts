import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-emergency-panel',
  standalone: true,
  templateUrl: './emergency-panel.component.html',
  styles: [`
    :host { display: block; }
  `]
})
export class EmergencyPanelComponent {
  @Input({ required: true }) level!: 'high' | 'medium';
  @Input({ required: true }) reason!: string;
  @Output() dismissed = new EventEmitter<void>();

  dismiss(): void {
    this.dismissed.emit();
  }
}
