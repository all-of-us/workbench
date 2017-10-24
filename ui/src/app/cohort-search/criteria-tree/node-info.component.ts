import {
  Component,
  ChangeDetectionStrategy,
  HostBinding,
  Input,
  EventEmitter,
  Output,
  ViewEncapsulation
} from '@angular/core';

@Component({
  selector: 'app-criteria-tree-node-info',
  template: `
    <div class="col-lg-8 col-md-8 col-sm-6 text-truncate">
      <small class="font-weight-bold">
        {{ displayName }}
      </small>
      <small *ngIf="node.get('code') && node.get('code') !== node.get('name')" class="text-muted">
        {{ node.get('name') }}
      </small>
    </div>

    <div class="col-lg col-md col-sm text-right">
      <span *ngIf="nonZeroCount" class="badge badge-light-blue">
        {{ node.get('count') }}
      </span>
    </div>

    <div class="col-lg-1 col-md-1 col-sm-1">
      <button
        *ngIf="selectable"
        type="button"
        class="btn btn-sm btn-link"
        style="margin: 0 0 0 0;"
        (click)="select($event)">
        <clr-icon shape="plus-circle"></clr-icon>
      </button>
    </div>
  `,
})
export class CriteriaTreeNodeInfoComponent {
  @Input() node;
  @Output() onSelect = new EventEmitter<boolean>();

  @HostBinding('class.clr-treenode-link') clrTreenodeLink = true;

  select(event) { this.onSelect.emit(true); }
  get nonZeroCount() { return this.node.get('count', 0) > 0; }
  get selectable() { return this.node.get('selectable', false); }
  get displayName() { return this.node.get('code') || this.node.get('name'); }
}
