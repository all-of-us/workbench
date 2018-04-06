import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  isParameterActive,
} from '../../redux';

/*
 * Stub function - some criteria types will have "attributes" that help define
 * them.  Demographics AGE was in this category until we removed demographics
 * to its own modal form.  This function and the overall attribute flow has
 * been left intact in order to provide a "hook-in" location for implementing
 * other types of attribute.
 */
function needsAttributes(node) {
  return false;
}

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
    const isPM = this.node.get('type', '') === 'PM';
    const nameIsCode = this.node.get('name', '') === this.node.get('code', '');
    return (isPM || nameIsCode) ? '' : this.node.get('name', '');
  }

  get displayCode() {
    if (this.node.get('type', '') === 'PM') {
      return this.node.get('name', '');
    }
    return this.node.get('code', '');
  }

  /*
   * On selection, we examine the selected criterion and see if it needs some
   * attributes. If it does, we set the criterion in "focus".  The explorer
   * listens for their being a node in focus; if there is, it sets its own mode
   * to `SetAttr` (setting attributes) and passes the node down to
   * `crit-attributes`, the entry point defined by the attributes module.
   *
   * If the node does NOT need an attribute we give it a deterministic ID and
   * add it to the selected params in the state.
   */
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
