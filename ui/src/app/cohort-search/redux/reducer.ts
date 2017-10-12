import {Reducer} from 'redux';
import {Map, List, fromJS} from 'immutable';

import {
  initialState,
  activeSearchGroupItemPath,
  activeCriteriaType,
  CohortSearchState,
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
        return state.update('requests', requestTable => requestTable.add(action.path));

      case CLEANUP_REQUEST:
        return state.update('requests', requestTable => requestTable.remove(action.path));

      case LOAD_CRITERIA_RESULTS:
        return state.updateIn(action.path.unshift('criteria'), fromJS(action.results));

      case LOAD_COUNT_RESULTS: {
        // action.path is [role, groupIndex, groupItemIndex, scope]
        const scope = action.path.last();
        const pathKey = {
          TOTAL: 'total',
          GROUP: action.path.skipLast(2),
          ITEM: action.path.skipLast(1),
        }[scope];
        return state.setIn(['counts', pathKey], action.count);
      }

      case INIT_SEARCH_GROUP:
        return state.updateIn(
          ['search', action.role],
          list => list.push(Map({items: List()}))
        );

      case INIT_GROUP_ITEM: {
        const itemPath = ['search', action.role, action.groupIndex, 'items'];
        const newItem = {
          type: activeCriteriaType(state),
          searchParameters: List(),
          modifiers: List(),
        };
        return state
          .updateIn(itemPath, items => items.push(newItem))
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
