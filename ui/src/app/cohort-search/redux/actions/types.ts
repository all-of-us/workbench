import {List} from 'immutable';
import {Criteria, SearchRequest} from 'generated';

export const BEGIN_CRITERIA_REQUEST = 'BEGIN_CRITERIA_REQUEST';
export const LOAD_CRITERIA_RESULTS = 'LOAD_CRITERIA_RESULTS';
export const CANCEL_CRITERIA_REQUEST = 'CANCEL_CRITERIA_REQUEST';
export const CRITERIA_REQUEST_ERROR = 'CRITERIA_REQUEST_ERROR';

export const BEGIN_COUNT_REQUEST = 'BEGIN_COUNT_REQUEST';
export const LOAD_COUNT_RESULTS = 'LOAD_COUNT_RESULTS';
export const CANCEL_COUNT_REQUEST = 'CANCEL_COUNT_REQUEST';
export const COUNT_REQUEST_ERROR = 'COUNT_REQUEST_ERROR';

export const INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
export const INIT_GROUP_ITEM = 'INIT_GROUP_ITEM';
export const SELECT_CRITERIA = 'SELECT_CRITERIA';
export const REMOVE = 'REMOVE';

export const SET_WIZARD_OPEN = 'SET_WIZARD_OPEN';
export const SET_WIZARD_CLOSED = 'SET_WIZARD_CLOSED';
export const SET_ACTIVE_CONTEXT = 'SET_ACTIVE_CONTEXT';
export const CLEAR_ACTIVE_CONTEXT = 'CLEAR_ACTIVE_CONTEXT';

interface ActiveContext {
  criteriaType?: string;
  role?: keyof SearchRequest;
  groupIndex?: number;
  groupItemIndex?: number;
}

export type KeyPath = List<number | string>;
export type CountRequestKind = 'item' | 'group' | 'total';

export interface ActionTypes {
  BEGIN_CRITERIA_REQUEST: {
    type: typeof BEGIN_CRITERIA_REQUEST;
    path: KeyPath;
  };
  LOAD_CRITERIA_RESULTS: {
    type: typeof LOAD_CRITERIA_RESULTS;
    path: KeyPath;
    results: Criteria[];
  };
  CANCEL_CRITERIA_REQUEST: {
    type: typeof CANCEL_CRITERIA_REQUEST;
    path: KeyPath;
  };
  CRITERIA_REQUEST_ERROR: {
    type: typeof CRITERIA_REQUEST_ERROR;
    error?: any;
    path?: KeyPath;
  };

  BEGIN_COUNT_REQUEST: {
    type: typeof BEGIN_COUNT_REQUEST;
    kind: CountRequestKind;
    path: KeyPath;
    request: SearchRequest;
  };
  LOAD_COUNT_RESULTS: {
    type: typeof LOAD_COUNT_RESULTS;
    kind: CountRequestKind;
    path: KeyPath;
    count: number;
  };
  CANCEL_COUNT_REQUEST: {
    type: typeof CANCEL_COUNT_REQUEST;
    kind: CountRequestKind;
    path?: KeyPath;
  };
  COUNT_REQUEST_ERROR: {
    type: typeof COUNT_REQUEST_ERROR;
    error?: any;
    path?: KeyPath;
  };

  INIT_SEARCH_GROUP: {
    type: typeof INIT_SEARCH_GROUP;
    role: keyof SearchRequest;
  };
  INIT_GROUP_ITEM: {
    type: typeof INIT_GROUP_ITEM;
    role: keyof SearchRequest;
    groupIndex: number;
  };
  SELECT_CRITERIA: {
    type: typeof SELECT_CRITERIA;
    criterion: Criteria;
  };
  REMOVE: {
    type: typeof REMOVE;
    path: KeyPath;
  };

  SET_WIZARD_OPEN: {
    type: typeof SET_WIZARD_OPEN;
  };
  SET_WIZARD_CLOSED: {
    type: typeof SET_WIZARD_CLOSED;
  };
  SET_ACTIVE_CONTEXT: {
    type: typeof SET_ACTIVE_CONTEXT;
    context: ActiveContext;
  };
  CLEAR_ACTIVE_CONTEXT: {
    type: typeof CLEAR_ACTIVE_CONTEXT;
  };
}

export type RootAction =
    ActionTypes[typeof BEGIN_CRITERIA_REQUEST]
  | ActionTypes[typeof LOAD_CRITERIA_RESULTS]
  | ActionTypes[typeof CANCEL_CRITERIA_REQUEST]
  | ActionTypes[typeof CRITERIA_REQUEST_ERROR]
  | ActionTypes[typeof BEGIN_COUNT_REQUEST]
  | ActionTypes[typeof LOAD_COUNT_RESULTS]
  | ActionTypes[typeof CANCEL_COUNT_REQUEST]
  | ActionTypes[typeof COUNT_REQUEST_ERROR]
  | ActionTypes[typeof INIT_SEARCH_GROUP]
  | ActionTypes[typeof INIT_GROUP_ITEM]
  | ActionTypes[typeof SELECT_CRITERIA]
  | ActionTypes[typeof REMOVE]
  | ActionTypes[typeof SET_WIZARD_OPEN]
  | ActionTypes[typeof SET_WIZARD_CLOSED]
  | ActionTypes[typeof SET_ACTIVE_CONTEXT]
  | ActionTypes[typeof CLEAR_ACTIVE_CONTEXT]
  ;
