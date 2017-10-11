import {AnyAction} from 'redux';
import {List, Map, Set} from 'immutable';

import {Criteria, SearchRequest} from 'generated';

export type KeyPath = List<string | number>;
export type CriteriaType = string;
export type ParentID = number;

export interface RequestAction extends AnyAction {
  type: 'START_REQUEST'|'CANCEL_REQUEST'|'CLEANUP_REQUEST';
  path: KeyPath;
}

export interface CriteriaRequestAction extends AnyAction {
  type: 'BEGIN_CRITERIA_REQUEST'|'LOAD_CRITERIA_RESULTS';
  path: KeyPath,
  kind?: CriteriaType;
  parentId?: ParentID;
  results?: Criteria[];
}

export interface CountRequestAction extends AnyAction {
  type: 'BEGIN_COUNT_REQUEST'|'LOAD_COUNT_RESULTS';
  path: KeyPath,
  request?: SearchRequest;
  count?: number;
}

export const CountScope = {
  ITEM: 'ITEM',
  GROUP: 'GROUP',
  TOTAL: 'TOTAL',
};

/*
 * TODO (jms): strongly type the store
 */
export type CohortSearchState = Map<string, any>;
