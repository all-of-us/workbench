import {AnyAction, Reducer} from 'redux';
import {Map, List, fromJS} from 'immutable';

import {
  InitialState,
  activeSearchGroupItemPath,
  activeCriteriaType,
} from './store';

import * as ActionTypes from './actions/types';
import {CohortSearchState, CountScope} from './typings';

export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      case ActionTypes.START_REQUEST:
        return state.update('requests', requestTable => requestTable.add(action.path));

      case ActionTypes.CLEANUP_REQUEST:
        return state.update('requests', requestTable => requestTable.remove(action.path));

      case ActionTypes.LOAD_CRITERIA_RESULTS:
        return state.updateIn(action.path.unshift('criteria'), fromJS(action.results));

      case ActionTypes.LOAD_COUNT_RESULTS: {
        // action.path === [role, groupIndex, groupItemIndex, scope]
        const scope = action.path.last();
        const pathKey = {
          [CountScope.TOTAL]: 'total',
          [CountScope.GROUP]: action.path.skipLast(2),
          [CountScope.ITEM]: action.path.skipLast(1),
        }[scope];
        return state.updateIn(['counts', pathKey], action.count);
      }

      case ActionTypes.INIT_SEARCH_GROUP:
        return state.updateIn(['search', action.sgRole], list => list.push(List()));

      case ActionTypes.INIT_GROUP_ITEM: {
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

      case ActionTypes.SELECT_CRITERIA:
        return state.updateIn(
          activeSearchGroupItemPath(state).push('searchParameters'),
          List(),
          criteria => criteria.push(action.criterion)
        );

      case ActionTypes.REMOVE:
        return state.deleteIn(action.path);

      case ActionTypes.SET_WIZARD_OPEN:
        return state.setIn(['context', 'wizardOpen'], true);

      case ActionTypes.SET_WIZARD_CLOSED:
        return state.setIn(['context', 'wizardOpen'], false);

      case ActionTypes.SET_ACTIVE_CONTEXT:
        return state.mergeIn(['context', 'active'], action.context);

      case ActionTypes.CLEAR_ACTIVE_CONTEXT:
        return state.setIn(['context', 'active'], Map());

      default: return state;
    }
};
