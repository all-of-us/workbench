import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ClrDatagridFilterInterface} from '@clr/angular';
import {Subject} from 'rxjs/Subject';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from 'app/cohort-review/participant.model';

import {
  ParticipantCohortStatusColumns
} from 'generated/fetch';

@Component({
  selector: 'app-multiselect-filter',
  templateUrl: './multiselect-filter.component.html',
})
export class MultiSelectFilterComponent
implements OnDestroy, OnInit, ClrDatagridFilterInterface<Participant> {
  @Input() property: ParticipantCohortStatusColumns;
  @Input() options: any[];

  selection = new FormControl();
  changes = new Subject<any>();
  subscription: Subscription;

  ngOnInit() {
    this.subscription = this.selection.valueChanges.subscribe(
      _ => this.changes.next(true)
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  isActive(): boolean {
    return (this.selection.value && this.selection.value.length > 0);
  }

  accepts(person: Participant): boolean {
    const attr = person[this.property];
    return this.selection.value.includes(attr);
  }

  get isDisabled(): boolean {
    return !this.selection.value;
  }
}
