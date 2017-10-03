import {AnyAction, Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';

import {CohortSearchActions as Actions} from './actions';
import {SearchRequest, SubjectListResponse as SubjectList, SearchGroupItem} from 'generated';


export type CohortSearchState = Map<string, any>;

export const InitialState = fromJS({
  search: {include: [[]], exclude: [[]]},
  subjects: Set(),
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  loading: {},
  chartData: {},
});


// Pathspecs & selectors
export const inclusionGroups = ['search', 'include'];
export const exclusionGroups = ['search', 'exclude'];
export const wizardOpen = ['context', 'wizardOpen'];
export const activeCriteriaType = ['context', 'active', 'criteriaType'];
export const activeSGRole = ['context', 'active', 'sgRole'];
export const activeSGIndex = ['context', 'active', 'sgIndex'];
export const activeSGItemIndex = ['context', 'active', 'sgItemIndex'];

export const sgiPath = (state) => ([
  'search', 
  state.getIn(['context', 'request', 'sgRole']),
  state.getIn(['context', 'request', 'sgIndex']),
  state.getIn(['context', 'request', 'sgItemIndex']),
]);

export const activeSGIPath = (state) => ([
  'search', 
  state.getIn(['context', 'active', 'sgRole']),
  state.getIn(['context', 'active', 'sgIndex']),
  state.getIn(['context', 'active', 'sgItemIndex']),
]);


export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      case Actions.INIT_SEARCH_GROUP: {
        return state.updateIn(['search', action.sgRole], list => list.push(List()));
      }

      case Actions.REMOVE_SEARCH_GROUP: {
        return state.deleteIn(['search', action.sgRole, action.sgIndex]);
      }

      case Actions.INIT_GROUP_ITEM: {
        const criteriaType = state.getIn(activeCriteriaType);
        const sgRole = state.getIn(activeSGRole);
        const sgIndex = state.getIn(activeSGIndex);
        const newItem = Map({
          type: criteriaType,
          searchParameters: List(),
          modifiers: List(),
        });
        const sgPath = ['search', sgRole, sgIndex];
        return state.updateIn(sgPath, group => group.push(newItem))
          .setIn(activeSGItemIndex, state.getIn(sgPath).size);
      }

      case Actions.REMOVE_GROUP_ITEM: {
        const {sgRole, groupIndex, itemIndex} = action;
        return state.deleteIn(['search', sgRole, groupIndex, itemIndex]);
      }

      case Actions.SET_WIZARD_CONTEXT: {
        const {criteriaType, sgIndex, sgRole} = action;
        const newContext = Map({criteriaType, sgIndex, sgRole});
        return state.mergeIn(['context', 'active'], newContext);
      }

      case Actions.OPEN_WIZARD: {
        return state.setIn(['context', 'wizardOpen'], true);
      }

      case Actions.FINISH_WIZARD: {
        // TODO(jms): this is where "successfull" closing of wizard happens;
        // some more processing is due here
        return state.setIn(['context', 'wizardOpen'], false);
      }

      case Actions.CANCEL_WIZARD: {
        const sgRole = state.getIn(activeSGRole);
        const sgIndex = state.getIn(activeSGIndex);
        const sgItemIndex = state.getIn(activeSGItemIndex);
        return state
          .deleteIn(['search', sgRole, sgIndex, sgItemIndex])
          .setIn(['context', 'wizardOpen'], false);
      }

      case Actions.FETCH_CRITERIA: {
        const {critType: _type, parentId} = action;
        return state.setIn(['loading', _type, parentId], true);
      }

      case Actions.LOAD_CRITERIA: {
        const {children, critType: _type, parentId} = action;
        return state
          .setIn(['criteriaTree', _type, parentId], children)
          .deleteIn(['loading', _type, parentId]);
      }

      case Actions.SELECT_CRITERIA: {
        const sgRole = state.getIn(activeSGRole);
        const sgIndex = state.getIn(activeSGIndex);
        const sgItemIndex = state.getIn(activeSGItemIndex);

        const paramPath = ['search', sgRole, sgIndex, sgItemIndex, 'searchParameters'];
        const addCriteria = (list) => list.push(action.criteria);
        return state.updateIn(paramPath, List(), addCriteria);
      }

      case Actions.LOAD_SEARCH_RESULTS: {
        const resultSet = Set(action.results);
        const countPath = sgiPath(state).concat(['count']);
        return state.setIn(countPath, resultSet.size)
          .update('subjects', Set(), subjectSet => subjectSet.union(resultSet));
      }

      case Actions.ERROR: {
        // TODO (jms)
        return state;
      }

      default: return state;
    }
};
