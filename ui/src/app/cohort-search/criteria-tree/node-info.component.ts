import {Component, Input} from '@angular/core';
import {Criteria} from 'generated';


@Component({
  selector: 'app-node-info',
  templateUrl: './node-info.component.html',
})
export class NodeInfoComponent {
  @Input() node: Criteria;
}
