import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {
  autocompleteError,
  autocompleteOptions,
  CohortSearchActions,
  CohortSearchState,
  isAutocompleteLoading,
} from '../redux';
import {fromJS} from 'immutable';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  @Input() _type;
  searchTerm = '';
  options = [];
  loading = false;
  error = false;
  subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    const errorSub = this.ngRedux
      .select(autocompleteError())
      .map(err => !(err === null || err === undefined))
      .subscribe(err => this.error = err);

    const loadingSub = this.ngRedux
      .select(isAutocompleteLoading())
      .subscribe(loading => this.loading = loading);

    const optionsSub = this.ngRedux
      .select(autocompleteOptions())
      .subscribe(options => {
        console.log(options.toJS());
      });

    this.subscription = errorSub;
    this.subscription.add(loadingSub);
    this.subscription.add(optionsSub);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  inputChange(newVal: string) {
    if (this._type === 'visit') {
      this.actions.setCriteriaSearchTerms(newVal);
    }
    if (this._type === 'drug') {
      if (newVal.length >= 4) {
        this.actions.fetchAutocompleteOptions(newVal);
      } else {
        this.options = [];
      }
    }
  }
}
