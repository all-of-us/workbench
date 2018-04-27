import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../redux';

@Component({
  selector: 'crit-multi-select',
  templateUrl: './multi-select.component.html',
  styleUrls: ['./multi-select.component.css']
})
export class MultiSelectComponent implements OnInit, OnDestroy {
  @Input() includeSearchBox = true;
  @Input() options = List();
  @Input() set initialSelection(opts) {
    const _selections = opts.map(opt => opt.hashCode()).toSet();
    this.selected = this.selected.union(_selections);
  }

  selected = Set<number>();
  filter = new FormControl();
  regex = new RegExp('');
  subscription: Subscription;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.filter.valueChanges
      .map(value => value || '')
      .map(value => new RegExp(value, 'i'))
      .subscribe(regex => this.regex = regex);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get filteredOptions() {
    return this.options
      .filter(opt => this.regex.test(opt.get('name', '')))
      .filter(opt => !this.selected.has(opt.hashCode()));
  }

  get selectedOptions() {
    return this.options.filter(opt => this.selected.has(opt.hashCode()));
  }

  select(opt) {
    this.selected = this.selected.add(opt.hashCode());
    this.actions.addParameter(opt);
  }

  unselect(opt) {
    this.selected = this.selected.delete(opt.hashCode());
    this.actions.removeParameter(opt.get('parameterId'));
  }

  unsetFilter() {
    this.filter.setValue(null);
  }
}
