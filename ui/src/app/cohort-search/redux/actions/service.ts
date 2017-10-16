import {Injectable} from '@angular/core';
import {NgRedux, dispatch} from '@angular-redux/store';
import {AnyAction} from 'redux';
import {List, Map, isCollection} from 'immutable';

import {environment} from 'environments/environment';

import {
  CohortSearchState,
  activeSearchGroupPath,
  activeSearchGroupItemPath,
  criteriaPath,
  pathTo,
  isRequesting,
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
  @dispatch() requestCriteria = ActionFuncs.requestCriteria;
  @dispatch() cancelCriteriaRequest = ActionFuncs.cancelCriteriaRequest;

  @dispatch() requestCounts = ActionFuncs.requestCounts;
  @dispatch() cancelCountRequest = ActionFuncs.cancelCountRequest;

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

  removeGroup(role: keyof SearchRequest, groupIndex: number): void {
    const groupPath = pathTo(role, groupIndex);
    const state = this.ngRedux.getState();

    /* Cancel the Group Request Itself */
    if (isRequesting('group', groupPath)(state)) {
      this.cancelCountRequest('group', groupPath);
    }

    /* Cancel any item requests that may be out there */
    state.getIn(groupPath.push('items')).forEach((item, key) => {
      const itemPath = groupPath.push('items', key);
      if (isRequesting('item', itemPath)(state)) {
        this.cancelCountRequest('item', itemPath);
      }
    });

    /* Refires the Totals request without the group */
    this.remove(groupPath);
    this.requestTotalCount();
  }

  removeGroupItem(role: keyof SearchRequest, groupIndex: number, groupItemIndex: number): void {
    const itemPath = pathTo(role, groupIndex, groupItemIndex);
    const state = this.ngRedux.getState();

    /* Cancel the item request itself */
    if (isRequesting('item', itemPath)(state)) {
      this.cancelCountRequest('item', itemPath);
    }

    /* Refire the group and total requests now without the item */
    this.remove(itemPath);
    this.requestGroupCount(pathTo(role, groupIndex));
    this.requestTotalCount();
  }

  removeCriterion(
    role: keyof SearchRequest,
    index: number,
    itemIndex: number,
    criterionIndex: number
  ): void {
    this.remove(pathTo(role, index, itemIndex, criterionIndex));
  }

  openWizard(criteriaType: string, role: keyof SearchRequest, groupIndex: number): void {
    this.setActiveContext({criteriaType, role, groupIndex});
    this.setWizardOpen();
    this.initGroupItem(role, groupIndex);
  }

  finishWizard(): void {
    const state = this.ngRedux.getState();
    const itemPath = activeSearchGroupItemPath(state);
    const groupPath = activeSearchGroupPath(state);

    this.requestItemCount(itemPath);
    this.requestGroupCount(groupPath);
    this.requestTotalCount();

    this.clearActiveContext();
    this.setWizardClosed();
  }

  cancelWizard(): void {
    this.setWizardClosed();
    this.remove(activeSearchGroupItemPath(this.ngRedux.getState()));
    this.clearActiveContext();
  }

  fetchCriteria(kind: string, parentId: number): void {
    /* Don't reload already loaded criteria subtrees */
    const path = criteriaPath(kind, parentId);
    const state = this.ngRedux.getState();
    if (state.getIn(path)) {
      return;
    }
    this.requestCriteria(path);
  }

  requestItemCount(itemPath): void {
    const state = this.ngRedux.getState();
    if (isRequesting('item', itemPath)(state)) {
      this.cancelCountRequest('item', itemPath);
    }
    const role = itemPath.get(1);
    const item = state.getIn(itemPath);
    const searchRequest = Map({
        includes: List(),
        excludes: List(),
      })
      .update(role, groups => groups.push(
        Map({ items: List([mapGroupItem(item)]) })
      ));
    this.requestCounts('item', <SearchRequest>searchRequest.toJS(), itemPath);
  }

  requestGroupCount(groupPath): void {
    const state = this.ngRedux.getState();
    if (isRequesting('group', groupPath)(state)) {
      this.cancelCountRequest('group', groupPath);
    }
    const role = groupPath.get(1);
    const group = state.getIn(groupPath);
    const searchRequest = Map({
        includes: List(),
        excludes: List(),
      })
      .update(role, groups => groups.push(
        mapGroup(group)
      ));
    this.requestCounts('group', <SearchRequest>searchRequest.toJS(), groupPath);
  }

  requestTotalCount(): void {
    const state = this.ngRedux.getState();
    if (isRequesting('total')(state)) {
      this.cancelCountRequest('total');
    }
    const searchRequest = mapAll(state);
    this.requestCounts('total', <SearchRequest>searchRequest.toJS());
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
