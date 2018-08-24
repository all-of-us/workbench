import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {DomainType} from 'generated';
import {Subscription} from 'rxjs/Subscription';
import {CRITERIA_SUBTYPES, CRITERIA_TYPES} from '../constant';
import {
  autocompleteError,
  autocompleteOptions,
  CohortSearchActions,
  CohortSearchState,
  ingredientsForBrand,
  isAutocompleteLoading,
} from '../redux';

import {highlightMatches} from '../utils';

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
  optionSelected = false;
  multiIngredient = false;
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
        if (this.searchTerm.length >= 4) {
          this.options = [];
          const optionNames = [];
          options.forEach(option => {
            if (optionNames.indexOf(option.name) === -1) {
              optionNames.push(option.name);
              option.displayName = highlightMatches([this.searchTerm], option.name);
              this.options.push(option);
            }
          });

          this.noResults = this._type === DomainType.DRUG
            && !this.optionSelected
            && !this.options.length;
        }
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
        this.multiIngredient = ingredientList.length > 1;
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
    switch (this._type) {
      case DomainType.VISIT:
        if (newVal.length > 2) {
          this.actions.setCriteriaSearchTerms([newVal]);
        } else {
          this.actions.setCriteriaSearchTerms([]);
        }
        break;
      default:
        this.optionSelected = false;
        this.multiIngredient = false;
        this.noResults = false;
        if (newVal.length >= 4) {
          this.actions.fetchAutocompleteOptions(this._type, newVal);
        } else {
          this.actions.setCriteriaSearchTerms([]);
          this.options = [];
        }
    }
  }

  selectOption(option: any) {
    this.optionSelected = true;
    this.actions.clearAutocompleteOptions();
    this.searchTerm = option.name;
    switch (this._type) {
      case DomainType.DRUG:
        if (option.subtype === CRITERIA_SUBTYPES.BRAND) {
          this.actions.fetchIngredientsForBrand(option.conceptId);
        } else if (option.subtype === CRITERIA_SUBTYPES.ATC) {
          this.actions.setCriteriaSearchTerms([option.name]);
          this.actions.loadCriteriaSubtree(this._type, option.id, option.path);
        }
        break;
      default:
        this.actions.setCriteriaSearchTerms([option.name]);
        this.actions.fetchCriteriaSubtree(this._type, option.id);
    }
  }
}
