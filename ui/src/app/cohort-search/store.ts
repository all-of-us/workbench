import {combineReducers, AnyAction, Reducer} from 'redux';
import {CohortSearchActions as Actions} from './actions';
import {SearchRequest, SubjectListResponse as SubjectList, SearchGroupItem} from 'generated';
import {AppContext, CohortSearchState, SearchGroupRole} from './store.interfaces';

export const InitialState = {
  search: {include: [[]], exclude: [[]]},
  subjects: [],
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  loading: {},
};


// Pathspecs & selectors
export const inclusionGroups = ['search', 'include'];
export const exclusionGroups = ['search', 'exclude'];
export const wizardOpen = ['context', 'wizardOpen'];
export const activeCriteriaType = ['context', 'active', 'criteria'];


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
        const {criteria, sgIndex, sgRole} = action;
        const context = {
          ...state.context,
          wizardOpen: true,
          active: { criteria, sgIndex, sgRole }
        };
        return {...state, context};
      }

      case Actions.CLOSE_WIZARD: {
        const context = {wizardOpen: false};
        return {...state, context};
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

      case Actions.SELECT_CRITERIA: {
        return state;
      }

      default: return state;
    }
};
