import {List} from 'immutable';
import {Criteria, SearchRequest} from 'generated';

export const START_REQUEST = 'START_REQUEST';
export const CANCEL_REQUEST = 'CANCEL_REQUEST';
export const CLEANUP_REQUEST = 'CLEANUP_REQUEST';

export const BEGIN_CRITERIA_REQUEST = 'BEGIN_CRITERIA_REQUEST';
export const LOAD_CRITERIA_RESULTS = 'LOAD_CRITERIA_RESULTS';

export const BEGIN_COUNT_REQUEST = 'BEGIN_COUNT_REQUEST';
export const LOAD_COUNT_RESULTS = 'LOAD_COUNT_RESULTS';

export const INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
export const INIT_GROUP_ITEM = 'INIT_GROUP_ITEM';
export const SELECT_CRITERIA = 'SELECT_CRITERIA';
export const REMOVE = 'REMOVE';

export const SET_WIZARD_OPEN = 'SET_WIZARD_OPEN';
export const SET_WIZARD_CLOSED = 'SET_WIZARD_CLOSED';
export const SET_ACTIVE_CONTEXT = 'SET_ACTIVE_CONTEXT';
export const CLEAR_ACTIVE_CONTEXT = 'CLEAR_ACTIVE_CONTEXT';

export const ERROR = 'ERROR';

interface ActiveContext {
  criteriaType?: string;
  role?: keyof SearchRequest;
  groupIndex?: number;
  groupItemIndex?: number;
}

export type KeyPath = List<number | string>;

export interface ActionTypes {
  START_REQUEST: {
    type: typeof START_REQUEST;
    path: KeyPath;
  };
  CANCEL_REQUEST: {
    type: typeof CANCEL_REQUEST;
    path: KeyPath;
  };
  CLEANUP_REQUEST: {
    type: typeof CLEANUP_REQUEST;
    path: KeyPath;
  };

  BEGIN_CRITERIA_REQUEST: {
    type: typeof BEGIN_CRITERIA_REQUEST;
    path: KeyPath;
  };
  LOAD_CRITERIA_RESULTS: {
    type: typeof LOAD_CRITERIA_RESULTS;
    path: KeyPath;
    results: Criteria[];
    cleanup: ActionTypes[typeof CLEANUP_REQUEST];
  };

  BEGIN_COUNT_REQUEST: {
    type: typeof BEGIN_COUNT_REQUEST;
    path: KeyPath;
    request: SearchRequest;
  };
  LOAD_COUNT_RESULTS: {
    type: typeof LOAD_COUNT_RESULTS;
    path: KeyPath;
    count: number;
    cleanup: ActionTypes[typeof CLEANUP_REQUEST];
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

  ERROR: {
    type: typeof ERROR;
    error?: any;
  };
}

export type RootAction =
    ActionTypes[typeof START_REQUEST]
  | ActionTypes[typeof START_REQUEST]
  | ActionTypes[typeof CANCEL_REQUEST]
  | ActionTypes[typeof CLEANUP_REQUEST]
  | ActionTypes[typeof BEGIN_CRITERIA_REQUEST]
  | ActionTypes[typeof LOAD_CRITERIA_RESULTS]
  | ActionTypes[typeof BEGIN_COUNT_REQUEST]
  | ActionTypes[typeof LOAD_COUNT_RESULTS]
  | ActionTypes[typeof INIT_SEARCH_GROUP]
  | ActionTypes[typeof INIT_GROUP_ITEM]
  | ActionTypes[typeof SELECT_CRITERIA]
  | ActionTypes[typeof REMOVE]
  | ActionTypes[typeof SET_WIZARD_OPEN]
  | ActionTypes[typeof SET_WIZARD_CLOSED]
  | ActionTypes[typeof SET_ACTIVE_CONTEXT]
  | ActionTypes[typeof CLEAR_ACTIVE_CONTEXT]
  | ActionTypes[typeof ERROR]
  ;
