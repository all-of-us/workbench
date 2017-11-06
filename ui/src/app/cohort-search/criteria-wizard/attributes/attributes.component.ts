import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List, Map} from 'immutable';

import {CohortSearchActions} from '../../redux';

@Component({
  selector: 'crit-attributes',
  templateUrl: './attributes.component.html',
  styleUrls: ['./attributes.component.css']
})
export class AttributesComponent {
  @Input() node: Map<any, any>;
  private attrs: List<Map<any, any>> = List();

  constructor(private actions: CohortSearchActions) {}

  cancel() {
    this.actions.clearWizardFocus();
  }

  finish() {
    // TODO(jms) transform formData into attributes
    const param = this.node.set('attributes', this.attrs);
    this.actions.addParameter(param);
    this.actions.clearWizardFocus();
  }
}
