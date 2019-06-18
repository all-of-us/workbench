import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {activeParameterList, CohortSearchActions, CohortSearchState} from 'app/cohort-search/redux';
import {TreeType} from 'generated';
import {List, Set} from 'immutable';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'crit-list-multi-select',
  templateUrl: './list-multi-select.component.html',
  styleUrls: ['./list-multi-select.component.css']
})
export class ListMultiSelectComponent implements OnInit, OnDestroy {
  @Input() includeSearchBox = true;
  @Input() options = List();
  @Input() set initialSelection(opts) {
    const _selections = opts.map(opt => opt.get('parameterId')).toSet();
    this.selected = this.selected.union(_selections);
  }
  @Input() loading: boolean;
  selected = Set<number>();
  filter = new FormControl();
  regex = new RegExp('');
  subscription: Subscription;
  selectedOption: any;
  constructor(private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>) {}


  ngOnInit() {
    this.subscription = this.filter.valueChanges
            .map(value => value || '')
            .map(value => new RegExp(value, 'i'))
            .subscribe(regex => this.regex = regex);

    this.subscription.add (this.ngRedux
      .select(activeParameterList)
      .subscribe(val => {
        this.selectedOption = [];
        val.forEach( paramList => {
          if (paramList.get('type') === TreeType.DEMO) {
            this.selectedOption.push(paramList.get('parameterId'));
          }
        });
      }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get filteredOptions() {
    return this.options
      .filter(opt => this.regex.test(opt.get('name', '')));
  }

  select(opt) {
    this.actions.addParameter(opt);
  }

  unsetFilter() {
    this.filter.setValue(null);
  }
}
