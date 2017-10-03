import {Component, OnDestroy, OnInit, Input, ViewEncapsulation} from '@angular/core';
import {NgRedux, select, dispatch} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import 'rxjs/add/operator/mergeMap';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store.interfaces';
import {CohortBuilderService, Criteria, SearchParameter} from 'generated';


@Component({
  selector: 'app-tree-node',
  templateUrl: './node.component.html',
})
export class CriteriaTreeNodeComponent implements OnInit, OnDestroy {
  @Input() node: Criteria;

  children;
  loading;

  subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const critType = this.node.type.toLowerCase();
    const parentId = this.node.id;

    const loadPath = ['loading', critType, parentId];
    const nodePath = ['criteriaTree', critType, parentId];

    this.subscriptions = [
      this.ngRedux.select(loadPath).subscribe(v => this.loading = v),
      this.ngRedux.select(nodePath).subscribe(n => this.children = n)
    ];

    this.actions.fetchCriteria(critType, parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }
}
