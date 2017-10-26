import {
  Component,
  OnDestroy,
  OnInit,
  Input,
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
  selector: 'app-criteria-tree-node',
  template: `
    <ng-container [clrLoading]="loading">
      <clr-tree-node *ngFor="let node of children; trackBy: trackById">

        <app-criteria-tree-node-info
          [node]="node"
          (onSelect)="handleSelection(node)">
        </app-criteria-tree-node-info>

        <ng-template clrIfExpanded *ngIf="node.get('group')">
          <app-criteria-tree-node [node]="node">
          </app-criteria-tree-node>
        </ng-template>

      </clr-tree-node>
    </ng-container>
  `,
  encapsulation: ViewEncapsulation.None,
  styles: [`
    .clr-treenode-children {
      overflow: hidden!important;
    }
  `]
})
export class CriteriaTreeNodeComponent implements OnInit, OnDestroy {
  @Input() node;

  private children;
  private loading;
  private subscriptions: Subscription[];

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const _type = this.node.get('type').toLowerCase();
    const _parentId = this.node.get('id');
    const children$ = this.ngRedux.select(criteriaChildren(_type, _parentId));
    const loading$ = this.ngRedux.select(isCriteriaLoading(_type, _parentId));
    this.subscriptions = [
      children$.subscribe(value => this.children = value),
      loading$.subscribe(value => this.loading = value)
    ];
    this.actions.fetchCriteria(_type, _parentId);
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
