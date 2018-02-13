import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ClrDatagridFilterInterface} from '@clr/angular';
import {Subject} from 'rxjs/Subject';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';

import {
  Filter,
  Operator,
  ParticipantCohortStatusColumns,
} from 'generated';

@Component({
  selector: 'app-choice-filter',
  templateUrl: './choice-filter.component.html',
})
export class ChoiceFilterComponent
implements OnDestroy, OnInit, ClrDatagridFilterInterface<Participant> {
  @Input() property: ParticipantCohortStatusColumns;
  @Input() options: any[];

  selection = new FormControl();
  changes = new Subject<any>();
  subscription: Subscription;

  ngOnInit() {
    this.selection.valueChanges.subscribe(_ => this.changes.next(true));
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

  toFilters(): Filter[] {
    const property = this.property;
    const operator = Operator.Equal;
    return this.selection.value.map(value => (<Filter>{property, value, operator}));
  }
}
