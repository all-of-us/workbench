import {List} from 'immutable';
import * as ActionTypes from './types';
import {
  KeyPath,
  CriteriaType,
  ParentID,
  RequestAction, 
  CountRequestAction,
  CriteriaRequestAction,
} from '../typings';

import {Criteria, SearchRequest} from 'generated';


/*
 * Each Request is identified by a KeyPath to the resource being requested and
 * tracked in a table of in-flight requests.  These three functions manage
 * that request tracking.
 */
/* Puts `path` into the `state.requests` table */
export const startRequest =
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.START_REQUEST, path});

/* Signals to the RequestEpic to cancel the request for `path` */
export const cancelRequest =
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.CANCEL_REQUEST, path});

/* Removes `path` from the request table */
export const cleanupRequest =
  (path: KeyPath): RequestAction =>
  ({type: ActionTypes.CLEANUP_REQUEST, path});


/* Signals the RequestEpic to begin the request. The Request's path identifier
 * is `List([kind, parentId])`
 */
export const requestCriteria =
  (kind: CriteriaType, parentId: ParentID): CriteriaRequestAction =>
  ({type: ActionTypes.BEGIN_CRITERIA_REQUEST, path: List([kind, parentId])});

export const loadCriteriaRequestResults =
  (path: KeyPath, results: Criteria[]): CriteriaRequestAction =>
  ({type: ActionTypes.LOAD_CRITERIA_RESULTS, path, results, cleanup: cleanupRequest(path)});


/* A request for counts is scoped to SearchGroupItem, SearchGroup, or global.
 * This function doesn't care about scope; that has already been determined
 * by the time this is dispatched. The path + scope is used as request ID
 */
export const requestCounts =
  (path: KeyPath, request: SearchRequest): CountRequestAction =>
  ({type: ActionTypes.BEGIN_COUNT_REQUEST, path, request});

export const loadCountRequestResults =
  (path: KeyPath, count: number): CountRequestAction =>
  ({type: ActionTypes.LOAD_COUNT_RESULTS, path, count, cleanup: cleanupRequest(path)});


/* UI related actions */
export const initGroup =
  (role: keyof SearchRequest) => ({type: ActionTypes.INIT_SEARCH_GROUP, role});

export const initGroupItem =
  (role: keyof SearchRequest, groupIndex: number) =>
  ({type: ActionTypes.INIT_GROUP_ITEM, role, groupIndex});

export const selectCriteria =
  (criterion: Criteria) => ({type: ActionTypes.SELECT_CRITERIA, criterion});

export const remove =
  (path: KeyPath) => ({type: ActionTypes.REMOVE, path});

export const setWizardOpen =
  () => ({type: ActionTypes.SET_WIZARD_OPEN});

export const setWizardClosed =
  () => ({type: ActionTypes.SET_WIZARD_CLOSED});

export const setActiveContext =
  (context: object) => ({type: ActionTypes.SET_ACTIVE_CONTEXT, context});

export const clearActiveContext =
  () => ({type: ActionTypes.CLEAR_ACTIVE_CONTEXT});
