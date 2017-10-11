import {
  Component,
  ChangeDetectionStrategy,
  Input,
  ViewEncapsulation
} from '@angular/core';
import {CohortSearchActions} from '../redux';
import {Criteria} from 'generated';


@Component({
  selector: 'app-criteria-tree-node-info',
  template: `
    <div class="row clr-treenode-link">

      <!-- Code/Name -->
      <div class="col-lg-9 col-md-9 col-sm-9 col-xs-9 text-truncate">
        <span *ngIf="node.code; then showCode else noCode"></span>
        <ng-template #showCode>
          <small class="font-weight-bold">{{ node.code }}</small>
          <small class="text-muted">{{ node.name }}</small>
        </ng-template>
        <ng-template #noCode>
          <small class="font-weight-bold">{{ node.name }}</small>
        </ng-template>
      </div>

      <div class="col-lg-3 col-md-3 col-sm-3 col-xs-3 text-right">
        <!-- Count -->
        <span *ngIf="node.count === 0; then noCount else count"></span>
        <ng-template #count>
          <span class="badge badge-light-blue">{{ node.count }}&nbsp;&nbsp;</span>
        </ng-template>
        <ng-template #noCount>
          <span class="badge badge-light-blue invisible">{{ node.count }}&nbsp;&nbsp;</span>
        </ng-template>

        <!-- Selectable -->
        <span *ngIf="node.selectable; then selectable else notSelectable"></span>
        <ng-template #notSelectable>
          <button type="button" class="btn btn-link btn-sm invisible">
            <clr-icon shape="plus-circle" size="20"></clr-icon></button>
        </ng-template>
        <ng-template #selectable>
          <button type="button" class="btn btn-link btn-sm" (click)="actions.selectCriteria(node)">
            <clr-icon shape="plus-circle" size="20"></clr-icon></button>
        </ng-template>

      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class CriteriaTreeNodeInfoComponent {
  @Input() node: Criteria;

  constructor(private actions: CohortSearchActions) {}
}
