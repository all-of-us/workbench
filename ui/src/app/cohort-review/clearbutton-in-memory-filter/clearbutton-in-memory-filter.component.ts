import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ClrDatagridFilterInterface} from '@clr/angular';

import {Participant} from '../participant.model';

@Component({
  selector: 'app-clearbutton-in-memory-filter',
  templateUrl: './clearbutton-in-memory-filter.component.html',
})
export class ClearButtonInMemoryFilterComponent
  implements ClrDatagridFilterInterface<Participant> {
  @Input() property: string;

  selection = new FormControl();
  changes: EventEmitter<any> = new EventEmitter<any>(false);
  @Output()
  filterChanges: EventEmitter<any> = new EventEmitter<any>();
  filteredColumns = [];

  isActive(): boolean {
    return this.selection.value;
  }

  accepts(person: Participant): boolean {
    return this.selection.value === '' ||
      ('' + person[this.property]).toLowerCase().startsWith(this.selection.value.toLowerCase());
  }

  refreshData() {
    this.changes.emit(true);
    if (this.selection.value) {
      this.filterChanges.emit({column: this.property, action: 'add'});
    } else {
      this.filterChanges.emit({column: this.property, action: 'remove'});
    }
  }

  reset() {
    this.selection.reset();
    this.filterChanges.emit({column: this.property, action: 'remove'});
  }

  get isDisabled(): boolean {
    this.changes.emit(true);
    return !this.selection.value;
  }
}
