import {ChartInfo, Criteria, Modifier, SearchRequest} from 'generated';

export const BEGIN_CRITERIA_REQUEST = 'BEGIN_CRITERIA_REQUEST';
export const BEGIN_ALL_CRITERIA_REQUEST = 'BEGIN_ALL_CRITERIA_REQUEST';
export const BEGIN_DRUG_CRITERIA_REQUEST = 'BEGIN_DRUG_CRITERIA_REQUEST';
export const LOAD_CRITERIA_RESULTS = 'LOAD_CRITERIA_RESULTS';
export const LOAD_DEMO_CRITERIA_RESULTS = 'LOAD_DEMO_CRITERIA_RESULTS';
export const CANCEL_CRITERIA_REQUEST = 'CANCEL_CRITERIA_REQUEST';
export const SET_CRITERIA_SEARCH = 'SET_CRITERIA_SEARCH';
export const BEGIN_DRUG_AUTOCOMPLETE_REQUEST = 'BEGIN_DRUG_AUTOCOMPLETE_REQUEST';
export const BEGIN_INGREDIENT_REQUEST = 'BEGIN_INGREDIENT_REQUEST';
export const LOAD_INGREDIENT_LIST = 'LOAD_INGREDIENT_LIST';
export const LOAD_AUTOCOMPLETE_OPTIONS = 'LOAD_AUTOCOMPLETE_OPTIONS';
export const CLEAR_AUTOCOMPLETE_OPTIONS = 'CLEAR_AUTOCOMPLETE_OPTIONS';
export const AUTOCOMPLETE_REQUEST_ERROR = 'AUTOCOMPLETE_REQUEST_ERROR';
export const CRITERIA_REQUEST_ERROR = 'CRITERIA_REQUEST_ERROR';

export const BEGIN_COUNT_REQUEST = 'BEGIN_COUNT_REQUEST';
export const BEGIN_ATTR_PREVIEW_REQUEST = 'BEGIN_ATTR_PREVIEW_REQUEST';
export const LOAD_ATTR_PREVIEW_RESULTS = 'LOAD_ATTR_PREVIEW_RESULTS';
export const ADD_ATTR_FOR_PREVIEW = 'ADD_ATTR_FOR_PREVIEW';
export const LOAD_COUNT_RESULTS = 'LOAD_COUNT_RESULTS';
export const CANCEL_COUNT_REQUEST = 'CANCEL_COUNT_REQUEST';
export const COUNT_REQUEST_ERROR = 'COUNT_REQUEST_ERROR';

export const BEGIN_PREVIEW_REQUEST = 'BEGIN_PREVIEW_REQUEST';
export const LOAD_PREVIEW_RESULTS = 'LOAD_PREVIEW_RESULTS';
export const PREVIEW_REQUEST_ERROR = 'PREVIEW_REQUEST_ERROR';

export const BEGIN_CHARTS_REQUEST = 'BEGIN_CHARTS_REQUEST';
export const LOAD_CHARTS_RESULTS = 'LOAD_CHARTS_RESULTS';
export const CANCEL_CHARTS_REQUEST = 'CANCEL_CHARTS_REQUEST';
export const CHARTS_REQUEST_ERROR = 'CHARTS_REQUEST_ERROR';

export const INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
export const ADD_PARAMETER = 'ADD_PARAMETER';
export const REMOVE_PARAMETER = 'REMOVE_PARAMETER';
export const ADD_MODIFIER = 'ADD_MODIFIER';
export const REMOVE_MODIFIER = 'REMOVE_MODIFIER';
export const SET_WIZARD_FOCUS = 'SET_WIZARD_FOCUS';
export const CLEAR_WIZARD_FOCUS = 'CLEAR_WIZARD_FOCUS';
export const REMOVE_ITEM = 'REMOVE_ITEM';
export const REMOVE_GROUP = 'REMOVE_GROUP';
export const SHOW_ATTRIBUTES_PAGE = 'SHOW_ATTRIBUTES_PAGE';
export const HIDE_ATTRIBUTES_PAGE = 'HIDE_ATTRIBUTES_PAGE';

export const OPEN_WIZARD = 'OPEN_WIZARD';
export const REOPEN_WIZARD = 'REOPEN_WIZARD';
export const WIZARD_FINISH = 'WIZARD_FINISH';
export const WIZARD_CANCEL = 'WIZARD_CANCEL';
export const SET_WIZARD_CONTEXT = 'SET_WIZARD_CONTEXT';

export const LOAD_ENTITIES = 'LOAD_ENTITIES';
export const RESET_STORE = 'RESET_STORE';

export interface ActiveContext {
  criteriaType?: string;
  fullTree?: boolean;
  role?: keyof SearchRequest;
  groupId?: number;
  itemId?: number;
}

export interface ActionTypes {
  BEGIN_CRITERIA_REQUEST: {
    type: typeof BEGIN_CRITERIA_REQUEST;
    cdrVersionId: number;
    kind: string;
    parentId: number;
  };
  BEGIN_ALL_CRITERIA_REQUEST: {
    type: typeof BEGIN_ALL_CRITERIA_REQUEST;
    cdrVersionId: number;
    kind: string;
    parentId: number;
  };
  BEGIN_DRUG_CRITERIA_REQUEST: {
    type: typeof BEGIN_DRUG_CRITERIA_REQUEST;
    cdrVersionId: number;
    kind: string;
    parentId: number;
    subtype: string;
  };
  LOAD_CRITERIA_RESULTS: {
    type: typeof LOAD_CRITERIA_RESULTS;
    kind: string;
    parentId: number;
    results: Criteria[];
  };
  LOAD_DEMO_CRITERIA_RESULTS: {
    type: typeof LOAD_DEMO_CRITERIA_RESULTS;
    kind: string;
    subtype: string;
    results: Criteria[];
  };
  CANCEL_CRITERIA_REQUEST: {
    type: typeof CANCEL_CRITERIA_REQUEST;
    kind: string;
    parentId: number;
  };
  SET_CRITERIA_SEARCH: {
    type: typeof SET_CRITERIA_SEARCH;
    searchTerms: Array<string>;
  };
  BEGIN_DRUG_AUTOCOMPLETE_REQUEST: {
    type: typeof BEGIN_DRUG_AUTOCOMPLETE_REQUEST;
    cdrVersionId: number;
    searchTerms: string;
  };
  BEGIN_INGREDIENT_REQUEST: {
    type: typeof BEGIN_INGREDIENT_REQUEST;
    cdrVersionId: number;
    conceptId: number;
  };
  LOAD_AUTOCOMPLETE_OPTIONS: {
    type: typeof LOAD_AUTOCOMPLETE_OPTIONS;
    options: any;
  };
  CLEAR_AUTOCOMPLETE_OPTIONS: {
    type: typeof CLEAR_AUTOCOMPLETE_OPTIONS;
  };
  AUTOCOMPLETE_REQUEST_ERROR: {
    type: typeof AUTOCOMPLETE_REQUEST_ERROR;
    error?: any;
  };
  LOAD_INGREDIENT_LIST: {
    type: typeof LOAD_INGREDIENT_LIST;
    ingredients: any;
  };
  CRITERIA_REQUEST_ERROR: {
    type: typeof CRITERIA_REQUEST_ERROR;
    kind: string;
    parentId: number;
    error?: any;
  };

  BEGIN_COUNT_REQUEST: {
    type: typeof BEGIN_COUNT_REQUEST;
    cdrVersionId: number;
    entityType: string;
    entityId: string;
    request: SearchRequest;
  };
  BEGIN_ATTR_PREVIEW_REQUEST: {
    type: typeof BEGIN_ATTR_PREVIEW_REQUEST;
    cdrVersionId: number;
    request: SearchRequest;
  };
  LOAD_ATTR_PREVIEW_RESULTS: {
    type: typeof LOAD_ATTR_PREVIEW_RESULTS;
    count: number;
  };
  ADD_ATTR_FOR_PREVIEW: {
    type: typeof ADD_ATTR_FOR_PREVIEW;
    parameter: any;
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

  BEGIN_PREVIEW_REQUEST: {
    type: typeof BEGIN_PREVIEW_REQUEST;
    cdrVersionId: number;
    request: SearchRequest;
  };
  LOAD_PREVIEW_RESULTS: {
    type: typeof LOAD_PREVIEW_RESULTS;
    count: number;
  };
  PREVIEW_REQUEST_ERROR: {
    type: typeof PREVIEW_REQUEST_ERROR;
    error?: any;
  };

  BEGIN_CHARTS_REQUEST: {
    type: typeof BEGIN_CHARTS_REQUEST;
    cdrVersionId: number;
    entityType: string;
    entityId: string;
    request: SearchRequest;
  };
  LOAD_CHARTS_RESULTS: {
    type: typeof LOAD_CHARTS_RESULTS;
    entityType: string;
    entityId: string;
    chartData: ChartInfo[];
  };
  CANCEL_CHARTS_REQUEST: {
    type: typeof CANCEL_CHARTS_REQUEST;
    entityType: string;
    entityId: string;
  };
  CHARTS_REQUEST_ERROR: {
    type: typeof CHARTS_REQUEST_ERROR;
    entityType: string;
    entityId: string;
    error?: any;
  };

  INIT_SEARCH_GROUP: {
    type: typeof INIT_SEARCH_GROUP;
    role: keyof SearchRequest;
    groupId: string;
  };

  ADD_PARAMETER: {
    type: typeof ADD_PARAMETER;
    parameter: any;
  };
  REMOVE_PARAMETER: {
    type: typeof REMOVE_PARAMETER;
    parameterId: string;
  };
  ADD_MODIFIER: {
    type: typeof ADD_MODIFIER;
    modifier: Modifier;
  };
  REMOVE_MODIFIER: {
    type: typeof REMOVE_MODIFIER;
    modifier: Modifier;
  };
  SET_WIZARD_FOCUS: {
    type: typeof SET_WIZARD_FOCUS;
    criterion: any;
  };
  CLEAR_WIZARD_FOCUS: {
    type: typeof CLEAR_WIZARD_FOCUS;
  };
  SHOW_ATTRIBUTES_PAGE: {
    type: typeof SHOW_ATTRIBUTES_PAGE;
    node: any
  };
  HIDE_ATTRIBUTES_PAGE: {
    type: typeof HIDE_ATTRIBUTES_PAGE;
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

  LOAD_ENTITIES: {
    type: typeof LOAD_ENTITIES;
    entities: any;
  };
  RESET_STORE: {
    type: typeof RESET_STORE;
  };
}

export type RootAction =
    ActionTypes[typeof BEGIN_CRITERIA_REQUEST]
  | ActionTypes[typeof BEGIN_ALL_CRITERIA_REQUEST]
  | ActionTypes[typeof BEGIN_DRUG_CRITERIA_REQUEST]
  | ActionTypes[typeof LOAD_CRITERIA_RESULTS]
  | ActionTypes[typeof LOAD_DEMO_CRITERIA_RESULTS]
  | ActionTypes[typeof CANCEL_CRITERIA_REQUEST]
  | ActionTypes[typeof SET_CRITERIA_SEARCH]
  | ActionTypes[typeof BEGIN_DRUG_AUTOCOMPLETE_REQUEST]
  | ActionTypes[typeof BEGIN_INGREDIENT_REQUEST]
  | ActionTypes[typeof LOAD_AUTOCOMPLETE_OPTIONS]
  | ActionTypes[typeof CLEAR_AUTOCOMPLETE_OPTIONS]
  | ActionTypes[typeof LOAD_INGREDIENT_LIST]
  | ActionTypes[typeof AUTOCOMPLETE_REQUEST_ERROR]
  | ActionTypes[typeof CRITERIA_REQUEST_ERROR]

  | ActionTypes[typeof BEGIN_COUNT_REQUEST]
  | ActionTypes[typeof BEGIN_ATTR_PREVIEW_REQUEST]
  | ActionTypes[typeof LOAD_ATTR_PREVIEW_RESULTS]
  | ActionTypes[typeof ADD_ATTR_FOR_PREVIEW]
  | ActionTypes[typeof LOAD_COUNT_RESULTS]
  | ActionTypes[typeof CANCEL_COUNT_REQUEST]
  | ActionTypes[typeof COUNT_REQUEST_ERROR]

  | ActionTypes[typeof BEGIN_PREVIEW_REQUEST]
  | ActionTypes[typeof LOAD_PREVIEW_RESULTS]
  | ActionTypes[typeof PREVIEW_REQUEST_ERROR]

  | ActionTypes[typeof BEGIN_CHARTS_REQUEST]
  | ActionTypes[typeof LOAD_CHARTS_RESULTS]
  | ActionTypes[typeof CANCEL_CHARTS_REQUEST]
  | ActionTypes[typeof CHARTS_REQUEST_ERROR]

  | ActionTypes[typeof INIT_SEARCH_GROUP]
  | ActionTypes[typeof ADD_PARAMETER]
  | ActionTypes[typeof REMOVE_PARAMETER]
  | ActionTypes[typeof ADD_MODIFIER]
  | ActionTypes[typeof REMOVE_MODIFIER]
  | ActionTypes[typeof SET_WIZARD_FOCUS]
  | ActionTypes[typeof CLEAR_WIZARD_FOCUS]
  | ActionTypes[typeof REMOVE_ITEM]
  | ActionTypes[typeof REMOVE_GROUP]
  | ActionTypes[typeof OPEN_WIZARD]
  | ActionTypes[typeof REOPEN_WIZARD]
  | ActionTypes[typeof WIZARD_FINISH]
  | ActionTypes[typeof WIZARD_CANCEL]
  | ActionTypes[typeof SET_WIZARD_CONTEXT]
  | ActionTypes[typeof SHOW_ATTRIBUTES_PAGE]
  | ActionTypes[typeof HIDE_ATTRIBUTES_PAGE]

  | ActionTypes[typeof LOAD_ENTITIES]
  | ActionTypes[typeof RESET_STORE]
  ;
