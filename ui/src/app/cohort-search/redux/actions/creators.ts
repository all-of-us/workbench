import {List} from 'immutable';
import * as ActionTypes from './types';
import {
  KeyPath,
  CriteriaType,
  ParentID,
  RequestAction,
  CountRequestAction,
  CriteriaRequestAction,
  UtilityAction,
} from '../typings';

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
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.START_REQUEST, path});

export const cancelRequest =
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.CANCEL_REQUEST, path});

export const cleanupRequest =
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.CLEANUP_REQUEST, path});


/**
 * The KeyPath identifier for a Criteria is [criteriaType, parentId], where the
 * roots of any given criteria tree have parentId === 0.  `requestCriteria`
 * signals the Epic to begin the request; `loadCriteriaRequestResults` is fired
 * by the Epic upon success.
 */
export const requestCriteria =
  (kind: CriteriaType, parentId: ParentID): CriteriaRequestAction =>
  ({type: ActionTypes.BEGIN_CRITERIA_REQUEST, path: List([kind, parentId])});

export const loadCriteriaRequestResults =
  (path: KeyPath, results: Criteria[]): CriteriaRequestAction =>
  ({type: ActionTypes.LOAD_CRITERIA_RESULTS, path, results, cleanup: cleanupRequest(path)});


/**
 * The KeyPath identifier for a Count request is the KeyPath to the originating
 * SearchGroupItem + the scope of the request ( i.e. 'ITEM', 'GROUP', or
 * 'TOTAL').  `requestCounts` triggers the Epic; `loadCountRequestResults` is
 * the Epic's success callback
 */
export const requestCounts =
  (path: KeyPath, request: SearchRequest): CountRequestAction =>
  ({type: ActionTypes.BEGIN_COUNT_REQUEST, path, request});

export const loadCountRequestResults =
  (path: KeyPath, count: number): CountRequestAction =>
  ({type: ActionTypes.LOAD_COUNT_RESULTS, path, count, cleanup: cleanupRequest(path)});


/**
 * Pushes a Map {items => []} onto the end of either the include or exclude list
 * of search groups
 */
export const initGroup =
  (role: keyof SearchRequest): UtilityAction =>
  ({type: ActionTypes.INIT_SEARCH_GROUP, role});

/**
 * Pushes a new SearchGroupItem onto the end of the specified search group
 */
export const initGroupItem =
  (role: keyof SearchRequest, groupIndex: number): UtilityAction =>
  ({type: ActionTypes.INIT_GROUP_ITEM, role, groupIndex});

/**
 * Copies a criterion from the criteria tree (where it is loaded) into the
 * active search group item's SearchParameters list
 */
export const selectCriteria =
  (criterion: Criteria): UtilityAction =>
  ({type: ActionTypes.SELECT_CRITERIA, criterion});

/**
 * Removes the resource at `path`
 */
export const remove =
  (path: KeyPath): UtilityAction =>
  ({type: ActionTypes.REMOVE, path});

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
  (): UtilityAction => ({type: ActionTypes.SET_WIZARD_OPEN});

export const setWizardClosed =
  (): UtilityAction => ({type: ActionTypes.SET_WIZARD_CLOSED});

export const setActiveContext =
  (context: object): UtilityAction =>
  ({type: ActionTypes.SET_ACTIVE_CONTEXT, context});

export const clearActiveContext =
  (): UtilityAction => ({type: ActionTypes.CLEAR_ACTIVE_CONTEXT});
