import {Component, OnInit, Input} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Observable} from 'rxjs/Observable';
import {Map} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  isCriteriaLoading,
  criteriaLoadErrors,
  focusedCriterion,
} from '../../redux';

const Tree = 'Tree';
const Search = 'Search';
const SetAttr = 'SetAttr';
type Mode = typeof Tree | typeof Search | typeof SetAttr;

@Component({
  selector: 'crit-explorer',
  templateUrl: './explorer.component.html',
})
export class ExplorerComponent implements OnInit {
  @Input() criteriaType: string;
  private readonly parentId = 0;  /* Root parent ID is always zero */

  private mode: Mode = Tree;

  private loading$: Observable<boolean>;
  private errors$: Observable<any>;
  private nodeInFocus$: Observable<Map<any, any>>;
  private settingAttributes$: Observable<boolean>;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    const _type = <string>this.rootNode.get('type');
    const _id = <number>this.rootNode.get('id');

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
    this.nodeInFocus$ = this.ngRedux.select(focusedCriterion);
    this.settingAttributes$ = this.nodeInFocus$.map(node => !node.isEmpty());
  }

  get rootNode() {
    return Map({type: this.criteriaType, id: this.parentId});
  }
}
