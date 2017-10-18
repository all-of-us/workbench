import {
  Component,
  ChangeDetectionStrategy,
  Input,
  EventEmitter,
  Output,
  ViewEncapsulation
} from '@angular/core';

@Component({
  selector: 'app-criteria-tree-node-info',
  template: `
    <div class="row clr-treenode-link">

      <!-- Code/Name -->
      <div class="col-lg-9 col-md-9 col-sm-9 col-xs-9 text-truncate">
        <span *ngIf="node.get('code'); then showCode else noCode"></span>
        <ng-template #showCode>
          <small class="font-weight-bold">{{ node.get('code') }}</small>
          <small class="text-muted">{{ node.get('name') }}</small>
        </ng-template>
        <ng-template #noCode>
          <small class="font-weight-bold">{{ node.get('name') }}</small>
        </ng-template>
      </div>

      <div class="col-lg-3 col-md-3 col-sm-3 col-xs-3 text-right">
        <!-- Count -->
        <span *ngIf="node.get('count') === 0; then noCount else count"></span>
        <ng-template #count>
          <span class="badge badge-light-blue">{{ node.get('count') }}&nbsp;&nbsp;</span>
        </ng-template>
        <ng-template #noCount>
          <span class="badge badge-light-blue invisible">{{ node.get('count') }}&nbsp;&nbsp;</span>
        </ng-template>

        <!-- Selectable -->
        <span *ngIf="node.get('selectable'); then selectable else notSelectable"></span>
        <ng-template #notSelectable>
          <button type="button" class="btn btn-link btn-sm invisible">
            <clr-icon shape="plus-circle" size="20"></clr-icon></button>
        </ng-template>
        <ng-template #selectable>
          <button type="button" class="btn btn-link btn-sm" (click)="select($event)">
            <clr-icon shape="plus-circle" size="20"></clr-icon></button>
        </ng-template>

      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class CriteriaTreeNodeInfoComponent {
  @Input() node;
  @Output() onSelect = new EventEmitter<boolean>();

  select(event) { this.onSelect.emit(true); }
}
