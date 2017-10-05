import {
  Component,
  OnDestroy,
  OnInit,
  Input,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../actions';
import {CohortSearchState} from '../store';


@Component({
  selector: 'app-tree-root',
  templateUrl: './root.component.html',
  encapsulation: ViewEncapsulation.None,
})
export class CriteriaTreeRootComponent implements OnInit, OnDestroy {

  @Input() critType: string;
  @Input() parentId: number;

  children;
  loading;

  subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const loadPath = ['loading', this.critType, this.parentId];
    const nodePath = ['criteriaTree', this.critType, this.parentId];

    this.subscriptions = [
      this.ngRedux.select(loadPath).subscribe(v => this.loading = v),
      this.ngRedux.select(nodePath).subscribe(n => this.children = n)
    ];

    this.actions.fetchCriteria(this.critType, this.parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  trackById(index, node) {
    return node ? node.id : undefined;
  }
}
