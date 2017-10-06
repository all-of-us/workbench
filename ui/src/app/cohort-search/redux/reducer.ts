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
  prunePath,
} from './store';
import {CohortSearchActions as Actions} from './actions';

/**
 * the Root Reducer
 * Performs pure (and ideally very simple, atomic) transformations
 * of the application state
 */
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

      case Actions.FETCH_CRITERIA: {
        const {critType, parentId} = action;
        return state.setIn(['loading', critType, parentId], true);
      }

      case Actions.LOAD_CRITERIA: {
        const {children, critType, parentId} = action;

        state = state.setIn(['criteriaTree', critType, parentId], children);
        state = prunePath(state, ['loading', critType, parentId]);
        return state;
      }

      case Actions.SELECT_CRITERIA: {
        const path = getActiveSGIPath(state).unshift('search').push('searchParameters');
        return state.updateIn(path, List(), critlist => critlist.push(action.criteria));
      }

      case Actions.FETCH_SEARCH_RESULTS: {
        return state.setIn(action.sgiPath.unshift('loading'), true);
      }

      case Actions.LOAD_SEARCH_RESULTS: {
        const result = Set(action.results);
        const path = action.sgiPath.unshift('results');
        state = state
          .setIn(path.push('count'), result.size)
          .setIn(path.push('subjects'), result);

        state = prunePath(state, action.sgiPath.unshift('loading'));
        return state;
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
            ['results', 'include'], Map(),
            groupList => groupList.map(mapper(_included)))
          .updateIn(
            ['results', 'exclude'], Map(),
            groupList => groupList.map(mapper(_excluded)))
          .updateIn(
            ['results', 'subjects'], Set(),
            totalSubjects => {
              const include = Set.intersect(_included);
              const exclude = Set.intersect(_excluded);
              return include.subtract(exclude);
            });
      }

      case Actions.ERROR: {
        // TODO (jms) flesh out the rest of error handling / dispatching error
        // logs to wherever
        console.log(`ERROR: ${action.error}`);

        if (action.critType && action.parentId) {
          return state
            .update('loading', loadZone => {
              loadZone = loadZone.deleteIn([action.critType, action.parentId]);
              if (loadZone.get(action.critType).isEmpty()) {
                loadZone = loadZone.delete(action.critType);
              }
              return loadZone;
            })
            .deleteIn(getActiveSGIPath(state).unshift('search'))
            .setIn(['context', 'wizardOpen'], false);
        }

        return state;
      }

      default: return state;
    }
};
