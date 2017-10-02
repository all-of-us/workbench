import {Component, Input} from '@angular/core';
import {CohortSearchActions} from '../actions';
import {Criteria} from 'generated';


@Component({
  selector: 'app-node-info',
  templateUrl: './node-info.component.html',
})
export class NodeInfoComponent {
  @Input() node: Criteria;

  constructor(private actions: CohortSearchActions) {}
}
