import {List} from 'immutable';
import {
  START_REQUEST,
  CANCEL_REQUEST,
  CLEANUP_REQUEST,
  BEGIN_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  BEGIN_COUNT_REQUEST,
  LOAD_COUNT_RESULTS,
  INIT_SEARCH_GROUP,
  INIT_GROUP_ITEM,
  SELECT_CRITERIA,
  REMOVE,
  SET_WIZARD_OPEN,
  SET_WIZARD_CLOSED,
  SET_ACTIVE_CONTEXT,
  CLEAR_ACTIVE_CONTEXT,
  ERROR,
  ActionTypes
} from './types';

import {KeyPath} from './types';
import {Criteria, SearchRequest} from 'generated';

/**
 * Each Request is identified by a KeyPath to the resource being requested and
 * tracked in a table of in-flight requests.  These three functions manage
 * that request tracking by, respectively,
 *    - pushing a KeyPath into the requests set
 *    - notifying the request handling Epic to cancel a request
 *    - or removing a KeyPath from the requests set
 */
export const startRequest =
  (path: KeyPath): ActionTypes[typeof START_REQUEST] =>
  ({type: START_REQUEST, path});

export const cancelRequest =
  (path: KeyPath): ActionTypes[typeof CANCEL_REQUEST] =>
  ({type: CANCEL_REQUEST, path});

export const cleanupRequest =
  (path: KeyPath): ActionTypes[typeof CLEANUP_REQUEST] =>
  ({type: CLEANUP_REQUEST, path});


/**
 * The KeyPath identifier for a Criteria is [criteriaType, parentId], where the
 * roots of any given criteria tree have parentId === 0.  `requestCriteria`
 * signals the Epic to begin the request; `loadCriteriaRequestResults` is fired
 * by the Epic upon success.
 */
export const requestCriteria =
  (kind: string, parentId: number): ActionTypes[typeof BEGIN_CRITERIA_REQUEST] =>
  ({type: BEGIN_CRITERIA_REQUEST, path: List([kind, parentId])});

export const loadCriteriaRequestResults =
  (path: KeyPath, results: Criteria[]): ActionTypes[typeof LOAD_CRITERIA_RESULTS] =>
  ({type: LOAD_CRITERIA_RESULTS, path, results, cleanup: cleanupRequest(path)});


/**
 * The KeyPath identifier for a Count request is the KeyPath to the originating
 * SearchGroupItem + the scope of the request ( i.e. 'ITEM', 'GROUP', or
 * 'TOTAL').  `requestCounts` triggers the Epic; `loadCountRequestResults` is
 * the Epic's success callback
 */
export const requestCounts =
  (path: KeyPath, request: SearchRequest): ActionTypes[typeof BEGIN_COUNT_REQUEST] =>
  ({type: BEGIN_COUNT_REQUEST, path, request});

export const loadCountRequestResults =
  (path: KeyPath, count: number): ActionTypes[typeof LOAD_COUNT_RESULTS] =>
  ({type: LOAD_COUNT_RESULTS, path, count, cleanup: cleanupRequest(path)});


/**
 * Pushes a Map {items => []} onto the end of either the include or exclude list
 * of search groups
 */
export const initGroup =
  (role: keyof SearchRequest): ActionTypes[typeof INIT_SEARCH_GROUP] =>
  ({type: INIT_SEARCH_GROUP, role});

/**
 * Pushes a new SearchGroupItem onto the end of the specified search group
 */
export const initGroupItem =
  (role: keyof SearchRequest, groupIndex: number): ActionTypes[typeof INIT_GROUP_ITEM] =>
  ({type: INIT_GROUP_ITEM, role, groupIndex});

/**
 * Copies a criterion from the criteria tree (where it is loaded) into the
 * active search group item's SearchParameters list
 */
export const selectCriteria =
  (criterion: Criteria): ActionTypes[typeof SELECT_CRITERIA] =>
  ({type: SELECT_CRITERIA, criterion});

/**
 * Removes the resource at `path`
 */
export const remove =
  (path: KeyPath): ActionTypes[typeof REMOVE] =>
  ({type: REMOVE, path});

/**
 * The next four all deal with wizard context.  Criteria selection is done via
 * wizards that are created in a given context, viz: is the wizard open?
 * The `context` object includes an `active` object that may have the following
 * keys:
 *    - criteriaType
 *    - role
 *    - groupIndex
 *    - groupItemIndex
 * Which collectively specify where to put the criteria once they're selected.
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


export const errorAction =
  (error: any): ActionTypes[typeof ERROR] =>
  ({type: ERROR, error});
