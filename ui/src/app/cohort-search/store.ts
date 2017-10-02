import {combineReducers, AnyAction, Reducer} from 'redux';
import {CohortSearchActions as Actions} from './actions';
import {SearchRequest, SubjectListResponse as SubjectList} from 'generated';

export type RequestKey = 'include' | 'exclude';

export interface CohortSearchState {
  search: SearchRequest;
  subjects?: SubjectList;
  ui?: object;
  criteriaTree?: object;
  loading?: object;
}

export const InitialState = {
  search: {include: [[]], exclude: [[]]},
  subjects: [],
  ui: {wizardOpen: false},
  criteriaTree: {},
  loading: {},
};


// Pathspecs & selectors
export const InclusionGroups = ['search', 'include'];
export const ExclusionGroups = ['search', 'exclude'];


export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      /* Appends a new empty list to the end of whichever seearch group list is
       * specified by action.list */
      case Actions.INIT_SEARCH_GROUP: {
        const key = action.key;
        const newGroupList = [...state.search[key], []];
        return {...state, search: {...state.search, [key]: newGroupList}};
      }

      case Actions.REMOVE_SEARCH_GROUP: {
        const {key, index} = action;
        const newGroupList = [
          ...state.search[key].slice(0, index),
          ...state.search[key].slice(index + 1)
        ];
        return {...state, search: {...state.search, [key]: newGroupList}};
      }

      case Actions.REMOVE_GROUP_ITEM: {
        const {key, groupIndex, itemIndex} = action;
        const newGroup = state.search[key][groupIndex].filter(
          (elem, i) => (i !== itemIndex)
        );
        const newGroupList = state.search[key].filter(
          (elem, i) => (i === groupIndex ? newGroup : elem)
        );
        return {...state, search: {...state.search, [key]: newGroupList}};
      }

      case Actions.OPEN_WIZARD: {
        const ui = {
          ...state.ui,
          wizardOpen: true,
          wizardCriteriaType: action.criteria
        };
        return {...state, ui};
      }

      case Actions.CLOSE_WIZARD: {
        const ui = {wizardOpen: false};
        return {...state, ui};
      }

      case Actions.FETCH_CRITERIA: {
        const {critType: _type, parentId} = action;
        const loading = {...state.loading};
        if (loading[_type] === undefined) {
          loading[_type] = {};
        }
        loading[_type][parentId] = true;
        return {...state, loading};
      }

      case Actions.LOAD_CRITERIA: {
        // load in the children
        const {children, critType: _type, parentId} = action;
        const tree = {...state.criteriaTree};
        if (tree[_type] === undefined) {
          tree[_type] = {};
        }
        tree[_type][parentId] = children;

        // remove from loading area
        const loading = {...state.loading};
        delete loading[_type][parentId];
        if (loading[_type] === {}) {
          delete loading[_type];
        }

        return {...state, criteriaTree: tree, loading};
      }

      default: return state;
    }
};
