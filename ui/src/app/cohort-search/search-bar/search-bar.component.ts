import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {
  autocompleteError,
  autocompleteOptions,
  CohortSearchActions,
  CohortSearchState,
  ingredientsForBrand,
  isAutocompleteLoading,
} from '../redux';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  @Input() _type;
  searchTerm = '';
  options = [];
  ingredients: any;
  loading = false;
  noResults = false;
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
        this.options = options;
        this.noResults = this._type === 'drug'
          && !this.options.length
          && this.searchTerm.length >= 4;
      });

    const ingredientSub = this.ngRedux
      .select(ingredientsForBrand())
      .subscribe(ingredients => {
        this.ingredients = ingredients;
        const ingredientList = [];
        this.ingredients.forEach(item => {
          ingredientList.push(item.name);
        });
        if (ingredientList.length) {
          this.actions.setCriteriaSearchTerms(ingredientList);
        }
      });

    this.subscription = errorSub;
    this.subscription.add(loadingSub);
    this.subscription.add(optionsSub);
    this.subscription.add(ingredientSub);
  }

  ngOnDestroy() {
    this.options = [];
    this.subscription.unsubscribe();
  }

  inputChange(newVal: string) {
    if (this._type === 'visit') {
      if (newVal.length > 2) {
        this.actions.setCriteriaSearchTerms([newVal]);
      } else {
        this.actions.setCriteriaSearchTerms([]);
      }
    }
    if (this._type === 'drug') {
      if (newVal.length >= 4) {
        this.actions.fetchAutocompleteOptions(newVal);
      } else {
        this.actions.setCriteriaSearchTerms([]);
        this.options = [];
      }
    }
  }

  selectOption(option: any) {
    this.actions.clearAutocompleteOptions();
    this.searchTerm = option.name;
    if (option.subtype === 'BRAND') {
      this.actions.fetchIngredientsForBrand(option.conceptId);
    } else if (option.subtype === 'ATC') {
      this.actions.setCriteriaSearchTerms([option.name]);
    }
  }
}
