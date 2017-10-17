import {List} from 'immutable';
import {
  BEGIN_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  CANCEL_CRITERIA_REQUEST,
  CRITERIA_REQUEST_ERROR,
  BEGIN_COUNT_REQUEST,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,
  INIT_SEARCH_GROUP,
  INIT_GROUP_ITEM,
  SELECT_CRITERIA,
  REMOVE_ITEM,
  REMOVE_GROUP,
  REMOVE_CRITERION,
  SET_WIZARD_OPEN,
  SET_WIZARD_CLOSED,
  SET_ACTIVE_CONTEXT,
  CLEAR_ACTIVE_CONTEXT,
  ActionTypes,
} from './types';

import {Criteria, SearchRequest} from 'generated';

/**
 * Criteria loading mgmt
 */
export const requestCriteria =
  (kind: string, parentId: number
  ): ActionTypes[typeof BEGIN_CRITERIA_REQUEST] =>
  ({type: BEGIN_CRITERIA_REQUEST, kind, parentId});

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
  (entityType: string, entityId: string, request: SearchRequest
  ): ActionTypes[typeof BEGIN_COUNT_REQUEST] =>
  ({type: BEGIN_COUNT_REQUEST, entityType, entityId, request});

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
 * Entity creation & deletion mgmt
 */
export const initGroup =
  (role: keyof SearchRequest, groupId: string
  ): ActionTypes[typeof INIT_SEARCH_GROUP] =>
  ({type: INIT_SEARCH_GROUP, role, groupId});

export const initGroupItem =
  (itemId: string, groupId: string
  ): ActionTypes[typeof INIT_GROUP_ITEM] =>
  ({type: INIT_GROUP_ITEM, itemId, groupId});

export const selectCriteria =
  (itemId: string, criterion: Criteria
  ): ActionTypes[typeof SELECT_CRITERIA] =>
  ({type: SELECT_CRITERIA, itemId, criterion});

export const removeGroup =
  (role: keyof SearchRequest, groupId: string
  ): ActionTypes[typeof REMOVE_GROUP] =>
  ({type: REMOVE_GROUP, role, groupId});

export const removeGroupItem =
  (groupId: string, itemId: string
  ): ActionTypes[typeof REMOVE_ITEM] =>
  ({type: REMOVE_ITEM, groupId, itemId});

export const removeCriterion =
  (itemId: string, criterionId: number
  ): ActionTypes[typeof REMOVE_CRITERION] =>
  ({type: REMOVE_CRITERION, itemId, criterionId});

/**
 * Context mgmt
 */
export const setWizardOpen =
  (): ActionTypes[typeof SET_WIZARD_OPEN] => ({type: SET_WIZARD_OPEN});

export const setWizardClosed =
  (): ActionTypes[typeof SET_WIZARD_CLOSED] => ({type: SET_WIZARD_CLOSED});

export const setActiveContext =
  (context: object): ActionTypes[typeof SET_ACTIVE_CONTEXT] =>
  ({type: SET_ACTIVE_CONTEXT, context});

export const clearActiveContext =
  (): ActionTypes[typeof CLEAR_ACTIVE_CONTEXT] => ({type: CLEAR_ACTIVE_CONTEXT});
