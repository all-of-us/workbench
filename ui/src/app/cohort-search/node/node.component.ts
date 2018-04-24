import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  criteriaChildren,
  criteriaError,
  isCriteriaLoading,
} from '../redux';

@Component({
  selector: 'crit-node',
  templateUrl: './node.component.html',
  styleUrls: ['./node.component.css']
})
export class NodeComponent implements OnInit, OnDestroy {
  @Input() node;

  expanded = false;
  children = List<any>();
  loading = false;
  error = false;
  subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const parentId = this.node.get('id');
    const errorSub = this.ngRedux
      .select(criteriaError(_type, parentId))
      .map(err => !(err === null || err === undefined))
      .subscribe(err => this.error = err);

    const loadingSub = this.ngRedux
      .select(isCriteriaLoading(_type, parentId))
      .subscribe(loading => this.loading = loading);

    const childSub = this.ngRedux
      .select(criteriaChildren(_type, parentId))
      .subscribe(children => this.children = children);

    this.subscription = errorSub;
    this.subscription.add(loadingSub);
    this.subscription.add(childSub);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  loadChildren(event) {
    if (!event) { return ; }
    const _type = this.node.get('type').toLowerCase();
    const parentId = this.node.get('id');
    this.actions.fetchCriteria(_type, parentId);
  }

  toggleExpanded() {
    this.expanded = !this.expanded;
  }
}
