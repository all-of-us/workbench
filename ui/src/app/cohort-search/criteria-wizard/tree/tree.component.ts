import {
  Component,
  OnDestroy,
  OnInit,
  Input,
  Output,
  EventEmitter,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaList,
  isCriteriaLoading,
  criteriaChildren,
  criteriaError,
} from '../../redux';


@Component({
  selector: 'crit-tree',
  templateUrl: './tree.component.html',
  styleUrls: ['./tree.component.css'],
})
export class TreeComponent implements OnInit, OnDestroy {
  @Input() node;

  private loading: boolean;
  private error: any;
  private children: List<any>;
  private selections: List<any>;
  private subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const _parentId = this.node.get('id');

    const error$ = this.ngRedux.select(criteriaError(_type, _parentId));
    const loading$ = this.ngRedux.select(isCriteriaLoading(_type, _parentId));
    const children$ = this.ngRedux.select(criteriaChildren(_type, _parentId));
    const selections$ = this.ngRedux.select(activeCriteriaList);

    this.subscriptions = [
      error$.subscribe(value => this.error = value),
      loading$.subscribe(value => this.loading = value),
      children$.subscribe(value => this.children = value),
      selections$.subscribe(value => this.selections = value),
    ];
    this.actions.fetchCriteria(_type, _parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  get hasError() {
    return !(this.error === null || this.error === undefined);
  }

  /** Functions of the node's child nodes */

  trackById(index, node) {
    return node ? node.get('id') : undefined;
  }

  select(node) {
    this.actions.selectCriteria(node);
  }

  isSelected(node) {
    return this.selections.includes(node);
  }

  nonZeroCount(node) {
    return node.get('count', 0) > 0;
  }

  selectable(node) {
    return node.get('selectable', false);
  }

  displayName(node) {
    const isDemo = node.get('type', '').match(/^DEMO.*/i);
    const nameIsCode = node.get('name', '') === node.get('code', '');
    return nameIsCode || isDemo
      ? ''
      : node.get('name', '');
  }

  displayCode(node) {
    return node.get('type', '').match(/^DEMO.*/i)
      ? node.get('name', '')
      : node.get('code', '');
  }
}
