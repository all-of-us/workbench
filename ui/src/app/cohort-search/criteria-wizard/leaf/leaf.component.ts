import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  /* tslint:disable-next-line:no-unused-variable */
  CohortSearchState,
  isParameterActive,
} from '../../redux';
import {needsAttributes} from '../utils';

@Component({
  selector: 'crit-leaf',
  templateUrl: './leaf.component.html',
  styleUrls: ['./leaf.component.css']
})
export class LeafComponent implements OnInit, OnDestroy {
  @Input() node;
  private isSelected: boolean;
  private subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    const noAttr = !needsAttributes(this.node);

    this.subscription = this.ngRedux
      .select(isParameterActive(this.paramId))
      .map(val => noAttr && val)
      .subscribe(val => this.isSelected = val);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get paramId() {
    return `param${this.node.get('id')}`;
  }

  get selectable() {
    return this.node.get('selectable', false);
  }

  get nonZeroCount() {
    return this.node.get('count', 0) > 0;
  }

  get displayName() {
    const isDemo = this.node.get('type', '').match(/^DEMO.*/i);
    const isPM = this.node.get('type', '').match(/^PM.*/i);
    const nameIsCode = this.node.get('name', '') === this.node.get('code', '');
    return (nameIsCode || isDemo) || isPM
      ? ''
      : this.node.get('name', '');
}

  get displayCode() {
    return this.node.get('type', '').match(/^(DEMO|PM).*/i)
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
