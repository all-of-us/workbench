import {
  Component,
  Input,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Wizard} from 'clarity-angular';
import {Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  activeCriteriaList,
  activeRole,
  activeGroupId,
  activeItem,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent {
  @Input() open: boolean;
  @Input() criteriaType: string;

  private rootsAreLoading = true;
  // Zero is default parent ID for criteria tree roots
  private readonly parentId = 0;
  @ViewChild('wizard') wizard: Wizard;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  setLoading(value: boolean) {
    this.rootsAreLoading = value;
  }

  get rootNode() {
    return Map({type: this.criteriaType, id: this.parentId});
  }

  cancel() {
    this.actions.cancelWizard();
  }

  finish() {
    const state = this.ngRedux.getState();
    const role = activeRole(state);
    const groupId = activeGroupId(state);
    const itemId = activeItem(state).get('id');
    const selections = activeCriteriaList(state);

    this.actions.finishWizard();
    if (!selections.isEmpty()) {
      this.actions.requestItemCount(role, itemId);
      this.actions.requestGroupCount(role, groupId);
      this.actions.requestTotalCount();
    }
  }
}
