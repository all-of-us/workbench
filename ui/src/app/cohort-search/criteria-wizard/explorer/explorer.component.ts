import {Component, OnInit, Input} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Map, fromJS} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  criteriaLoadErrors,
  focusedCriterion,
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
    const _id = <number>this.rootNode.get('id');

    // TODO(jms) - loading now needs to listen for either the roots loading or
    // the quicksearch results loading as appropriate
    this.loading$ = this.ngRedux.select(isCriteriaLoading(_type, _id));
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
    this.searchValue = value;
    if (value.length >= 3) {
      this.api.getCriteriaTreeQuickSearch(this.criteriaType, value)
        .first()
        .subscribe(results => this.searchResults = fromJS(results.items));
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
