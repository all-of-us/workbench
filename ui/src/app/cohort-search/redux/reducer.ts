import {AnyAction, Reducer} from 'redux';
import {Map, List, Set, fromJS, isCollection} from 'immutable';

import {
  CohortSearchState,
  InitialState,
  getActiveSGIPath,
  activeCriteriaType,
  activeSGRole,
  activeSGIndex,
  activeSGItemIndex,
} from './store';
import {CohortSearchActions as Actions} from './actions';
import requestReducer from './requests';


export const rootReducer = (state, action) => [
  mainReducer,
  requestReducer,
].reduce((s, r) => r(s, action), state);


export const mainReducer: Reducer<CohortSearchState> =
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
        const critType = state.getIn(activeCriteriaType);
        const sgRole = state.getIn(activeSGRole);
        const sgIndex = state.getIn(activeSGIndex);
        const newItem = Map({
          type: critType,
          searchParameters: List(),
          modifiers: List(),
        });
        const sgPath = ['search', sgRole, sgIndex];
        return state.updateIn(sgPath, group => group.push(newItem))
          .setIn(activeSGItemIndex, state.getIn(sgPath).size);
      }

      case Actions.SET_WIZARD_CONTEXT: {
        const {critType, sgIndex, sgRole} = action;
        const newContext = Map({critType, sgIndex, sgRole});
        return state.mergeIn(['context', 'active'], newContext);
      }

      case Actions.OPEN_WIZARD: {
        return state.setIn(['context', 'wizardOpen'], true);
      }

      case Actions.FINISH_WIZARD: {
        return state.setIn(['context', 'wizardOpen'], false);
      }

      case Actions.CANCEL_WIZARD: {
        const path = getActiveSGIPath(state);
        return state
          .deleteIn(path.unshift('search'))
          .setIn(['context', 'wizardOpen'], false);
      }

      case Actions.LOAD_CRITERIA: {
        const {children, critType, parentId} = action;
        return state.setIn(['criteriaTree', critType, parentId], children);
      }

      case Actions.SELECT_CRITERIA: {
        const path = getActiveSGIPath(state).unshift('search').push('searchParameters');
        return state.updateIn(path, List(), critlist => critlist.push(action.criteria));
      }

      case Actions.LOAD_SEARCH_RESULTS: {
        const result = Set(action.results);
        const path = action.sgiPath.unshift('results');
        return state
          .setIn(path.push('count'), result.size)
          .setIn(path.push('subjects'), result);
      }

      case Actions.RECALCULATE_COUNTS: {
        const _included = [];
        const _excluded = [];

        const getSubjects = group => Set.union(
          group
            .filter(isCollection)
            .map(item => item.get('subjects', Set()))
        );

        const mapper = (memoLoc) => (value, key) => {
          if (key === 'count') {
            return value;
          }
          const subjects = getSubjects(value);
          memoLoc.push(subjects);
          return value.set('count', subjects.size);
        };

        return state
          .updateIn(
            ['results', 'includes'], Map(),
            groupList => groupList.map(mapper(_included)))
          .updateIn(
            ['results', 'excludes'], Map(),
            groupList => groupList.map(mapper(_excluded)))
          .updateIn(
            ['results', 'subjects'], Set(),
            totalSubjects => {
              const includes = Set.intersect(_included);
              const excludes = Set.intersect(_excluded);
              return includes.subtract(excludes);
            });
      }

      case Actions.ERROR: {
        // TODO (jms) flesh out the rest of error handling / dispatching error
        // logs to wherever
        console.log(`ERROR: ${action.error}`);
        return state;
      }

      default: return state;
    }
};
