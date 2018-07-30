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
              option.displayName = option.name;
              const start = option.name.toLowerCase().indexOf(this.searchTerm.toLowerCase());
              if (start > -1) {
                const end = start + this.searchTerm.length;
                option.displayName = option.name.slice(0, start)
                  + '<span style="color: #659F3D;'
                  + 'font-weight: bolder;'
                  + 'background-color: rgba(101,159,61,0.2);'
                  + 'padding: 2px 0;">'
                  + option.name.slice(start, end) + '</span>'
                  + option.name.slice(end);
              }
              this.options.push(option);
            }
          });
          this.noResults = this._type === 'drug'
            && !this.optionSelected
            && !this.options.length;
        }
      });

    const ingredientSub = this.ngRedux
      .select(ingredientsForBrand())
      .subscribe(ingredients => {
        this.ingredients = ingredients;
        const ingredientList = [];
        console.log(ingredients);
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
    if (this._type === 'visit') {
      if (newVal.length > 2) {
        this.actions.setCriteriaSearchTerms([newVal]);
      } else {
        this.actions.setCriteriaSearchTerms([]);
      }
    }
    if (this._type === 'drug') {
      this.optionSelected = false;
      this.multiIngredient = false;
      this.noResults = false;
      if (newVal.length >= 4) {
        this.actions.fetchAutocompleteOptions(newVal);
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
    if (option.subtype === 'BRAND') {
      this.actions.fetchIngredientsForBrand(option.conceptId);
    } else if (option.subtype === 'ATC') {
      this.actions.setCriteriaSearchTerms([option.name]);
    }
  }
}
