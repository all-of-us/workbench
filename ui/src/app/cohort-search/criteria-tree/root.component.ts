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
  isRequesting,
} from '../redux';


@Component({
  selector: 'app-criteria-tree-root',
  template: `
    <span *ngIf="loading; then requestingNodes else nodesLoaded"></span>

    <ng-template #requestingNodes>
      <span class="spinner spinner-inline">Loading...</span>
      <span>Loading...</span>
    </ng-template>

    <ng-template #nodesLoaded>
      <clr-tree-node *ngFor="let node of children; trackBy: trackById">
        <app-criteria-tree-node-info [node]="node">
        </app-criteria-tree-node-info>

        <ng-template clrIfExpanded>
          <app-criteria-tree-node [node]="node">
          </app-criteria-tree-node>
        </ng-template>
      </clr-tree-node>
    </ng-template>
  `,
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
    const path = criteriaPath(this.critType, this.parentId);
    this.subscriptions = [
      this.ngRedux.select(
        isRequesting('criteria', path.rest())
      ).subscribe(v => this.loading = v),

      this.ngRedux.select(
        path.toArray()
      ).subscribe(n => this.children = n)
    ];
    this.actions.fetchCriteria(this.critType, this.parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  trackById(index, node) {
    return node ? node.get('id') : undefined;
  }
}
