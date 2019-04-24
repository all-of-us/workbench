import {NgRedux, select} from '@angular-redux/store';
import {Component, HostListener, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {
  autocompleteError,
  CohortSearchActions,
  CohortSearchState,
  ingredientsForBrand,
  isAutocompleteLoading,
} from 'app/cohort-search/redux';
import {
  autocompleteStore,
  selectedPathStore,
  selectedStore
} from 'app/cohort-search/search-state.service';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {TreeSubType, TreeType} from 'generated';
import {Subscription} from 'rxjs/Subscription';

const trigger = 2;

@Component({
  selector: 'app-list-search-bar',
  templateUrl: './list-search-bar.component.html',
  styleUrls: ['./list-search-bar.component.css']
})
export class ListSearchBarComponent implements OnInit, OnDestroy {
  @Input() _type;
  searchTerm: FormControl = new FormControl();
  typedTerm: string;
  options = [];
  loading = false;
  noResults = false;
  optionSelected = false;
  error = false;
  subscription: Subscription;
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
    this.subscription = autocompleteStore.subscribe(searchTerm => {
      this.searchTerm.setValue(searchTerm, {emitEvent: false});
      console.log(this.searchTerm.value);
    });
    // TODO set to false for now, may need to change for conditions/procedures
    this.codes = false;

    const loadingSub = this.ngRedux
      .select(isAutocompleteLoading())
      .subscribe(loading => this.loading = loading);

    const ingredientSub = this.ngRedux
      .select(ingredientsForBrand())
      .subscribe(ingredients => {
        this.ingredientList = [];
        const ids = [];
        let path = [];
        ingredients.forEach(item => {
          if (!this.ingredientList.includes(item.name)) {
            this.ingredientList.push(item.name);
            ids.push(item.id);
            path = path.concat(item.path.split('.'));
          }
        });
        if (this.ingredientList.length) {
          this.actions.setCriteriaSearchTerms(this.ingredientList);
          this.actions.loadCriteriaSubtree(this._type, TreeSubType[TreeSubType.BRAND], ids, path);
        }
      });

    const inputSub = this.searchTerm.valueChanges
      .debounceTime(300)
      .distinctUntilChanged()
      .subscribe( value => {
        console.log(value);
        if (value.length >= trigger) {
          this.inputChange();
        } else {
          if (!this.optionSelected) {
            this.actions.setCriteriaSearchTerms([]);
          }
          this.options = [];
          this.noResults = false;
        }
      });

    this.subscription.add(loadingSub);
    this.subscription.add(ingredientSub);
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
      this.loading = true;
      this.optionSelected = false;
      this.ingredientList = [];
      this.noResults = false;
      // const subtype = this.codes ? this.subtype : null;
      const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
      cohortBuilderApi().getCriteriaAutoComplete(cdrId, this._type, this.searchTerm.value)
        .then(resp => {
          this.options = [];
          const optionNames: Array<string> = [];
          resp.items.forEach(option => {
            this.highlightedOption = null;
            if (optionNames.indexOf(option.name) === -1) {
              optionNames.push(option.name);
              this.options.push(option);
            }
          });
          this.loading = false;
        }, (err) => this.error = err);
    }
  }

  get showOverflow() {
    return this.options && this.options.length <= 10;
  }

  selectOption(option: any) {
    if (option) {
      this.optionSelected = true;
      this.searchTerm.reset('');
      this.searchTerm.setValue(option.name, {emitEvent: false});
      if (option.subtype === TreeSubType[TreeSubType.BRAND]) {
        this.actions.fetchIngredientsForBrand(option.conceptId);
      } else {
        autocompleteStore.next(option.name);
        selectedPathStore.next(option.path.split('.'));
        selectedStore.next(option.id);
      }
    }
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
