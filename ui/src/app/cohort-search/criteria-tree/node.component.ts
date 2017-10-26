import {
  Component,
  OnDestroy,
  OnInit,
  Input,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  isCriteriaSelected,
  criteriaChildren,
} from '../redux';


@Component({
  selector: 'app-criteria-tree-node',
  templateUrl: './node.component.html',
})
export class CriteriaTreeNodeComponent implements OnInit, OnDestroy {
  @Input() node;

  private children: List<any>;
  private loading: boolean;
  private isSelected: boolean;
  private subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const _parentId = this.node.get('id');

    const children$ = this.ngRedux.select(criteriaChildren(_type, _parentId));
    const loading$ = this.ngRedux.select(isCriteriaLoading(_type, _parentId));
    const selected$ = this.ngRedux.select(isCriteriaSelected(this.node.get('id')));

    this.subscriptions = [
      children$.subscribe(value => this.children = value),
      loading$.subscribe(value => this.loading = value),
      selected$.subscribe(value => this.isSelected = value),
    ];
    this.actions.fetchCriteria(_type, _parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  select(event) {
    this.actions.selectCriteria(this.node);
  }

  trackById(index, node) {
    return node ? node.get('id') : undefined;
  }

  get nonZeroCount() {
    return this.node.get('count', 0) > 0;
  }

  get selectable() {
    return this.node.get('selectable', false);
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
}
