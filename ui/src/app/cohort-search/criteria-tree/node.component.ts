import {
  Component,
  OnDestroy,
  OnInit,
  Input,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  criteriaPath,
  isLoading,
} from '../redux';


@Component({
  selector: 'app-criteria-tree-node',
  template: `
    <ng-container [clrLoading]="loading">
      <clr-tree-node *ngFor="let node of children; trackBy: trackById">

        <app-criteria-tree-node-info [node]="node">
        </app-criteria-tree-node-info>

        <span *ngIf="node.get('group')">
          <ng-template clrIfExpanded>
            <app-criteria-tree-node [node]="node">
            </app-criteria-tree-node>
          </ng-template>
        </span>

      </clr-tree-node>
    </ng-container>
  `,
  encapsulation: ViewEncapsulation.None,
})
export class CriteriaTreeNodeComponent implements OnInit, OnDestroy {
  @Input() node;

  children;
  loading;

  subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const critType = this.node.get('type').toLowerCase();
    const parentId = this.node.get('id');
    const path = criteriaPath(critType, parentId);
    this.subscriptions = [
      this.ngRedux.select(
        isLoading(path)
      ).subscribe(v => this.loading = v),

      this.ngRedux.select(
        path.toArray()
      ).subscribe(n => this.children = n)
    ];
    this.actions.fetchCriteria(critType, parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  trackById(index, node) {
    return node ? node.get('id') : undefined;
  }
}
