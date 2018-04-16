import {Component, Input, OnInit, OnDestroy} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../../redux';

/*
 * Sorts a plain JS array of plain JS objects first by a 'count' key and then
 * by a 'name' key
 */
function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
    ? (critA.name > critB.name ? 1 : -1)
    : diff;
}

@Component({
  selector: 'app-demo-select',
  templateUrl: './demo-select.component.html',
  styleUrls: ['./demo-select.component.css']
})
export class DemoSelectComponent implements OnInit, OnDestroy {
  @Input() label: string;
  @Input() includeSearchBox = true;
  @Input() options;
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
