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
export const SELECT_CRITERIA = 'SELECT_CRITERIA';
export const UNSELECT_CRITERIA = 'UNSELECT_CRITERIA';
export const REMOVE_ITEM = 'REMOVE_ITEM';
export const REMOVE_GROUP = 'REMOVE_GROUP';

export const OPEN_WIZARD = 'OPEN_WIZARD';
export const REOPEN_WIZARD = 'REOPEN_WIZARD';
export const WIZARD_FINISH = 'WIZARD_FINISH';
export const WIZARD_CANCEL = 'WIZARD_CANCEL';
export const SET_WIZARD_CONTEXT = 'SET_WIZARD_CONTEXT';

interface ActiveContext {
  criteriaType?: string;
  role?: keyof SearchRequest;
  groupId?: number;
  itemId?: number;
}

export interface ActionTypes {
  BEGIN_CRITERIA_REQUEST: {
    type: typeof BEGIN_CRITERIA_REQUEST;
    kind: string;
    parentId: number;
  };
  LOAD_CRITERIA_RESULTS: {
    type: typeof LOAD_CRITERIA_RESULTS;
    kind: string;
    parentId: number;
    results: Criteria[];
  };
  CANCEL_CRITERIA_REQUEST: {
    type: typeof CANCEL_CRITERIA_REQUEST;
    kind: string;
    parentId: number;
  };
  CRITERIA_REQUEST_ERROR: {
    type: typeof CRITERIA_REQUEST_ERROR;
    kind: string;
    parentId: number;
    error?: any;
  };

  BEGIN_COUNT_REQUEST: {
    type: typeof BEGIN_COUNT_REQUEST;
    entityType: string;
    entityId: string;
    request: SearchRequest;
  };
  LOAD_COUNT_RESULTS: {
    type: typeof LOAD_COUNT_RESULTS;
    entityType: string;
    entityId: string;
    count: number;
  };
  CANCEL_COUNT_REQUEST: {
    type: typeof CANCEL_COUNT_REQUEST;
    entityType: string;
    entityId: string;
  };
  COUNT_REQUEST_ERROR: {
    type: typeof COUNT_REQUEST_ERROR;
    entityType: string;
    entityId: string;
    error?: any;
  };

  INIT_SEARCH_GROUP: {
    type: typeof INIT_SEARCH_GROUP;
    role: keyof SearchRequest;
    groupId: string;
  };

  SELECT_CRITERIA: {
    type: typeof SELECT_CRITERIA;
    criterion: any;
  };
  UNSELECT_CRITERIA: {
    type: typeof UNSELECT_CRITERIA;
    criterion?: any;
    criterionId?: number;
  };

  REMOVE_ITEM: {
    type: typeof REMOVE_ITEM;
    itemId: string;
    groupId: string
  };
  REMOVE_GROUP: {
    type: typeof REMOVE_GROUP;
    groupId: string;
    role: keyof SearchRequest;
  };

  OPEN_WIZARD: {
    type: typeof OPEN_WIZARD;
    itemId: string;
    context?: ActiveContext;
  };
  REOPEN_WIZARD: {
    type: typeof REOPEN_WIZARD;
    item: any;
    context?: ActiveContext;
  };
  WIZARD_FINISH: {
    type: typeof WIZARD_FINISH;
  };
  WIZARD_CANCEL: {
    type: typeof WIZARD_CANCEL;
  };
  SET_WIZARD_CONTEXT: {
    type: typeof SET_WIZARD_CONTEXT;
    context: ActiveContext;
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
  | ActionTypes[typeof SELECT_CRITERIA]
  | ActionTypes[typeof UNSELECT_CRITERIA]
  | ActionTypes[typeof REMOVE_ITEM]
  | ActionTypes[typeof REMOVE_GROUP]
  | ActionTypes[typeof OPEN_WIZARD]
  | ActionTypes[typeof REOPEN_WIZARD]
  | ActionTypes[typeof WIZARD_FINISH]
  | ActionTypes[typeof WIZARD_CANCEL]
  | ActionTypes[typeof SET_WIZARD_CONTEXT]
  ;
