import {Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';

import {
  initialState,
  activeSearchGroupItemPath,
  activeCriteriaType,
  CohortSearchState,
  prunePath,
} from './store';

import {
  START_REQUEST,
  CLEANUP_REQUEST,
  LOAD_CRITERIA_RESULTS,
  LOAD_COUNT_RESULTS,
  INIT_SEARCH_GROUP,
  INIT_GROUP_ITEM,
  SELECT_CRITERIA,
  REMOVE,
  SET_WIZARD_OPEN,
  SET_WIZARD_CLOSED,
  SET_ACTIVE_CONTEXT,
  CLEAR_ACTIVE_CONTEXT,
  RootAction,
} from './actions/types';

/**
 * The root Reducer.  Handles synchronous changes to application State
 */

export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = initialState, action: RootAction): CohortSearchState => {
    switch (action.type) {

      case START_REQUEST:
        return state.setIn(action.path.unshift('requests'), true);

      case CLEANUP_REQUEST:
        return state
          // First delete the Leaf value of True
          .deleteIn(action.path.unshift('requests'))
          // Then prune the tree of any empty collections
          .update('requests', tree => prunePath(action.path.butLast(), tree));

      case LOAD_CRITERIA_RESULTS:
        return state.setIn(action.path, fromJS(action.results));

      case LOAD_COUNT_RESULTS: {
        // action.path is ['search', role, groupIndex, 'items', groupItemIndex, scope]
        const scope = action.path.last();
        const pathKey = {
          TOTAL: 'total',
          GROUP: action.path.skipLast(3),
          ITEM: action.path.skipLast(1),
        }[scope];
        return state.setIn(['counts', pathKey], action.count);
      }

      case INIT_SEARCH_GROUP:
        return state.updateIn(
          ['search', action.role],
          List(),
          list => list.push(Map({items: List()}))
        );

      case INIT_GROUP_ITEM: {
        const itemPath = ['search', action.role, action.groupIndex, 'items'];
        const newItem = fromJS({
          type: activeCriteriaType(state),
          searchParameters: [],
          modifiers: [],
        });
        return state
          .updateIn(itemPath, List(), items => items.push(newItem))
          .setIn(
            ['context', 'active', 'groupItemIndex'],
            state.getIn(itemPath).size
          );
      }

      case SELECT_CRITERIA:
        return state.updateIn(
          activeSearchGroupItemPath(state).push('searchParameters'),
          List(),
          criteria => criteria.push(action.criterion)
        );

      case REMOVE:
        return state.deleteIn(action.path);

      case SET_WIZARD_OPEN:
        return state.setIn(['context', 'wizardOpen'], true);

      case SET_WIZARD_CLOSED:
        return state.setIn(['context', 'wizardOpen'], false);

      case SET_ACTIVE_CONTEXT:
        return state.mergeIn(['context', 'active'], action.context);

      case CLEAR_ACTIVE_CONTEXT:
        return state.setIn(['context', 'active'], Map());

      default: return state;
    }
};
