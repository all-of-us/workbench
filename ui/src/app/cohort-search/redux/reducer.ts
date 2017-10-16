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
  BEGIN_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  CANCEL_CRITERIA_REQUEST,
  CRITERIA_REQUEST_ERROR,
  BEGIN_COUNT_REQUEST,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,
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

      case BEGIN_CRITERIA_REQUEST:
        return state
          .updateIn(
            ['requests', 'criteria'],
            // path.rest() : removes the redundant 'criteria' prefix
            requestSet => requestSet.add(action.path.rest())
          );

      case LOAD_CRITERIA_RESULTS:
        return state
          .setIn(action.path, fromJS(action.results))
          .updateIn(
            ['requests', 'criteria'],
            requestSet => requestSet.delete(action.path.rest())
          );

      case CANCEL_CRITERIA_REQUEST:
        return state
          .updateIn(
            ['requests', 'criteria'],
            requestSet => requestSet.delete(action.path.rest())
          );

      case BEGIN_COUNT_REQUEST:
        return action.kind === 'total'
          ? state.setIn(['requests', 'total'], true)
          : state.updateIn(
            ['requests', action.kind],
            requestSet => requestSet.add(action.path)
          );

      case LOAD_COUNT_RESULTS:
        return action.kind === 'total'
          ? state
              .setIn(['counts', 'total'], action.count)
              .setIn(['requests', 'total'], false)
          : state
            .setIn(['counts', action.path], action.count)
            .updateIn(
              ['requests', action.kind],
              requestSet => requestSet.delete(action.path)
            );

      case CANCEL_COUNT_REQUEST:
        return action.kind === 'total'
          ? state.setIn(['requests', 'total'], false)
          : state.updateIn(
            ['requests', action.kind],
            requestSet => requestSet.delete(action.path)
          );

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
