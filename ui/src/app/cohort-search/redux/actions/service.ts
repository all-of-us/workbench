import {Injectable} from '@angular/core';
import {NgRedux, dispatch} from '@angular-redux/store';
import {AnyAction} from 'redux';
import {List, Map, Set, isCollection, isImmutable} from 'immutable';

import {environment} from 'environments/environment';

import {
  CohortSearchState,
  activeItemId,
  activeGroupId,
  activeRole,
  isCriteriaLoading,
  getItem,
  getGroup,
  getSearchRequest,
  SR_ID,
} from '../store';
import * as ActionFuncs from './creators';

import {
  Criteria,
  CohortBuilderService,
  SearchRequest,
  SearchParameter,
  SearchGroup,
  SearchGroupItem,
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

  @dispatch() _initGroup = ActionFuncs.initGroup;
  @dispatch() _initGroupItem = ActionFuncs.initGroupItem;
  @dispatch() selectCriteria = ActionFuncs.selectCriteria;
  @dispatch() _removeGroup = ActionFuncs.removeGroup;
  @dispatch() _removeGroupItem = ActionFuncs.removeGroupItem;
  @dispatch() _removeCriterion = ActionFuncs.removeCriterion;

  @dispatch() setWizardOpen = ActionFuncs.setWizardOpen;
  @dispatch() setWizardClosed = ActionFuncs.setWizardClosed;
  @dispatch() setActiveContext = ActionFuncs.setActiveContext;
  @dispatch() clearActiveContext = ActionFuncs.clearActiveContext;

  /** Internal tooling */
  _idsInUse = Set<string>();

  _generateId(prefix?: string) {
    prefix = prefix || 'id';
    let newId = `${prefix}${this._genSuffix()}`;
    while (this._idsInUse.has(newId)) {
      newId = `${prefix}${this._genSuffix()}`;
    }
    this._idsInUse = this._idsInUse.add(newId);
    return newId;
  }

  _genSuffix() {
    return Math.random().toString(36).substr(2, 9);
  }

  _removeId(id: string) {
    this._idsInUse = this._idsInUse.delete(id);
  }

  get state() {
    return this.ngRedux.getState();
  }

  /* Higher order actions - actions composed of other actions or providing
   * alternate interfaces for a simpler action.
   */
  initGroup(role: keyof SearchRequest) {
    const newId = this._generateId(role);
    this._initGroup(role, newId);
  }

  initGroupItem(groupId: string) {
    const itemId = this._generateId('item');
    this._initGroupItem(itemId, groupId);
    this.setActiveContext({itemId});
  }

  removeGroup(role: keyof SearchRequest, groupId: string): void {
    console.log(`Removing Group ${groupId} in ${role}`);
    const group = getGroup(groupId)(this.state);

    /* Cancel the Group Request Itself */
    if (group.get('isRequesting', false)) {
      this.cancelCountRequest('group', groupId);
    }

    /* If there are any child items, cancel their requests and delete them */
    group.get('items', List()).forEach(itemId => {
      this.removeGroupItem(role, groupId, itemId);
    });

    /* Refires the Totals request without the group */
    this._removeGroup(role, groupId);
    this._removeId(groupId);
    this.requestTotalCount();
  }

  removeGroupItem(role: keyof SearchRequest, groupId: string, itemId: string): void {
    const item = getItem(itemId)(this.state);
    /* Cancel the item request itself */
    if (item.get('isRequesting', false)) {
      this.cancelCountRequest('item', itemId);
    }

    /* Remove any criterion associated with the item */
    item.get('searchParameters', List()).forEach(id => {
      this._removeCriterion(itemId, id);
    });

    /* Refire the group and total requests now without the item */
    this._removeGroupItem(groupId, itemId);
    this._removeId(itemId);
    this.requestGroupCount(role, groupId);
    this.requestTotalCount();
  }

  removeCriterion(
    role: keyof SearchRequest, groupId: string, itemId: string, criterionId: number
  ): void {
    /* If we're not in a wizard context, then we need to update all the counts.
     * Otherwise, finishing the wizard will effectively do that for us
     */
    if (!this.state.getIn(['context', 'wizardOpen'], false)) {
      this.requestGroupCount(role, groupId);
      this.requestItemCount(role, itemId);
      this.requestTotalCount();
    }
    this._removeCriterion(itemId, criterionId);
  }

  openWizard(criteriaType: string, role: keyof SearchRequest, groupId: string): void {
    this.setActiveContext({criteriaType, role, groupId});
    this.setWizardOpen();
    this.initGroupItem(groupId);
  }

  finishWizard(): void {
    this.requestItemCount(activeRole(this.state), activeItemId(this.state));
    this.requestGroupCount(activeRole(this.state), activeGroupId(this.state));
    this.requestTotalCount();

    this.clearActiveContext();
    this.setWizardClosed();
  }

  cancelWizard(): void {
    this.setWizardClosed();
    this.removeGroupItem(
      activeRole(this.state),
      activeGroupId(this.state),
      activeItemId(this.state)
    );
    this.clearActiveContext();
  }

  fetchCriteria(kind: string, parentId: number): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    const isLoaded = this.state.getIn(['criteria', kind, parentId]);
    if (isLoaded || isLoading) {
      return;
    }
    this.requestCriteria(kind, parentId);
  }

  requestItemCount(role: keyof SearchRequest, itemId: string): void {
    const item = getItem(itemId)(this.state);
    if (item.get('isRequesting', false)) {
      this.cancelCountRequest('items', itemId);
    }
    const request = <SearchRequest>{
      includes: [],
      excludes: [],
      [role]: [{
        items: [this.mapGroupItem(itemId)],
      }]
    };
    this.requestCounts('items', itemId, request);
  }

  requestGroupCount(role: keyof SearchRequest, groupId: string): void {
    const group = getGroup(groupId)(this.state);
    if (group.get('isRequesting', false)) {
      this.cancelCountRequest('groups', groupId);
    }
    const request = <SearchRequest>{
      includes: [],
      excludes: [],
      [role]: [this.mapGroup(groupId)]
    };
    this.requestCounts('groups', groupId, request);
  }

  requestTotalCount(): void {
    const searchRequest = getSearchRequest(SR_ID)(this.state);
    if (searchRequest.get('isRequesting', false)) {
      this.cancelCountRequest('searchRequests', SR_ID);
    }
    const request = this.mapAll();
    this.requestCounts('searchRequests', SR_ID, request);
  }

  mapAll = (): SearchRequest => {
    const getGroups = kind =>
      (getSearchRequest(SR_ID)(this.state))
        .get(kind, List())
        .map(this.mapGroup)
        // By this point, unlike almost everywhere else, we're back in vanilla JS land
        .filterNot(grp => grp.items.length === 0)
        .toJS();

    const includes = getGroups('includes');
    const excludes = getGroups('excludes');

    return <SearchRequest>{includes, excludes};
  }

  mapGroup = (groupId: string): SearchGroup => {
    const group = getGroup(groupId)(this.state);
    let items = group.get('items', List()).map(this.mapGroupItem);
    if (isImmutable(items)) {
      items = items.toJS();
    }
    return <SearchGroup>{items};
  }

  mapGroupItem = (itemId: string): SearchGroupItem => {
    const item = getItem(itemId)(this.state);
    const critIds = item.get('searchParameters', List());

    const params = this.state
      .getIn(['entities', 'criteria'], Map())
      .filter((_, key) => critIds.includes(key))
      .valueSeq()
      .map(this.mapParameter)
      .toJS();

    return <SearchGroupItem>{
      type: item.get('type', '').toUpperCase(),
      searchParameters: params,
      modifiers: [],
    };
  }

  mapParameter = (param): SearchParameter => {
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
    } else {
      return;
    }
  }
}
