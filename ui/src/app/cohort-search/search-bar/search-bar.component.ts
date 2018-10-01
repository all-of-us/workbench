import {NgRedux, select} from '@angular-redux/store';
import {
  Component,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import {FormControl} from '@angular/forms';
import {TreeSubType, TreeType} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {
  activeCriteriaSubtype,
  autocompleteError,
  autocompleteOptions,
  CohortSearchActions,
  CohortSearchState,
  ingredientsForBrand,
  isAutocompleteLoading,
  subtreeSelected,
} from '../redux';

import {highlightMatches, stripHtml} from '../utils';

const trigger = 2;

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  @select(activeCriteriaSubtype) subtype$: Observable<string>;
  @select(subtreeSelected) selected$: Observable<any>;
  @Input() _type;
  searchTerm: FormControl = new FormControl('');
  typedTerm: string;
  options = [];
  multiples: any;
  loading = false;
  noResults = false;
  optionSelected = false;
  error = false;
  subscription: Subscription;
  numMatches: number;
  ingredientList = [];
  highlightedOption: number;
  subtype: string;
  codes: any;

  @ViewChild('searchBar') searchBar;

  @HostListener('document:mouseup', ['$event.target'])
  onClick(targetElement) {
    const clickedInside = this.searchBar.nativeElement.contains(targetElement);
    if (!clickedInside) {
      this.hideDropdown();
    }
  }

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions
  ) {}

  ngOnInit() {
    this.codes = this.ngRedux.getState().getIn(['wizard', 'codes']);
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
        if (this.triggerSearch) {
          this.options = [];
          this.multiples = {};
          const optionNames = [];
          if (options !== null) {
            options.forEach(option => {
              this.highlightedOption = null;
              if (optionNames.indexOf(option.name) === -1) {
                optionNames.push(option.name);
                option.displayName = highlightMatches([this.searchTerm.value], option.name);
                this.options.push(option);
              } else {
                if (this.multiples[option.name]) {
                  this.multiples[option.name].push({id: option.id, path: option.path});
                } else {
                  this.multiples[option.name] = [{id: option.id, path: option.path}];
                }
              }
            });
          }
          this.noResults = !this.optionSelected
            && !this.options.length;
        }
      });

    const ingredientSub = this.ngRedux
      .select(ingredientsForBrand())
      .subscribe(ingredients => {
        this.ingredientList = [];
        const ids = [];
        let path = [];
        ingredients.forEach(item => {
          if (!this.ingredientList.includes(item.name)) {
            this.ingredientList.push(item.name);
          }
          ids.push(item.id);
          path = path.concat(item.path.split('.'));
        });
        if (this.ingredientList.length) {
          this.actions.setCriteriaSearchTerms(this.ingredientList);
          this.actions.loadCriteriaSubtree(this._type, TreeSubType[TreeSubType.BRAND], ids, path);
        }
      });

    const subtreeSelectSub = this.selected$
      .filter(selectedIds => !!selectedIds)
      .subscribe(selectedIds => this.numMatches = selectedIds.length);

    const subtypeSub = this.subtype$
      .subscribe(subtype => {
        this.searchTerm.setValue('');
        this.subtype = subtype;
      });

    const inputSub = this.searchTerm.valueChanges
      .debounceTime(300)
      .distinctUntilChanged()
      .subscribe( value => {
        if (value.length >= trigger) {
          this.inputChange();
        } else {
          this.actions.setCriteriaSearchTerms([]);
          this.options = [];
        }
      });

    this.subscription = errorSub;
    this.subscription.add(loadingSub);
    this.subscription.add(optionsSub);
    this.subscription.add(ingredientSub);
    this.subscription.add(subtreeSelectSub);
    this.subscription.add(subtypeSub);
    this.subscription.add(inputSub);
  }

  ngOnDestroy() {
    this.options = [];
    this.subscription.unsubscribe();
  }

  inputChange() {
    this.typedTerm = this.searchTerm.value;
    if (this._type === TreeType[TreeType.VISIT] || this._type === TreeType[TreeType.PM]) {
      this.actions.setCriteriaSearchTerms([this.searchTerm.value]);
    } else {
      this.optionSelected = false;
      this.ingredientList = [];
      this.numMatches = 0;
      this.noResults = false;
      const subtype = this.codes ? this.subtype : null;
      this.actions.fetchAutocompleteOptions(this._type, subtype, this.searchTerm.value);
    }
  }

  get triggerSearch() {
    return this.searchTerm.value.length >= trigger;
  }

  selectOption(option: any) {
    console.log(option);
    this.optionSelected = true;
    this.searchTerm.setValue(option.name, {emitEvent: false});
    if (option.subtype === TreeSubType[TreeSubType.BRAND]) {
      this.actions.fetchIngredientsForBrand(option.conceptId);
    } else {
      this.actions.setCriteriaSearchTerms([option.name]);
      const ids = [option.id];
      let path = option.path.split('.');
      if (this.multiples[option.name]) {
        this.multiples[option.name].forEach(multiple => {
          ids.push(multiple.id);
          path = path.concat(multiple.path.split('.'));
        });
      }
      this.actions.loadCriteriaSubtree(this._type, option.subtype, ids, path);
    }
    this.actions.clearAutocompleteOptions();
  }

  hideDropdown() {
    this.options = [];
  }

  moveUp() {
    if (this.highlightedOption === 0) {
      this.highlightedOption = null;
      this.searchTerm.setValue(this.typedTerm, {emitEvent: false});
    } else if (this.highlightedOption > 0) {
      this.highlightedOption--;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    }
  }

  moveDown() {
    if (this.highlightedOption === null) {
      this.highlightedOption = 0;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    } else if ((this.highlightedOption + 1) < this.options.length) {
      this.highlightedOption++;
      this.searchTerm.setValue(this.options[this.highlightedOption].name, {emitEvent: false});
    }
  }

  enterSelect() {
    this.selectOption(this.options[this.highlightedOption]);
  }
}
