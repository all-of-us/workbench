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

  @Input() resourceType: string;
  @Output() receiveRename = new EventEmitter<object>();
  @Input() resource: any;

  open(): void {
    this.renaming = true;
  }

  close(): void {
    this.renaming = false;
  }
}
