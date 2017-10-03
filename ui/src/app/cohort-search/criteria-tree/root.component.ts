import {Component, OnDestroy, OnInit, Input, ViewEncapsulation} from '@angular/core';
import {NgRedux, select, dispatch} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import 'rxjs/add/operator/mergeMap';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store.interfaces';
import {CohortBuilderService, Criteria, SearchParameter} from 'generated';


@Component({
  selector: 'app-tree-root',
  templateUrl: './root.component.html',
})
export class CriteriaTreeRootComponent implements OnInit, OnDestroy {

  @Input() criteriaType: string;
  @Input() parentId: number;

  children;
  loading;

  subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const loadPath = ['loading', this.criteriaType, this.parentId];
    const nodePath = ['criteriaTree', this.criteriaType, this.parentId];

    this.subscriptions = [
      this.ngRedux.select(loadPath).subscribe(v => this.loading = v),
      this.ngRedux.select(nodePath).subscribe(n => this.children = n)
    ];

    this.actions.fetchCriteria(this.criteriaType, this.parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }
}
