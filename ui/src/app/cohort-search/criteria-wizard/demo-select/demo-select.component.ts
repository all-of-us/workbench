import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl} from '@angular/forms';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

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
export class DemoSelectComponent implements OnInit {
  @Input() label: string;
  @Input() includeSearchBox: boolean = true;
  @Input() options;
  @Output() selected = new EventEmitter<any>();

  _selected = Set<number>();
  filter = new FormControl();
  regex = new RegExp('');
  subscription: Subscription;

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
      .filter(opt => !this._selected.has(opt.hashCode()));
  }

  get selectedOptions() {
    return this.options.filter(opt => this._selected.has(opt.hashCode()));
  }

  select(opt) {
    this._selected = this._selected.add(opt.hashCode());
    this.selected.emit(this.selectedOptions);
  }

  unselect(opt) {
    this._selected = this._selected.delete(opt.hashCode());
    this.selected.emit(this.selectedOptions);
  }

  unsetFilter() {
    this.filter.setValue(null);
  }
}
