/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  CANCEL_CRITERIA_REQUEST,
  CRITERIA_REQUEST_ERROR,

  BEGIN_COUNT_REQUEST,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,

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
  REMOVE_ITEM,
  REMOVE_GROUP,
  OPEN_WIZARD,
  REOPEN_WIZARD,
  WIZARD_FINISH,
  WIZARD_CANCEL,
  SET_WIZARD_CONTEXT,

  LOAD_ENTITIES,
  RESET_STORE,
  ActionTypes,
} from './types';
/* tslint:enable:ordered-imports */

import {ChartInfo, Criteria, SearchRequest} from 'generated';

/**
 * Criteria loading mgmt
 */
export const requestCriteria =
  (cdrVersionId: number, kind: string, parentId: number
  ): ActionTypes[typeof BEGIN_CRITERIA_REQUEST] =>
  ({type: BEGIN_CRITERIA_REQUEST, cdrVersionId, kind, parentId});

export const loadCriteriaRequestResults =
  (kind: string, parentId: number, results: Criteria[]
  ): ActionTypes[typeof LOAD_CRITERIA_RESULTS] =>
  ({type: LOAD_CRITERIA_RESULTS, kind, parentId, results});

export const cancelCriteriaRequest =
  (kind: string, parentId: number
  ): ActionTypes[typeof CANCEL_CRITERIA_REQUEST] =>
  ({type: CANCEL_CRITERIA_REQUEST, kind, parentId});

export const criteriaRequestError =
  (kind: string, parentId: number, error?: any
  ): ActionTypes[typeof CRITERIA_REQUEST_ERROR] =>
  ({type: CRITERIA_REQUEST_ERROR, kind, parentId, error});

/**
 * Count loading mgmt
 */
export const requestCounts =
  (cdrVersionId: number, entityType: string, entityId: string, request: SearchRequest
  ): ActionTypes[typeof BEGIN_COUNT_REQUEST] =>
  ({type: BEGIN_COUNT_REQUEST, cdrVersionId, entityType, entityId, request});

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

/**
 * Count loading mgmt
 */
export const requestCharts =
  (cdrVersionId: number, entityType: string, entityId: string, request: SearchRequest
  ): ActionTypes[typeof BEGIN_CHARTS_REQUEST] =>
  ({type: BEGIN_CHARTS_REQUEST, cdrVersionId, entityType, entityId, request});

export const loadChartsRequestResults =
  (entityType: string, entityId: string, chartData: ChartInfo[]
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
  (parameterId: string
  ): ActionTypes[typeof REMOVE_PARAMETER] =>
  ({type: REMOVE_PARAMETER, parameterId});

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

export const removeGroup =
  (role: keyof SearchRequest, groupId: string
  ): ActionTypes[typeof REMOVE_GROUP] =>
  ({type: REMOVE_GROUP, role, groupId});

export const removeGroupItem =
  (groupId: string, itemId: string
  ): ActionTypes[typeof REMOVE_ITEM] =>
  ({type: REMOVE_ITEM, groupId, itemId});

/**
 * Context mgmt
 */
export const openWizard =
  (itemId: string, context: object
  ): ActionTypes[typeof OPEN_WIZARD] =>
  ({type: OPEN_WIZARD, itemId, context});

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

export const loadEntities =
  (entities: any): ActionTypes[typeof LOAD_ENTITIES] =>
  ({type: LOAD_ENTITIES, entities});

export const resetStore =
  (): ActionTypes[typeof RESET_STORE] =>
  ({type: RESET_STORE});
