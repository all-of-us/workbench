import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List} from 'immutable';

import {CohortSearchActions} from '../redux';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent {
  @Input() chartData$: Observable<List<any>>;
  @Input() total$: Observable<number>;
  @Input() isRequesting$: Observable<boolean>;

  constructor(private actions: CohortSearchActions) {}

  save() {
    // TODO(jms) Dispatch Save action here (?)
  }
}
