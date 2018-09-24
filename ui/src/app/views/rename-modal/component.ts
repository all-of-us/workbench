import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-rename-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class RenameModalComponent {
  public renaming = false;
  public newName = '';

  @Output() receiveRename = new EventEmitter<object>();
  @Input() resource: any;

  loading = false;

  open(): void {
    this.renaming = true;
    this.loading = false;
  }

  close(): void {
    this.renaming = false;
  }

  emitRename(resource: any): void {
    if (!this.loading) {
      this.loading = true;
      this.receiveRename.emit({name: resource.name, newName: this.newName});
    }
  }
}
