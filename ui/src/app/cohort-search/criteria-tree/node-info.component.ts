import {
  Component,
  ChangeDetectionStrategy,
  Input,
  ViewEncapsulation
} from '@angular/core';
import {CohortSearchActions} from '../actions';
import {Criteria} from 'generated';


@Component({
  selector: 'app-node-info',
  templateUrl: './node-info.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class NodeInfoComponent {
  @Input() node: Criteria;

  constructor(private actions: CohortSearchActions) {}
}
