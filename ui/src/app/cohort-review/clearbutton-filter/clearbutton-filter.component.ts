import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ClrDatagridFilterInterface} from '@clr/angular';

import {Participant} from 'app/cohort-review/participant.model';

import {
  ParticipantCohortStatusColumns,
} from 'generated/fetch';
import {Subject} from 'rxjs/Subject';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-clearbutton-filter',
  templateUrl: './clearbutton-filter.component.html',
})
export class ClearButtonFilterComponent
  implements OnDestroy, OnInit, ClrDatagridFilterInterface<Participant> {
  @Input() property: ParticipantCohortStatusColumns;

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
    return this.selection.value;
  }

  accepts(person: Participant): boolean {
    return this.selection.value === person[this.property];
  }

  get isDisabled(): boolean {
    return !this.selection.value;
  }
}
