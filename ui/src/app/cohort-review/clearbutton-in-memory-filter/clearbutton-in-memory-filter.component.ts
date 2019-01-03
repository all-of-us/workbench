import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ClrDatagridFilterInterface} from '@clr/angular';
import {Subject} from 'rxjs/Subject';
import {Subscription} from 'rxjs/Subscription';
import {Participant} from '../participant.model';


@Component({
  selector: 'app-clearbutton-in-memory-filter',
  templateUrl: './clearbutton-in-memory-filter.component.html',
})
export class ClearButtonInMemoryFilterComponent
  implements OnDestroy, OnInit, ClrDatagridFilterInterface<Participant> {
  @Input() property: string;
  selection = new FormControl();
  @Output()
  filterChanges: EventEmitter<any> = new EventEmitter<any>();
  changes = new Subject<any>();
  subscription: Subscription;

  ngOnInit() {
    this.subscription = this.selection.valueChanges.subscribe(
      _ => this.changes.next(true)
    );
  }

  isActive(): boolean {
    return this.selection.value;
  }

  accepts(person: Participant): boolean {
    return this.selection.value === '' ||
      ('' + person[this.property]).toLowerCase().startsWith(this.selection.value.toLowerCase());
  }

  refreshData() {
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
    return !this.selection.value;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
