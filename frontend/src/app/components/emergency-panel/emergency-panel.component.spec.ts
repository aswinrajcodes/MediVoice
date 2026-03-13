import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmergencyPanelComponent } from './emergency-panel.component';

describe('EmergencyPanelComponent', () => {
  let component: EmergencyPanelComponent;
  let fixture: ComponentFixture<EmergencyPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmergencyPanelComponent]
    }).compileComponents();
  });

  describe('HIGH severity', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(EmergencyPanelComponent);
      component = fixture.componentInstance;
      component.level = 'high';
      component.reason = 'Chest pain detected — seek immediate help';
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should render emergency overlay for HIGH level', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.emergency-overlay')).toBeTruthy();
    });

    it('should show MEDICAL EMERGENCY DETECTED heading', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const h1 = compiled.querySelector('h1');
      expect(h1?.textContent).toContain('MEDICAL EMERGENCY DETECTED');
    });

    it('should display the reason', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const reason = compiled.querySelector('.emergency-reason');
      expect(reason?.textContent).toContain('Chest pain detected');
    });

    it('should render 911 call button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-emergency');
      expect(btn).toBeTruthy();
      expect(btn?.getAttribute('href')).toBe('tel:911');
    });

    it('should render Poison Control button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-poison');
      expect(btn).toBeTruthy();
      expect(btn?.getAttribute('href')).toBe('tel:18002221222');
    });

    it('should render Find Nearest ER button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const btn = compiled.querySelector('.btn-er');
      expect(btn).toBeTruthy();
    });

    it('should emit dismissed event when dismiss button is clicked', () => {
      spyOn(component.dismissed, 'emit');

      const btn = fixture.nativeElement.querySelector('.btn-dismiss') as HTMLButtonElement;
      btn.click();

      expect(component.dismissed.emit).toHaveBeenCalled();
    });
  });

  describe('MEDIUM severity', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(EmergencyPanelComponent);
      component = fixture.componentInstance;
      component.level = 'medium';
      component.reason = 'High fever detected';
      fixture.detectChanges();
    });

    it('should render medium alert bar (not overlay)', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.medium-alert-bar')).toBeTruthy();
      expect(compiled.querySelector('.emergency-overlay')).toBeNull();
    });

    it('should display the reason in the alert bar', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const bar = compiled.querySelector('.medium-alert-bar');
      expect(bar?.textContent).toContain('High fever detected');
    });

    it('should emit dismissed when X button is clicked', () => {
      spyOn(component.dismissed, 'emit');

      const btn = fixture.nativeElement.querySelector('.alert-dismiss') as HTMLButtonElement;
      btn.click();

      expect(component.dismissed.emit).toHaveBeenCalled();
    });
  });
});
