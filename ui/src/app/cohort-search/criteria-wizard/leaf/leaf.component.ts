import {Component, Input} from '@angular/core';

import {CohortSearchActions} from '../../redux';
import {needsAttributes} from '../utils';

@Component({
  selector: 'crit-leaf',
  templateUrl: './leaf.component.html',
  styleUrls: ['./leaf.component.css']
})
export class LeafComponent {
  @Input() node;
  @Input() selections;

  constructor(private actions: CohortSearchActions) {}

  /**
   * Properties
   */
  get paramId() {
    return `param${this.node.get('id')}`;
  }

  get selectable() {
    return this.node.get('selectable', false);
  }

  get isSelected() {
    const noAttr = !needsAttributes(this.node);
    const selectedIDs = this.selections.map(n => n.get('parameterId'));
    const selected = selectedIDs.includes(this.paramId);
    return noAttr && selected;
  }

  get nonZeroCount() {
    return this.node.get('count', 0) > 0;
  }

  get displayName() {
    const isDemo = this.node.get('type', '').match(/^DEMO.*/i);
    const nameIsCode = this.node.get('name', '') === this.node.get('code', '');
    return nameIsCode || isDemo
      ? ''
      : this.node.get('name', '');
  }

  get displayCode() {
    return this.node.get('type', '').match(/^DEMO.*/i)
      ? this.node.get('name', '')
      : this.node.get('code', '');
  }

  select() {
    if (needsAttributes(this.node)) {
      this.actions.setWizardFocus(this.node);
    } else {
      /*
       * Here we set the parameter ID to `param<criterion ID>` - this is
       * deterministic and avoids duplicate parameters for criterion which do
       * not require attributes.  Criterion which require attributes in order
       * to have a complete sense are given a unique ID based on the attribute
       */
      const param = this.node.set('parameterId', this.paramId);
      this.actions.addParameter(param);
    }
  }
}
