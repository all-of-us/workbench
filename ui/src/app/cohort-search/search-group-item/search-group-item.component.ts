import {Component, Input} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {CohortSearchActions, CohortSearchState, getItem} from '../redux';

@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
})
export class SearchGroupItemComponent {
  @Input() role: string;
  @Input() groupId: string;
  @Input() itemId: string;
  @Input() itemIndex: number;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  get description() {
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }

  get item() {
    return getItem(this.itemId)(this.ngRedux.getState());
  }

  get isRequesting() {
    return this.item.get('isRequesting', false);
  }

  launchWizard() {
    this.actions.setActiveContext({
      criteriaType: this.item.get('type'),
      role: this.role,
      groupId: this.groupId,
      itemId: this.itemId,
    });

    this.actions.setWizardOpen();
  }
}
