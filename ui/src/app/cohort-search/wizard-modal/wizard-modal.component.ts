import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {Wizard} from 'clarity-angular';
import {List, Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaList,
  activeRole,
  activeGroupId,
  activeItem,
  wizardOpen,
} from '../redux';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent implements OnInit, OnDestroy {
  @Input() open: boolean;
  @Input() criteriaType: string;
  @select(['wizard']) wizardState$: Observable<Map<any, any>>;
  @select(['criteria']) criteriaState$: Observable<Map<any, any>>;
  private wizardState: Map<any, any> = Map();
  private criteriaState: Map<any, any> = Map();
  // Zero is default parent ID for criteria tree roots
  private readonly parentId = 0;
  private _subscriptions: Subscription[];
  @ViewChild('wizard') wizard: Wizard;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    this._subscriptions = [
      this.wizardState$.subscribe(s => this.wizardState = s),
      this.criteriaState$.subscribe(s => this.criteriaState = s),
    ];
    this.actions.fetchCriteria(this.criteriaType, this.parentId);
  }

  ngOnDestroy() {
    this._subscriptions.forEach(sub => sub.unsubscribe());
  }

  get criteriaRootsLoading() {
    const path = ['requests', this.criteriaType, this.parentId];
    return this.criteriaState.getIn(path, false);
  }

  get selectedCriteria() {
    return this.wizardState.get('selections', Map());
  }

  get children() {
    const path = ['tree', this.criteriaType, this.parentId];
    return this.criteriaState.getIn(path, List());
  }

  cancel() {
    this.actions.cancelWizard();
  }

  finish() {
    const role = this.wizardState.get('role');
    const groupId = this.wizardState.get('groupId');
    const itemId = this.wizardState.get('itemId');

    this.actions.finishWizard();
    if (!this.selectedCriteria.isEmpty()) {
      this.actions.requestItemCount(role, itemId);
      this.actions.requestGroupCount(role, groupId);
      this.actions.requestTotalCount();
    }
  }
}
