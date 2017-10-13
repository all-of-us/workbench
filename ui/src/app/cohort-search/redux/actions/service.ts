import {Injectable} from '@angular/core';
import {NgRedux, dispatch} from '@angular-redux/store';
import {AnyAction} from 'redux';
import {List, Map} from 'immutable';

import {environment} from 'environments/environment';

import {
  CohortSearchState,
  activeSearchGroupItemPath,
  criteriaPath,
} from '../store';
import {KeyPath} from './types';
import * as ActionFuncs from './creators';

import {
  Criteria,
  CohortBuilderService,
  SearchRequest,
  SearchParameter,
} from 'generated';


@Injectable()
export class CohortSearchActions {

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private service: CohortBuilderService) {}

  /*
   * Auto-dispatched action creators:
   * We wrap the bare action creators in dispatch here so that we
   * (A) provide a unified action dispatching interface to components and
   * (B) can easily perform multi-step, complex actions from this service
   */
  @dispatch() startRequest = ActionFuncs.startRequest;
  @dispatch() cancelRequest = ActionFuncs.cancelRequest;
  @dispatch() cleanupRequest = ActionFuncs.cleanupRequest;

  @dispatch() requestCriteria = ActionFuncs.requestCriteria;
  @dispatch() loadCriteriaRequestResults = ActionFuncs.loadCriteriaRequestResults;
  @dispatch() requestCounts = ActionFuncs.requestCounts;
  @dispatch() loadCountRequestResults = ActionFuncs.loadCountRequestResults;

  @dispatch() initGroup = ActionFuncs.initGroup;
  @dispatch() initGroupItem = ActionFuncs.initGroupItem;
  @dispatch() remove = ActionFuncs.remove;
  @dispatch() selectCriteria = ActionFuncs.selectCriteria;

  @dispatch() setWizardOpen = ActionFuncs.setWizardOpen;
  @dispatch() setWizardClosed = ActionFuncs.setWizardClosed;
  @dispatch() setActiveContext = ActionFuncs.setActiveContext;
  @dispatch() clearActiveContext = ActionFuncs.clearActiveContext;

  /* Higher order actions - actions composed of other actions or providing
   * alternate interfaces for a simpler action.
   */

  // TODO(jms)  removals affect in-flight count requests: implement that affect
  removeGroup(role: keyof SearchRequest, groupIndex: number): void {
    this.remove(List(['search', role, groupIndex]));
  }

  removeGroupItem(role: keyof SearchRequest, groupIndex: number, groupItemIndex: number): void {
    const path = List().push(role, groupIndex, groupItemIndex);
    const state = this.ngRedux.getState();
    const isloading = state.get('requests').has(path);
    if (isloading) {
      this.cancelRequest(path);
    }
    this.remove(path.unshift('search'));
  }

  removeCriterion(
    role: keyof SearchRequest,
    groupIndex: number,
    groupItemIndex: number,
    criterionIndex: number,
  ): void {
    this.remove(List().push(
      'search',
      role,
      groupIndex,
      'items',
      groupItemIndex,
      'searchParameters',
      criterionIndex
    ));
  }

  openWizard(criteriaType: string, role: keyof SearchRequest, groupIndex: number): void {
    this.setActiveContext({criteriaType, role, groupIndex});
    this.setWizardOpen();
    this.initGroupItem(role, groupIndex);
  }

  finishWizard(): void {
    const path = activeSearchGroupItemPath(this.ngRedux.getState());

    this.getCounts(path, 'ITEM');
    this.getCounts(path, 'GROUP');
    this.getCounts(path, 'TOTAL');

    this.clearActiveContext();
    this.setWizardClosed();
  }

  getCounts = (path: KeyPath, scope: string) => {
    const request = this.prepareSearchRequest(path, scope);
    if (environment.debug) {
      console.log('Created a new SearchRequest');
      console.dir(request);
      console.log(path);
    }
    this.startRequest(path.push(scope));
    this.requestCounts(path.push(scope), request);
  }

  cancelWizard(): void {
    const path = activeSearchGroupItemPath(this.ngRedux.getState());
    this.remove(path);
    this.clearActiveContext();
    this.setWizardClosed();
  }

  fetchCriteria(kind: string, parentId: number): void {
    /* Don't reload already loaded criteria subtrees */
    const path = criteriaPath(kind, parentId);
    const state = this.ngRedux.getState();
    if (state.getIn(path)) { 
      return; 
    }
    this.startRequest(path);
    this.requestCriteria(path);
  }

  prepareSearchRequest(itemPath, scope): SearchRequest {
    // itemPath should be ['search', role, index, 'items', itemIndex];
    const state = this.ngRedux.getState();
    const role = itemPath.get(1);

    let searchRequest = Map({
      includes: List(),
      excludes: List(),
    });

    if (scope === 'TOTAL') {
      searchRequest = mapAll(state);
    } else if (scope === 'GROUP') {
      const group = state.getIn(itemPath.skipLast(2));
      searchRequest = searchRequest
        .update(role, groups => groups.push(
          mapGroup(group)
        ));
    } else if (scope === 'ITEM') {
      const item = state.getIn(itemPath);
      searchRequest = searchRequest
        .update(role, groups => groups.push(
          Map({ items: List([mapGroupItem(item)]) })
        ));
    } else {
      console.log('Unknown scope! (this should be unreachable)');
      return;
    }
    return <SearchRequest>searchRequest.toJS();
  }
}

/*
 * Helper functions for transforming immutable data into JSON and removing
 * unneeded properties
 */
const mapParameter = (param) => {
  const _type = param.get('type');
  if (_type.match(/^DEMO.*/i)) {
    return <SearchParameter>{
      value: param.get('code'),
      domain: param.get('type'),
      conceptId: param.get('conceptId'),
    };
  } else if (_type.match(/^ICD.*/i)) {
    return <SearchParameter>{
      value: param.get('code'),
      domain: param.get('domainId'),
    };
  }
};

const mapGroupItem = (groupItem) =>
  groupItem
    .update('searchParameters', List(), params => params.map(mapParameter))
    .update('type', '', _type => _type.toUpperCase());

const mapGroup = (group) =>
  group.update('items', Map(), items => items.map(mapGroupItem));

const mapGroupList = (grouplist) =>
  grouplist
    .filterNot(group => group.get('items').isEmpty())
    .map(mapGroup);

const mapAll = (state) =>
  state.get('search')
    .update('includes', List(),  mapGroupList)
    .update('excludes', List(), mapGroupList);
