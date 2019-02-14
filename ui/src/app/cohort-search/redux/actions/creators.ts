/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_SUBTYPE_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  BEGIN_DRUG_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  LOAD_CRITERIA_SUBTYPE_RESULTS,
  LOAD_DEMO_CRITERIA_RESULTS,
  LOAD_CRITERIA_SUBTREE,
  CANCEL_CRITERIA_REQUEST,
  SET_CRITERIA_SEARCH,
  BEGIN_AUTOCOMPLETE_REQUEST,
  CANCEL_AUTOCOMPLETE_REQUEST,
  BEGIN_INGREDIENT_REQUEST,
  BEGIN_CHILDREN_REQUEST,
  LOAD_AUTOCOMPLETE_OPTIONS,
  LOAD_INGREDIENT_LIST,
  LOAD_CHILDREN_LIST,
  SELECT_CHILDREN_LIST,
  LOAD_ATTRIBUTE_LIST,
  AUTOCOMPLETE_REQUEST_ERROR,
  ATTRIBUTE_REQUEST_ERROR,
  CRITERIA_REQUEST_ERROR,
  CHANGE_CODE_OPTION,
  SET_SCROLL_ID,

  BEGIN_COUNT_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,
  LOAD_ATTR_PREVIEW_RESULTS,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,
  CLEAR_TOTAL_COUNT,
  CLEAR_GROUP_COUNT,

  BEGIN_PREVIEW_REQUEST,
  LOAD_PREVIEW_RESULTS,
  PREVIEW_REQUEST_ERROR,

  BEGIN_CHARTS_REQUEST,
  LOAD_CHARTS_RESULTS,
  CANCEL_CHARTS_REQUEST,
  CHARTS_REQUEST_ERROR,

  INIT_SEARCH_GROUP,
  ADD_PARAMETER,
  REMOVE_PARAMETER,
  ADD_MODIFIER,
  REMOVE_MODIFIER,
  SET_WIZARD_FOCUS,
  CLEAR_WIZARD_FOCUS,
  HIDE_ITEM,
  HIDE_GROUP,
  ENABLE_ENTITY,
  REMOVE_ITEM,
  REMOVE_GROUP,
  SET_ENTITY_TIMEOUT,
  OPEN_WIZARD,
  REOPEN_WIZARD,
  WIZARD_FINISH,
  UPDATE_TEMPORAL,
  UPDATE_WHICH_MENTION,
  UPDATE_TEMPORAL_TIME,
  UPDATE_TEMPORAL_TIME_VALUE,
  WIZARD_CANCEL,
  SET_WIZARD_CONTEXT,
  SHOW_ATTRIBUTES_PAGE,
  HIDE_ATTRIBUTES_PAGE,

  LOAD_ENTITIES,
  RESET_STORE,
  CLEAR_STORE,
  ActionTypes,
} from './types';
/* tslint:enable:ordered-imports */

import {Criteria, DemoChartInfo, SearchRequest} from 'generated';

/**
 * Criteria loading mgmt
 */
export const requestCriteria =
  (cdrVersionId: number, kind: string, parentId: number
  ): ActionTypes[typeof BEGIN_CRITERIA_REQUEST] =>
  ({type: BEGIN_CRITERIA_REQUEST, cdrVersionId, kind, parentId});

export const requestCriteriaBySubtype =
  (cdrVersionId: number, kind: string, subtype: string, parentId: number
  ): ActionTypes[typeof BEGIN_SUBTYPE_CRITERIA_REQUEST] =>
  ({type: BEGIN_SUBTYPE_CRITERIA_REQUEST, cdrVersionId, kind, subtype, parentId});

export const requestAllCriteria =
  (cdrVersionId: number, kind: string, parentId: number
  ): ActionTypes[typeof BEGIN_ALL_CRITERIA_REQUEST] =>
  ({type: BEGIN_ALL_CRITERIA_REQUEST, cdrVersionId, kind, parentId});

export const requestDrugCriteria =
  (cdrVersionId: number, kind: string, parentId: number, subtype: string
  ): ActionTypes[typeof BEGIN_DRUG_CRITERIA_REQUEST] =>
  ({type: BEGIN_DRUG_CRITERIA_REQUEST, cdrVersionId, kind, parentId, subtype});

export const loadCriteriaRequestResults =
  (kind: string, parentId: number, results: Criteria[]
  ): ActionTypes[typeof LOAD_CRITERIA_RESULTS] =>
  ({type: LOAD_CRITERIA_RESULTS, kind, parentId, results});

export const loadCriteriaSubtypeRequestResults =
  (kind: string, subtype: string, parentId: number, results: Criteria[]
  ): ActionTypes[typeof LOAD_CRITERIA_SUBTYPE_RESULTS] =>
  ({type: LOAD_CRITERIA_SUBTYPE_RESULTS, kind, subtype, parentId, results});

export const loadCriteriaSubtree =
  (kind: string, subtype: string, ids: Array<number>, path: Array<string>
  ): ActionTypes[typeof LOAD_CRITERIA_SUBTREE] =>
  ({type: LOAD_CRITERIA_SUBTREE, kind, subtype, ids, path});

export const loadDemoCriteriaRequestResults =
  (kind: string, subtype: string, results: any
  ): ActionTypes[typeof LOAD_DEMO_CRITERIA_RESULTS] =>
  ({type: LOAD_DEMO_CRITERIA_RESULTS, kind, subtype, results});

export const cancelCriteriaRequest =
  (kind: string, parentId: number
  ): ActionTypes[typeof CANCEL_CRITERIA_REQUEST] =>
  ({type: CANCEL_CRITERIA_REQUEST, kind, parentId});

export const setCriteriaSearchTerms =
  (searchTerms: Array<string>
  ): ActionTypes[typeof SET_CRITERIA_SEARCH] =>
  ({type: SET_CRITERIA_SEARCH, searchTerms});

export const requestAutocompleteOptions =
  (cdrVersionId: number, kind: string, subtype: string, searchTerms: string
  ): ActionTypes[typeof BEGIN_AUTOCOMPLETE_REQUEST] =>
  ({type: BEGIN_AUTOCOMPLETE_REQUEST, cdrVersionId, kind, subtype, searchTerms});

export const cancelAutocompleteRequest =
  (): ActionTypes[typeof CANCEL_AUTOCOMPLETE_REQUEST] =>
  ({type: CANCEL_AUTOCOMPLETE_REQUEST});

export const requestIngredientsForBrand =
  (cdrVersionId: number, conceptId: number
  ): ActionTypes[typeof BEGIN_INGREDIENT_REQUEST] =>
  ({type: BEGIN_INGREDIENT_REQUEST, cdrVersionId, conceptId});

export const requestAllChildren =
  (cdrVersionId: number, kind: string, parentId: number
  ): ActionTypes[typeof BEGIN_CHILDREN_REQUEST] =>
  ({type: BEGIN_CHILDREN_REQUEST, cdrVersionId, kind, parentId});

export const loadAutocompleteOptions =
  (options: any
  ): ActionTypes[typeof LOAD_AUTOCOMPLETE_OPTIONS] =>
  ({type: LOAD_AUTOCOMPLETE_OPTIONS, options});

export const autocompleteRequestError =
  (error?: any
  ): ActionTypes[typeof AUTOCOMPLETE_REQUEST_ERROR] =>
  ({type: AUTOCOMPLETE_REQUEST_ERROR, error});

export const loadIngredients =
  (ingredients: any
  ): ActionTypes[typeof LOAD_INGREDIENT_LIST] =>
  ({type: LOAD_INGREDIENT_LIST, ingredients});

export const loadAndSelectChildren =
  (parentId: number, children: any
  ): ActionTypes[typeof LOAD_CHILDREN_LIST] =>
  ({type: LOAD_CHILDREN_LIST, parentId, children});

export const selectChildren =
  (kind: string, parentId: number
  ): ActionTypes[typeof SELECT_CHILDREN_LIST] =>
  ({type: SELECT_CHILDREN_LIST, kind, parentId});

export const loadAttributes =
  (node: any, attributes: any
  ): ActionTypes[typeof LOAD_ATTRIBUTE_LIST] =>
  ({type: LOAD_ATTRIBUTE_LIST, node, attributes});

export const attributeRequestError =
  (error?: any
  ): ActionTypes[typeof ATTRIBUTE_REQUEST_ERROR] =>
    ({type: ATTRIBUTE_REQUEST_ERROR, error});

export const criteriaRequestError =
  (kind: string, parentId: number, error?: any
  ): ActionTypes[typeof CRITERIA_REQUEST_ERROR] =>
  ({type: CRITERIA_REQUEST_ERROR, kind, parentId, error});

export const setScrollId =
  (nodeId: string
  ): ActionTypes[typeof SET_SCROLL_ID] =>
  ({type: SET_SCROLL_ID, nodeId});

export const changeCodeOption =
  (): ActionTypes[typeof CHANGE_CODE_OPTION] =>
  ({type: CHANGE_CODE_OPTION});


/**
 * Count loading mgmt
 */
export const requestCounts =
  (cdrVersionId: number, entityType: string, entityId: string, request: SearchRequest
  ): ActionTypes[typeof BEGIN_COUNT_REQUEST] =>
  ({type: BEGIN_COUNT_REQUEST, cdrVersionId, entityType, entityId, request});

export const requestAttributePreview =
  (cdrVersionId: number, request: SearchRequest
  ): ActionTypes[typeof BEGIN_ATTR_PREVIEW_REQUEST] =>
  ({type: BEGIN_ATTR_PREVIEW_REQUEST, cdrVersionId, request});

export const loadAttributePreviewRequestResults =
  (count: number): ActionTypes[typeof LOAD_ATTR_PREVIEW_RESULTS] =>
  ({type: LOAD_ATTR_PREVIEW_RESULTS, count});

export const loadCountRequestResults =
  (entityType: string, entityId: string, count: number
  ): ActionTypes[typeof LOAD_COUNT_RESULTS] =>
  ({type: LOAD_COUNT_RESULTS, entityType, entityId, count});

export const cancelCountRequest =
  (entityType: string, entityId: string
  ): ActionTypes[typeof CANCEL_COUNT_REQUEST] =>
  ({type: CANCEL_COUNT_REQUEST, entityType, entityId});

export const countRequestError =
  (entityType: string, entityId: string, error?: any
  ): ActionTypes[typeof COUNT_REQUEST_ERROR] =>
  ({type: COUNT_REQUEST_ERROR, entityType, entityId, error});

export const clearTotalCount =
  (groupId?: string): ActionTypes[typeof CLEAR_TOTAL_COUNT] =>
  ({type: CLEAR_TOTAL_COUNT, groupId});

export const clearGroupCount =
  (groupId: string
  ): ActionTypes[typeof CLEAR_GROUP_COUNT] =>
  ({type: CLEAR_GROUP_COUNT, groupId});

export const requestPreview =
  (cdrVersionId: number, request: SearchRequest
  ): ActionTypes[typeof BEGIN_PREVIEW_REQUEST] =>
  ({type: BEGIN_PREVIEW_REQUEST, cdrVersionId, request});

export const loadPreviewRequestResults =
  (count: number): ActionTypes[typeof LOAD_PREVIEW_RESULTS] =>
  ({type: LOAD_PREVIEW_RESULTS, count});

export const previewRequestError =
  (error?: any): ActionTypes[typeof PREVIEW_REQUEST_ERROR] =>
  ({type: PREVIEW_REQUEST_ERROR, error});

export const requestCharts =
  (cdrVersionId: number, entityType: string, entityId: string, request: SearchRequest
  ): ActionTypes[typeof BEGIN_CHARTS_REQUEST] =>
  ({type: BEGIN_CHARTS_REQUEST, cdrVersionId, entityType, entityId, request});

export const loadChartsRequestResults =
  (entityType: string, entityId: string, chartData: DemoChartInfo[]
  ): ActionTypes[typeof LOAD_CHARTS_RESULTS] =>
  ({type: LOAD_CHARTS_RESULTS, entityType, entityId, chartData});

export const cancelChartsRequest =
  (entityType: string, entityId: string
  ): ActionTypes[typeof CANCEL_CHARTS_REQUEST] =>
  ({type: CANCEL_CHARTS_REQUEST, entityType, entityId});

export const chartsRequestError =
  (entityType: string, entityId: string, error?: any
  ): ActionTypes[typeof CHARTS_REQUEST_ERROR] =>
  ({type: CHARTS_REQUEST_ERROR, entityType, entityId, error});

/**
 * Entity creation & deletion mgmt
 */
export const initGroup =
  (role: keyof SearchRequest, groupId: string
  ): ActionTypes[typeof INIT_SEARCH_GROUP] =>
  ({type: INIT_SEARCH_GROUP, role, groupId});

export const addParameter =
  (parameter: any
  ): ActionTypes[typeof ADD_PARAMETER] =>
  ({type: ADD_PARAMETER, parameter});

export const removeParameter =
  (parameterId: string, path?: string, id?: number
  ): ActionTypes[typeof REMOVE_PARAMETER] =>
  ({type: REMOVE_PARAMETER, parameterId, path, id});

export const addModifier =
  (modifier: any
  ): ActionTypes[typeof ADD_MODIFIER] =>
  ({type: ADD_MODIFIER, modifier});

export const removeModifier =
  (modifier: any
  ): ActionTypes[typeof REMOVE_MODIFIER] =>
  ({type: REMOVE_MODIFIER, modifier});

export const setWizardFocus =
  (criterion: any
  ): ActionTypes[typeof SET_WIZARD_FOCUS] =>
  ({type: SET_WIZARD_FOCUS, criterion});

export const clearWizardFocus =
  (): ActionTypes[typeof CLEAR_WIZARD_FOCUS] =>
  ({type: CLEAR_WIZARD_FOCUS});

export const hideGroup =
  (groupId: string, status: string
  ): ActionTypes[typeof HIDE_GROUP] =>
    ({type: HIDE_GROUP, groupId, status});

export const hideGroupItem =
  (groupId: string, itemId: string, status: string
  ): ActionTypes[typeof HIDE_ITEM] =>
    ({type: HIDE_ITEM, groupId, itemId, status});

export const enableEntity =
  (entity: string, entityId: string,
  ): ActionTypes[typeof ENABLE_ENTITY] =>
    ({type: ENABLE_ENTITY, entity, entityId});

export const removeGroup =
  (role: keyof SearchRequest, groupId: string
  ): ActionTypes[typeof REMOVE_GROUP] =>
  ({type: REMOVE_GROUP, role, groupId});

export const removeGroupItem =
  (groupId: string, itemId: string
  ): ActionTypes[typeof REMOVE_ITEM] =>
  ({type: REMOVE_ITEM, groupId, itemId});

export const setTimeoutId =
  (entity: string, entityId: string, timeoutId: any
  ): ActionTypes[typeof SET_ENTITY_TIMEOUT] =>
  ({type: SET_ENTITY_TIMEOUT, entity, entityId, timeoutId});

export const requestAttributes =
  (cdrVersionId: number, node: any
  ): ActionTypes[typeof SHOW_ATTRIBUTES_PAGE] =>
  ({type: SHOW_ATTRIBUTES_PAGE, cdrVersionId, node});

export const hideAttributesPage =
  (): ActionTypes[typeof HIDE_ATTRIBUTES_PAGE] =>
  ({type: HIDE_ATTRIBUTES_PAGE});

/**
 * Context mgmt
 */
export const openWizard =
  (itemId: string, itemType: string, context: object, tempGroup?: number
  ): ActionTypes[typeof OPEN_WIZARD] =>
  ({type: OPEN_WIZARD, itemId, itemType, context, tempGroup});

export const reOpenWizard =
  (item: any, context: object
  ): ActionTypes[typeof REOPEN_WIZARD] =>
  ({type: REOPEN_WIZARD, item, context});

export const finishWizard =
  (): ActionTypes[typeof WIZARD_FINISH] => ({type: WIZARD_FINISH});

export const cancelWizard =
  (): ActionTypes[typeof WIZARD_CANCEL] => ({type: WIZARD_CANCEL});

export const setWizardContext =
  (context: object): ActionTypes[typeof SET_WIZARD_CONTEXT] =>
  ({type: SET_WIZARD_CONTEXT, context});

export const updatedTemporal =
  (flag: boolean, groupId: any): ActionTypes[typeof UPDATE_TEMPORAL] =>
    ({type: UPDATE_TEMPORAL, flag, groupId});

export const updateWhichMention =
  (mention: any, groupId: any): ActionTypes[typeof UPDATE_WHICH_MENTION] =>
    ({type: UPDATE_WHICH_MENTION, mention, groupId});

export const updateTemporalTime =
  (time: any, groupId: any): ActionTypes[typeof UPDATE_TEMPORAL_TIME] =>
    ({type: UPDATE_TEMPORAL_TIME, time, groupId});

export const updateTemporalTimeValue =
  (timeValue: any, groupId: any): ActionTypes[typeof UPDATE_TEMPORAL_TIME_VALUE] =>
    ({type: UPDATE_TEMPORAL_TIME_VALUE, timeValue, groupId});

export const loadEntities =
  (entities: any): ActionTypes[typeof LOAD_ENTITIES] =>
  ({type: LOAD_ENTITIES, entities});

export const resetStore =
  (): ActionTypes[typeof RESET_STORE] =>
  ({type: RESET_STORE});

export const clearStore =
  (): ActionTypes[typeof CLEAR_STORE] =>
  ({type: CLEAR_STORE});
