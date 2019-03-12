import {dispatch, NgRedux} from '@angular-redux/store';
import {Injectable} from '@angular/core';


import {
  SearchGroup,
  SearchGroupItem,
  SearchParameter,
  SearchRequest,
  TreeSubType,
  TreeType
} from 'generated';

import {stripHtml} from 'app/cohort-search/utils';
import {fromJS, isImmutable, List, Map, Set} from 'immutable';

import {
  activeGroupId,
  activeItem,
  activeParameterList,
  activeRole,
  CohortSearchState,
  getGroup,
  getItem,
  getSearchRequest,
  groupList,
  includeGroups,
  isAttributeLoading,
  isAutocompleteLoading,
  isCriteriaLoading,
  isRequesting,
  SR_ID,
} from 'app/cohort-search/redux/store';
import * as ActionFuncs from './creators';

@Injectable()
export class CohortSearchActions {
  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
  ) {}

  /* We instrument the API calls with the CDR version (which is set in the root
    * component of the Search app and does not change)
    */
  cdrVersionId: number;

  /*
   * Auto-dispatched action creators:
   * We wrap the bare action creators in dispatch here so that we
   * (A) provide a unified action dispatching interface to components and
   * (B) can easily perform multi-step, complex actions from this service
   */
  @dispatch() requestCriteria = ActionFuncs.requestCriteria;
  @dispatch() requestCriteriaBySubtype = ActionFuncs.requestCriteriaBySubtype;
  @dispatch() requestAllCriteria = ActionFuncs.requestAllCriteria;
  @dispatch() requestDrugCriteria = ActionFuncs.requestDrugCriteria;
  @dispatch() loadDemoCriteriaRequestResults = ActionFuncs.loadDemoCriteriaRequestResults;
  @dispatch() cancelCriteriaRequest = ActionFuncs.cancelCriteriaRequest;
  @dispatch() setCriteriaSearchTerms = ActionFuncs.setCriteriaSearchTerms;
  @dispatch() requestAutocompleteOptions = ActionFuncs.requestAutocompleteOptions;
  @dispatch() cancelAutocompleteRequest = ActionFuncs.cancelAutocompleteRequest;
  @dispatch() requestIngredientsForBrand = ActionFuncs.requestIngredientsForBrand;
  @dispatch() requestAllChildren = ActionFuncs.requestAllChildren;
  @dispatch() selectChildren = ActionFuncs.selectChildren;
  @dispatch() loadCriteriaSubtree = ActionFuncs.loadCriteriaSubtree;
  @dispatch() changeCodeOption = ActionFuncs.changeCodeOption;
  @dispatch() setScrollId = ActionFuncs.setScrollId;

  @dispatch() requestCounts = ActionFuncs.requestCounts;
  @dispatch() _requestAttributePreview = ActionFuncs.requestAttributePreview;
  @dispatch() cancelCountRequest = ActionFuncs.cancelCountRequest;
  @dispatch() setCount = ActionFuncs.loadCountRequestResults;
  @dispatch() clearTotalCount = ActionFuncs.clearTotalCount;
  @dispatch() clearGroupCount = ActionFuncs.clearGroupCount;

  @dispatch() _requestPreview = ActionFuncs.requestPreview;

  @dispatch() requestCharts = ActionFuncs.requestCharts;
  @dispatch() cancelChartsRequest = ActionFuncs.cancelChartsRequest;
  @dispatch() setChartData = ActionFuncs.loadChartsRequestResults;

  @dispatch() initGroup = ActionFuncs.initGroup;
  @dispatch() addParameter = ActionFuncs.addParameter;
  @dispatch() removeParameter = ActionFuncs.removeParameter;
  @dispatch() addModifier = ActionFuncs.addModifier;
  @dispatch() removeModifier = ActionFuncs.removeModifier;
  @dispatch() setWizardFocus = ActionFuncs.setWizardFocus;
  @dispatch() clearWizardFocus = ActionFuncs.clearWizardFocus;
  @dispatch() hideGroup = ActionFuncs.hideGroup;
  @dispatch() hideGroupItem = ActionFuncs.hideGroupItem;
  @dispatch() enableEntity = ActionFuncs.enableEntity;
  @dispatch() _removeGroup = ActionFuncs.removeGroup;
  @dispatch() _removeGroupItem = ActionFuncs.removeGroupItem;
  @dispatch() setTimeoutId = ActionFuncs.setTimeoutId;
  @dispatch() requestAttributes = ActionFuncs.requestAttributes;
  @dispatch() loadAttributes = ActionFuncs.loadAttributes;
  @dispatch() hideAttributesPage = ActionFuncs.hideAttributesPage;

  @dispatch() openWizard = ActionFuncs.openWizard;
  @dispatch() reOpenWizard = ActionFuncs.reOpenWizard;
  @dispatch() _finishWizard = ActionFuncs.finishWizard;
  @dispatch() _cancelWizard = ActionFuncs.cancelWizard;
  @dispatch() setWizardContext = ActionFuncs.setWizardContext;
  @dispatch() _updatedTemporal = ActionFuncs.updatedTemporal;
  @dispatch() _updateWhichMention = ActionFuncs.updateWhichMention;
  @dispatch() _updateTemporalTime = ActionFuncs.updateTemporalTime;
  @dispatch() _updateTemporalTimeValue = ActionFuncs.updateTemporalTimeValue;

  @dispatch() loadEntities = ActionFuncs.loadEntities;
  @dispatch() _resetStore = ActionFuncs.resetStore;
  @dispatch() clearStore = ActionFuncs.clearStore;

  /** Internal tooling */
  idsInUse = Set<string>();

  generateId(prefix?: string): string {
    prefix = prefix || 'id';
    let newId = `${prefix}_${this.genSuffix()}`;
    while (this.idsInUse.has(newId)) {
      newId = `${prefix}_${this.genSuffix()}`;
    }
    this.addId(newId);
    return newId;
  }

  getGroupItem(groupId, role) {
    const group = getGroup(groupId)(this.state);
    const itemId = group.get('id');
    const temporal = group.get('temporal');
    const groupItems = group
      .get('items', List())
      .map(id => getItem(id)(this.state))
      .filterNot(it => it.get('status') === 'deleted');
    const [temporalGroupItems, nonTemporalGroupItems] =
      this.getActiveTemporalGroups(groupItems, itemId);
    if (temporal) {
      if (temporalGroupItems && nonTemporalGroupItems) {
        this.requestGroupCount(role, groupId);
        this.requestTotalCount(groupId);
      } else {
        this.clearGroupCount(groupId);
        this.clearTotalCount(groupId);
        this.cancelTotalIfRequesting();
      }
    } else {
      this.requestGroupCount(role, groupId);
      this.requestTotalCount(groupId);
    }
  }

  updateTemporal(flag: boolean, groupId: string, role: keyof SearchRequest) {
    this._updatedTemporal(flag, groupId);
    this.getGroupItem(groupId, role);
  }

  updateWhichMention(mention: any, groupId: string, role: keyof SearchRequest) {
    this._updateWhichMention(mention, groupId);
    this.getGroupItem(groupId, role);
  }

  updateTemporalTime(time: any, groupId: string, role: keyof SearchRequest) {
    this._updateTemporalTime(time, groupId);
    this.getGroupItem(groupId, role);
  }

  updateTemporalTimeValue(timeValue: any, groupId: string, role: keyof SearchRequest) {
    this._updateTemporalTimeValue(timeValue, groupId);
    this.getGroupItem(groupId, role);
  }

  genSuffix(): string {
    return Math.random().toString(36).substr(2, 9);
  }

  removeId(id: string): void {
    this.idsInUse = this.idsInUse.delete(id);
  }

  addId(newId: string): void {
    this.idsInUse = this.idsInUse.add(newId);
  }

  hasActiveItems(group: any) {
    return !group
      .get('items', List())
      .map(id => getItem(id)(this.state))
      .filter(it => it.get('status') === 'active')
      .isEmpty();
  }

  otherGroupsWithActiveItems(ingoreGroupId: string) {
    return !groupList('includes')(this.state)
      .merge(groupList('excludes')(this.state))
      .filter(
        grp => {
          const temporal = grp.get('temporal');
          if (temporal) {
            const groupItems = grp
              .get('items', List())
              .map(id => getItem(id)(this.state))
              .filterNot(it => it.get('status') === 'deleted');
            const  [temporalGroupItems, nonTemporalGroupItems] =
              this.getActiveTemporalGroups(groupItems);
            return grp.get('status') === 'active'
              && grp.get('id') !== ingoreGroupId
              && (temporalGroupItems && nonTemporalGroupItems);
          } else {
            return grp.get('status') === 'active'
              && grp.get('id') !== ingoreGroupId
              && this.hasActiveItems(grp);
          }
        }
      ).isEmpty();
  }

  get state() {
    return this.ngRedux.getState();
  }

  /* Higher order actions - actions composed of other actions or providing
   * alternate interfaces for a simpler action.
   */
  finishWizard(): void {
    const role = activeRole(this.state);
    const groupId = activeGroupId(this.state);
    const itemId = activeItem(this.state).get('id');
    const selections = activeParameterList(this.state);
    this._finishWizard();

    if (!selections.isEmpty()) {
      this.requestItemCount(role, itemId);
      const group = getGroup(groupId)(this.state);
      if (group.get('temporal')) {
        const groupItems = group
          .get('items', List())
          .map(id => getItem(id)(this.state))
          .filterNot(it => it.get('status') === 'deleted');
        const [temporalGroupItems, nonTemporalGroupItems] =
          this.getActiveTemporalGroups(groupItems);
        if (temporalGroupItems && nonTemporalGroupItems) {
          this.requestGroupCount(role, groupId);
          this.requestTotalCount(groupId);
        }
      } else {
        this.requestGroupCount(role, groupId);
        this.requestTotalCount(groupId);
      }
    }
  }

  cancelWizard(kind: string, parentId: number): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    if (isLoading) {
      this.cancelCriteriaRequest(kind, parentId);
    }
    const autocompleteLoading = isAutocompleteLoading()(this.state);
    if (autocompleteLoading) {
      this.cancelAutocompleteRequest();
    }
    this._cancelWizard();
  }

  cancelIfRequesting(kind, id): void {
    if (isRequesting(kind, id)(this.state)) {
      this.cancelCountRequest(kind, id);
    }
  }

  cancelTotalIfRequesting(): void {
    const searchRequest = getSearchRequest(SR_ID)(this.state);
    if (searchRequest.get('isRequesting', false)) {
      this.cancelChartsRequest('searchRequests', SR_ID);
    }
  }

  removeGroup(role: keyof SearchRequest, groupId: string, status?: string): void {
    const group = getGroup(groupId)(this.state);
    this.cancelIfRequesting('groups', groupId);
    if (!status) {
      this._removeGroup(role, groupId);
      this.removeId(groupId);
      group.get('items', List()).forEach(itemId => {
        this.cancelIfRequesting('items', itemId);
        this._removeGroupItem(groupId, itemId);
        this.removeId(itemId);
      });
    } else {
      this.hideGroup(groupId, status);
      if (this.hasActiveItems(group)) {
        if (this.otherGroupsWithActiveItems(groupId)) {
          this.requestTotalCount();
        } else {
          this.cancelTotalIfRequesting();
          this.clearTotalCount();
        }
      }
    }
  }

  getActiveTemporalGroups(groupItems, itemId?) {
    const temporalGroupItems = !groupItems
        .filter(it => it.get('id') !== itemId && it.get('status') === 'active'
          && it.get('temporalGroup') === 1)
        .isEmpty();
    const  nonTemporalGroupItems = !groupItems
        .filter(it => it.get('id') !== itemId && it.get('status') === 'active'
          && it.get('temporalGroup') === 0)
        .isEmpty();
    return [temporalGroupItems, nonTemporalGroupItems];
  }


  removeGroupItem(
    role: keyof SearchRequest,
    groupId: string,
    itemId: string,
    status?: string
  ): void {
    const group = getGroup(groupId)(this.state);
    const temporal = group.get('temporal');
    const groupItems = group
      .get('items', List())
      .map(id => getItem(id)(this.state))
      .filterNot(it => it.get('status') === 'deleted');
    let temporalGroupItems;
    let nonTemporalGroupItems;
    let isOnlyActiveChild;
    if (temporal) {
      [temporalGroupItems, nonTemporalGroupItems] =
        this.getActiveTemporalGroups(groupItems, itemId);
    } else {
      isOnlyActiveChild = groupItems
      .filter(it => it.get('id') !== itemId && it.get('status') === 'active')
        .isEmpty();
    }
    if (!status) {
      this._removeGroupItem(groupId, itemId);
      this.removeId(itemId);
    } else {
      const item = getItem(itemId)(this.state);
      const hasItems = !item.get('searchParameters', List()).isEmpty();
      const countIsNonZero = item.get('count') !== 0;
      this.cancelIfRequesting('items', itemId);
      this.cancelIfRequesting('groups', groupId);
      this.hideGroupItem(groupId, itemId, status);
      const onlyChild = (!temporal && isOnlyActiveChild) ||
        (temporal && (!temporalGroupItems || !nonTemporalGroupItems));
      if (hasItems && (countIsNonZero || onlyChild)) {
        if (onlyChild) {
          if (groupItems.size === 1 && status === 'pending') {
            this.clearGroupCount(groupId);
          }
          if (this.otherGroupsWithActiveItems(groupId)) {
            if (!temporal || group.get('count') !== null) {
              this.requestTotalCount(groupId);
            }
          } else {
            this.cancelTotalIfRequesting();
            this.clearTotalCount(groupId);
          }
        } else {
          this.requestTotalCount();
          this.requestGroupCount(role, groupId);
        }
      }
    }
  }

  enableGroup(group: any) {
    const groupId = group.get('id');
    const temporal = group.get('temporal');
    const groupItems = group
      .get('items', List())
      .map(id => getItem(id)(this.state))
      .filterNot(it => it.get('status') === 'deleted');
    const [temporalGroupItems, nonTemporalGroupItems] = this.getActiveTemporalGroups(groupItems);
    this.enableEntity('groups', groupId);
    if (temporal) {
      if ((temporalGroupItems && nonTemporalGroupItems) ||
        this.otherGroupsWithActiveItems(groupId)) {
        this.requestTotalCount();
      }
    } else if (this.hasActiveItems(group) || this.otherGroupsWithActiveItems(groupId)) {
      this.requestTotalCount();
    }
  }

  enableGroupItem(role: keyof SearchRequest, groupId: string, itemId: string) {
    const group = getGroup(groupId)(this.state);
    const temporal = group.get('temporal');

    if (temporal) {
      const groupItems = group
        .get('items', List())
        .map(id => getItem(id)(this.state))
        .filterNot(it => it.get('status') === 'deleted');
      const item = getItem(itemId)(this.state);
      const temporalGroupItems = !groupItems
          .filter(it => it.get('id') !== itemId && it.get('status') === 'active'
            && it.get('temporalGroup') !== item.get('temporalGroup'))
          .isEmpty();
      this.enableEntity('items', itemId);
      if (temporalGroupItems) {
        this.requestGroupCount(role, groupId);
        this.requestTotalCount();
      }
    } else {
      this.enableEntity('items', itemId);
      this.requestGroupCount(role, groupId);
      this.requestTotalCount();
    }


  }

  fetchCriteria(kind: string, parentId: number): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    const isLoaded = this.state.getIn(['criteria', 'tree', kind, parentId]);
    if (isLoaded || isLoading) {
      return;
    }
    this.requestCriteria(this.cdrVersionId, kind, parentId);
  }

  fetchCriteriaBySubtype(kind: string, subtype: string, parentId: number): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    const isLoaded = this.state.getIn(['criteria', 'tree', kind, subtype, parentId]);
    if (isLoaded || isLoading) {
      return;
    }
    this.requestCriteriaBySubtype(this.cdrVersionId, kind, subtype, parentId);
  }

  fetchAllCriteria(kind: string, parentId: number): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    const isLoaded = this.state.getIn(['criteria', 'tree', kind, parentId]);
    if (isLoaded || isLoading) {
      return;
    }
    this.requestAllCriteria(this.cdrVersionId, kind, parentId);
  }

  fetchDrugCriteria(kind: string, parentId: number, subtype: string): void {
    const isLoading = isCriteriaLoading(kind, parentId)(this.state);
    const isLoaded = this.state.getIn(['criteria', 'tree', kind, parentId]);
    if (isLoaded || isLoading) {
      return;
    }
    this.requestDrugCriteria(this.cdrVersionId, kind, parentId, subtype);
  }

  fetchAutocompleteOptions(kind: string, subtype: string, terms: string): void {
    const isLoading = isAutocompleteLoading()(this.state);
    if (isLoading) {
      this.cancelAutocompleteRequest();
    }
    this.requestAutocompleteOptions(this.cdrVersionId, kind, subtype, terms);
  }

  fetchIngredientsForBrand(conceptId: number): void {
    const isLoading = isAutocompleteLoading()(this.state);
    if (isLoading) {
      return;
    }
    this.requestIngredientsForBrand(this.cdrVersionId, conceptId);
  }

  fetchAllChildren(node: any): void {
    const kind = node.get('type');
    const id = node.get('id');
    const paramId = `param${node.get('conceptId')
      ? (node.get('conceptId') + node.get('code')) : id}`;
    const param = node.set('parameterId', paramId);
    this.addParameter(param);
    this.selectChildren(kind, id);
  }

  fetchAttributes(node: any): void {
    const isLoading = isAttributeLoading(this.state);
    if (isLoading) {
      return;
    }
    this.requestAttributes(this.cdrVersionId, node);
  }

  requestPreview(): void {
    const params = activeParameterList(this.state)
      .valueSeq()
      .map(this.mapParameter)
      .toJS();
    const item = activeItem(this.state);
    this._requestPreview(this.cdrVersionId, <SearchRequest>{
      includes: [],
      excludes: [],
      [activeRole(this.state)]: [{
        items: [<SearchGroupItem>{
          id: item.get('paramId'),
          type: item.get('type', '').toUpperCase(),
          searchParameters: params,
          modifiers: item.get('modifiers', List()).toJS(),
        }]
      }]
    });
  }

  requestAttributePreview(param: any): void {
    const role = activeRole(this.state);
    const itemId = activeItem(this.state).get('id');
    const searchParam = [this.mapParameter(param)];
    const groupItem = <SearchGroupItem>{
      id: itemId,
      type: searchParam[0].type,
      searchParameters: searchParam,
      modifiers: [],
    };
    const request = <SearchRequest>{
      includes: [],
      excludes: [],
      [role]: [{
        items: [groupItem],
      }]
    };
    this._requestAttributePreview(this.cdrVersionId, request);
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
    this.requestCounts(this.cdrVersionId, 'items', itemId, request);
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
    this.requestCounts(this.cdrVersionId, 'groups', groupId, request);
  }

  /**
   * @param outdatedGroup: string
   */
  requestTotalCount(outdatedGroupId?: string): void {
    // const [temporalGroupItems, nonTemporalGroupItems] = this.getActiveTemporalGroups(groupItems);
    this.cancelTotalIfRequesting();
    const included = includeGroups(this.state);

    /* If there are no members of an intersection, the intersection is the null
     * set
     */
    const noActiveGroups = included.filter(group => group.get('status') === 'active').size === 0;
    const noGroupsWithActiveItems = included.every(group => {
      return group.get('items')
        .filter(itemId => (getItem(itemId)(this.state)).get('status') === 'active')
        .size === 0;
    });

    if (noActiveGroups || noGroupsWithActiveItems) {
      this.clearTotalCount();
      return;
    }

    /* If any member of an intersection is the null set, the intersection is
     * the null set - unless the member in question is itself outdated (i.e.
     * the group with a zero count is itself being updated concurrently with
     * the totals, so the ocunt may not actually be zero anymore)
     */
    const nullIntersection = included
      .filterNot(group => group.get('id') === outdatedGroupId)
      .some(group => group.get('count') === 0);

    /* In either case the total count is provably zero without needing to ask
     * the API
     */
    if (nullIntersection) {
      this.setChartData('searchRequests', SR_ID, []);
      return;
    }

    const request = this.mapAll();
    this.requestCharts(this.cdrVersionId, 'searchRequests', SR_ID, request);
  }

  /*
   * Iterates through the full state object, re-running all count / chart
   * requests
   */
  runAllRequests() {
    const doRequests = (kind) => {
      const groups = groupList(kind)(this.state);
      groups.forEach(group => {
        group.get('items', List()).forEach(itemId => {
          this.requestItemCount(kind, itemId);
        });
        this.requestGroupCount(kind, group.get('id'));
      });
    };
    doRequests('includes');
    doRequests('excludes');

    /* Since everything is being run again, the optimizations in
     * `this.requestTotalCount` are sure to be off.  Basically ALL the groups
     * are outdated at the time this runs */
    const request = this.mapAll();
    this.requestCharts(this.cdrVersionId, 'searchRequests', SR_ID, request);
  }

  mapAll = (): SearchRequest => {
    const getGroups = kind =>
      (getSearchRequest(SR_ID)(this.state))
        .get(kind, List())
        .map(this.mapGroup)
        // By this point, unlike almost everywhere else, we're back in vanilla JS land
        .filterNot(grp => !grp || grp.items.length === 0)
        .toJS();

    const includes = getGroups('includes');
    const excludes = getGroups('excludes');

    return <SearchRequest>{includes, excludes};
  }

  mapGroup = (groupId: string): SearchGroup => {
    const group = getGroup(groupId)(this.state);
    if (group.get('status') !== 'active') {
      return;
    }
    const temporal = group.get('temporal');
    let items = group.get('items', List())
      .map(item => this.mapGroupItem(item, temporal))
      .filter(item => !!item);
    if (isImmutable(items)) {
      items = items.toJS();
    }
    const searchGroup = <SearchGroup>{id: groupId, items, temporal};
    if (temporal) {
      const groupItems = group
        .get('items', List())
        .map(id => getItem(id)(this.state))
        .filterNot(it => it.get('status') === 'deleted');
      const [temporalGroupItems, nonTemporalGroupItems] = this.getActiveTemporalGroups(groupItems);
      if (!temporalGroupItems || !nonTemporalGroupItems) {
        return;
      }
      searchGroup.mention = group.get('mention');
      searchGroup.time = group.get('time');
      searchGroup.timeValue = group.get('timeValue');
      searchGroup.timeFrame = group.get('timeFrame');
    }
    return searchGroup;
  }

  mapGroupItem = (itemId: string, temporal?: boolean): SearchGroupItem => {
    const item = getItem(itemId)(this.state);
    if (item.get('status') !== 'active') {
      return;
    }
    const critIds = item.get('searchParameters', List());

    const params = this.state
      .getIn(['entities', 'parameters'], Map())
      .filter((_, key) => critIds.includes(key))
      .valueSeq()
      .map(this.mapParameter)
      .toJS();

    const searchGroupItem = <SearchGroupItem>{
      id: itemId,
      type: item.get('type', '').toUpperCase(),
      searchParameters: params,
      modifiers: item.get('modifiers', List()).toJS(),
    };
    if (temporal) {
      searchGroupItem.temporalGroup = item.get('temporalGroup');
    }
    return searchGroupItem;
  }

  mapParameter = (immParam): SearchParameter => {
    const param = <SearchParameter>{
      parameterId: immParam.get('parameterId'),
      name: stripHtml(immParam.get('name', '')),
      type: immParam.get('type', ''),
      subtype: immParam.get('subtype', ''),
      group: immParam.get('group'),
      attributes: immParam.get('attributes'),
    };

    if (immParam.get('conceptId')) {
      param.conceptId = immParam.get('conceptId');
    }
    if (TreeSubType[TreeSubType.DEC] === immParam.get('subtype')) {
      param.value = immParam.get('name');
    } else if (immParam.get('code') &&
      (TreeType.ICD9 === immParam.get('type') ||
      TreeType.ICD10 === immParam.get('type'))) {
      param.value = immParam.get('code');
    }
    return param;
  }


  /*
   * Deserializes a JSONified SearchRequest into an entities object
   */
  deserializeEntities(jsonStore: string): Map<any, any> {
    const data = JSON.parse(jsonStore);
    const entities = {
      searchRequests: {
        [SR_ID]: {
          includes: data.includes.map(g => g.id),
          excludes: data.excludes.map(g => g.id),
        }
      },
      groups: {},
      items: {},
      parameters: {},
    };

    for (const role of ['includes', 'excludes']) {
      entities.searchRequests[SR_ID][role] = data[role].map(group => {
        group.items = group.items.map(item => {
          item.searchParameters = item.searchParameters.map(param => {
            param.code = param.value;
            if (param.attributes) {
              param.hasAttributes = param.attributes.length > 0;
            }
            entities.parameters[param.parameterId] = param;
            this.addId(param.parameterId);
            return param.parameterId;
          });
          if (!group.temporal) {
            item.temporalGroup = 0;
          }
          item.count = 0;
          item.isRequesting = false;
          item.status = 'active';
          entities.items[item.id] = item;
          this.addId(item.id);
          return item.id;
        });

        group.mention = group.mention ? group.mention : '';
        group.time = group.time ? group.time : '';
        group.timeValue = group.timeValue ? group.timeValue : 0;
        group.timeFrame = group.timeFrame ? group.timeFrame : '';
        group.count = 0;
        group.isRequesting = false;
        group.status = 'active';
        entities.groups[group.id] = group;
        this.addId(group.id);
        return group.id;
      });
    }
    return fromJS(entities);
  }

  /*
   * Reset the ID cache, then
   * Loads a JSONified SearchRequest into the store
   */
  loadFromJSON(json: string): void {
    this.idsInUse = Set<string>();
    const entities = this.deserializeEntities(json);
    this.loadEntities(entities);
  }

  /*
   * Reset Store: reset the store to the initial state and wipe all cached ID's
   */
  resetStore(): void {
    this.idsInUse = Set<string>();
    this._resetStore();
  }
}
