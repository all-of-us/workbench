import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';

@Component({
  selector: 'crit-root-spinner',
  template: `
    <div *ngIf="(loading$ | async)" id="spinner-container">
      <div class="spinner" [style.left]="'50%'">Loading...</div>
    </div>
  `,
})
export class RootSpinnerComponent {
  @Input() loading$: Observable<boolean>;
}
