import {Action} from 'redux';
import {List, Map, Set} from 'immutable';

import {Criteria, SearchRequest} from 'generated';

export type KeyPath = List<string | number>;
export type CriteriaType = string;
export type ParentID = number;

export interface RequestAction extends Action {
  type: 'START_REQUEST' | 'CANCEL_REQUEST' | 'CLEANUP_REQUEST';
  path: KeyPath;
}

export interface CriteriaRequestAction extends Action {
  type: 'BEGIN_CRITERIA_REQUEST' | 'LOAD_CRITERIA_RESULTS';
  path: KeyPath,
  kind?: CriteriaType;
  parentId?: ParentID;
  results?: Criteria[];
  cleanup?: RequestAction;
}

export interface CountRequestAction extends Action {
  type: 'BEGIN_COUNT_REQUEST' | 'LOAD_COUNT_RESULTS';
  path: KeyPath,
  request?: SearchRequest;
  count?: number;
  cleanup?: RequestAction;
}

export interface UtilityAction extends Action {
  type: 'INIT_SEARCH_GROUP'
    | 'INIT_GROUP_ITEM'
    | 'SELECT_CRITERIA'
    | 'REMOVE'
    | 'SET_WIZARD_OPEN'
    | 'SET_WIZARD_CLOSED'
    | 'SET_ACTIVE_CONTEXT'
    | 'CLEAR_ACTIVE_CONTEXT';
  context?: object;
  path?: KeyPath;
  criterion?: Criteria;
  role?: keyof SearchRequest;
  groupIndex?: number;
}

export type CohortSearchActionType = RequestAction
  | CriteriaRequestAction
  | CountRequestAction
  | UtilityAction;

export const CountScope = {
  ITEM: 'ITEM',
  GROUP: 'GROUP',
  TOTAL: 'TOTAL',
};

/*
 * TODO (jms): strongly type the store
 */
export type CohortSearchState = Map<string, any>;
