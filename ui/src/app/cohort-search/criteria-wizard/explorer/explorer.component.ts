import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {fromJS, Map} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {
  CohortSearchActions,
  CohortSearchState,
  criteriaLoadErrors,
  focusedCriterion,
  isCriteriaLoading,
} from '../../redux';

import {CohortBuilderService} from 'generated';

/* Modes of operation */
const Tree = 'Tree';
const Search = 'Search';
const SetAttr = 'SetAttr';

@Component({
  selector: 'crit-explorer',
  templateUrl: './explorer.component.html',
  styleUrls: ['./explorer.component.css']
})
export class ExplorerComponent implements OnInit {
  @Input() criteriaType: string;
  private readonly parentId = 0;  /* Root parent ID is always zero */

  private searchValue = '';
  private searchResults;
  private settingAttributes = false;

  private loading$: Observable<boolean>;
  private errors$: Observable<any>;
  private nodeInFocus$: Observable<Map<any, any>>;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
    private api: CohortBuilderService,
  ) {}

  ngOnInit() {
    const _type = <string>this.rootNode.get('type');
    const id = <number>this.rootNode.get('id');

    // TODO(jms) - loading now needs to listen for either the roots loading or
    // the quicksearch results loading as appropriate
    this.loading$ = this.ngRedux.select(isCriteriaLoading(_type, id));
    this.errors$ = this.ngRedux.select(criteriaLoadErrors).map(errSet =>
      errSet
        .filter((_, key) => key.first() === this.criteriaType)
        .map((val, key) => ({
          kind: key.first(),
          id: key.last(),
          error: val
        })).valueSeq().toJS()
    );

    this.nodeInFocus$ = this.ngRedux.select(focusedCriterion)
      .do(node => this.settingAttributes = !node.isEmpty());
  }

  search(value: string) {
    // TODO(jms) - profile this; it seems likely that we may want to
    //  (A) debounce the keystrokes
    //  (B) use an LRU on the search results
    //  (C) cancel outdated requests
    //  (D) do something smarter than recreating the Observable (twice) with
    //      every keystroke
    this.searchValue = value;
    if (value.length >= 3) {
      this.loading$ = this.loading$.merge(Observable.of(true));
      this.api.getCriteriaTreeQuickSearch(this.actions.cdrVersionId, this.criteriaType, value)
        .first()
        .subscribe(results => {
          this.searchResults = fromJS(results.items);
          this.loading$ = this.loading$.merge(Observable.of(false));
        });
    }
  }

  get mode() {
    if (this.settingAttributes) {
      return SetAttr;
    } else if (this.searchValue.length >= 3) {
      return Search;
    } else {
      return Tree;
    }
  }

  get rootNode() {
    return Map({type: this.criteriaType, id: this.parentId});
  }
}
