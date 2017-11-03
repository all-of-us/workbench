import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';

@Component({
  selector: 'crit-alerts',
  templateUrl: './alerts.component.html',
})
export class AlertsComponent {
  @Input() errors$: Observable<any>;
}
