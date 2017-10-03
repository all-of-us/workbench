import {AnyAction, Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';

import {CohortSearchActions as Actions} from './actions';
import {SearchRequest, SubjectListResponse as SubjectList, SearchGroupItem} from 'generated';


export type CohortSearchState = Map<string, any>;

export const InitialState = fromJS({
  search: {include: [[]], exclude: [[]]},
  results: {include: [[]], exlude: [[]]},
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  loading: {},
});


// Pathspecs & selectors
export const inclusionGroups = ['search', 'include'];
export const exclusionGroups = ['search', 'exclude'];
export const wizardOpen = ['context', 'wizardOpen'];
export const activeCriteriaType = ['context', 'active', 'criteriaType'];
export const activeSGRole = ['context', 'active', 'sgRole'];
export const activeSGIndex = ['context', 'active', 'sgIndex'];
export const activeSGItemIndex = ['context', 'active', 'sgItemIndex'];

/* The actual, not symbolic path */
export const getActiveSGIPath = (state) => List([
  state.getIn(activeSGRole),
  state.getIn(activeSGIndex),
  state.getIn(activeSGItemIndex),
]);

export const subjects = (state) => {
  const unions = (role) => (
    state
      .getIn(['results', role], List())
      .map(group => Set.union(group.map(item => item.get('subjects', Set()))))
  );
  const include = Set.intersect(unions('include'));
  const exclude = Set.intersect(unions('exclude'));
  console.log('Generating total set: ');
  console.log(include.subtract(exclude));
  return include.subtract(exclude);
};


export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      case Actions.INIT_SEARCH_GROUP: {
        return state.updateIn(['search', action.sgRole], list => list.push(List()));
      }

      case Actions.REMOVE_GROUP_ITEM:
      case Actions.REMOVE_SEARCH_GROUP:
      case Actions.REMOVE_CRITERIA: {
        return state
          .deleteIn(action.path.unshift('search'))
          .deleteIn(action.path.unshift('results'));
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
        const path = getActiveSGIPath(state);
        return state
          .deleteIn(path.unshift('search'))
          .deleteIn(path.unshift('results'))
          .setIn(['context', 'wizardOpen'], false);
      }

      case Actions.FETCH_CRITERIA: {
        const {critType: _type, parentId} = action;
        return state.setIn(['loading', _type, parentId], true);
      }

      case Actions.LOAD_CRITERIA: {
        const {children, critType: _type, parentId} = action;
        state = state
          .setIn(['criteriaTree', _type, parentId], children)
          .deleteIn(['loading', _type, parentId]);
        if (state.getIn(['loading', _type]).isEmpty()) {
          state = state.deleteIn(['loading', _type]);
        }
        return state;
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
        const result = Set(action.results);
        const path = action.sgiPath.unshift('results');
        return state
          .setIn(path.push('count'), result.size)
          .setIn(path.push('subjects'), result);
      }

      case Actions.ERROR: {
        // TODO (jms)
        return state;
      }

      default: return state;
    }
};
