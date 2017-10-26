import {
  Component,
  HostBinding,
  Input,
  EventEmitter,
  Output,
} from '@angular/core';

@Component({
  selector: 'app-criteria-tree-node-info',
  templateUrl: './node-info.component.html',
})
export class CriteriaTreeNodeInfoComponent {
  @Input() node;
  @Output() onSelect = new EventEmitter<boolean>();

  @HostBinding('class.clr-treenode-link') clrTreenodeLink = true;

  select(event) {
    this.onSelect.emit(true);
  }

  get nonZeroCount() {
    return this.node.get('count', 0) > 0;
  }

  get selectable() {
    return this.node.get('selectable', false);
  }

  get displayName() {
    const isDemo = this.node.get('type', '').match(/^DEMO.*/i);
    const nameIsCode = this.node.get('name', '') === this.node.get('code', '');
    return nameIsCode || isDemo
      ? ''
      : this.node.get('name', '');
  }

  get displayCode() {
    return this.node.get('type', '').match(/^DEMO.*/i)
      ? this.node.get('name', '')
      : this.node.get('code', '');
  }
}
