import {
  Component,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Wizard} from 'clarity-angular';
import {Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
  activeRole,
  activeItemId,
  activeGroupId,
  getItem,
  wizardOpen,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent implements OnInit {

  private priorState;
  @select(wizardOpen) readonly open$: Observable<boolean>;
  @select(activeCriteriaType) readonly critType$: Observable<string>;
  @ViewChild('wizard') wizard: Wizard;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    this.priorState = this.ngRedux.getState();
    const itemId = activeItemId(this.priorState);
    const groupId = activeGroupId(this.priorState);

    const item = getItem(itemId)(this.priorState);
    if (item.isEmpty()) {
      this.actions.initGroupItem(itemId, groupId);
    }
  }

  close() {
    this.actions.setWizardClosed();
    this.actions.clearActiveContext();
  }

  finish() {
    const state = this.ngRedux.getState();

    this.actions.requestItemCount(activeRole(state), activeItemId(state));
    this.actions.requestGroupCount(activeRole(state), activeGroupId(state));
    this.actions.requestTotalCount();

    this.actions.setWizardClosed();
    this.actions.clearActiveContext();
  }
}
