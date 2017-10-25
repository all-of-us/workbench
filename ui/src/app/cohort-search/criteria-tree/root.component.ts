import {
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewEncapsulation,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  criteriaChildren,
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
        <app-criteria-tree-node-info
          [node]="node"
          (onSelect)="handleSelection(node)"
        >
        </app-criteria-tree-node-info>

        <ng-template clrIfExpanded>
          <app-criteria-tree-node [node]="node">
          </app-criteria-tree-node>
        </ng-template>
      </clr-tree-node>
    </ng-template>
  `,
})
export class CriteriaTreeRootComponent implements OnInit, OnDestroy {

  @Input() critType: string;
  @Input() parentId: number;

  private children;
  private loading;
  private subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const children$ = this.ngRedux.select(criteriaChildren(this.critType, this.parentId));
    const loading$ = this.ngRedux.select(isCriteriaLoading(this.critType, this.parentId));
    this.subscriptions = [
      children$.subscribe(value => this.children = value),
      loading$.subscribe(value => this.loading = value)
    ];
    this.actions.fetchCriteria(this.critType, this.parentId);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  handleSelection(node) {
    this.actions.selectCriteria(node);
  }

  trackById(index, node) {
    return node ? node.get('id') : undefined;
  }
}
