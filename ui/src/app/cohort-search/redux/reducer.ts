import {Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';

import {
  initialState,
  CohortSearchState,
  SR_ID,
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
  REMOVE_ITEM,
  REMOVE_GROUP,
  REMOVE_CRITERION,
  SET_WIZARD_OPEN,
  SET_WIZARD_CLOSED,
  SET_ACTIVE_CONTEXT,
  CLEAR_ACTIVE_CONTEXT,
  RESET_STATE,
  RootAction,
} from './actions/types';

/**
 * The root Reducer.  Handles synchronous changes to application State
 */
export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = initialState, action: RootAction): CohortSearchState => {
    switch (action.type) {

      case BEGIN_CRITERIA_REQUEST:
        return state.setIn(['requests', action.kind, action.parentId], true);

      case LOAD_CRITERIA_RESULTS:
        return state
          .setIn(['criteria', action.kind, action.parentId], fromJS(action.results))
          .deleteIn(['requests', action.kind, action.parentId]);

      case CANCEL_CRITERIA_REQUEST:
        return state.deleteIn(['requests', action.kind, action.parentId]);

      case CRITERIA_REQUEST_ERROR:
        return state
          .setIn(
            ['requests', action.kind, action.parentId],
            fromJS({error: action.error})
          );

      case BEGIN_COUNT_REQUEST:
        return state.setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true);

      case LOAD_COUNT_RESULTS:
        return state.mergeIn(
          ['entities', action.entityType, action.entityId],
          fromJS({count: action.count, isRequesting: false})
        );

      case CANCEL_COUNT_REQUEST:
        return state.setIn(['entities', action.entityType, action.entityId, 'isRequesting'], false);

      case COUNT_REQUEST_ERROR:
        return state
          .setIn(
            ['entities', action.entityType, action.entityId, 'isRequesting'],
            fromJS({error: action.error})
          );

      case INIT_SEARCH_GROUP:
        return state
          .setIn(
            ['entities', 'groups', action.groupId],
            fromJS({
              id: action.groupId,
              items: [],
              count: null,
              isRequesting: false
            })
          )
          .updateIn(
            ['entities', 'searchRequests', SR_ID, action.role],
            groupList => groupList.push(action.groupId)
          );

      case INIT_GROUP_ITEM:
        return state
          .setIn(
            ['entities', 'items', action.itemId],
            fromJS({
              id: action.itemId,
              type: state.getIn(['context', 'active', 'criteriaType']),
              searchParameters: [],
              modifiers: [],
              count: null,
              isRequesting: false,
            })
          )
          .updateIn(
            ['entities', 'groups', action.groupId, 'items'],
            itemList => itemList.push(action.itemId)
          );

      case SELECT_CRITERIA:
        return state
          .setIn(['entities', 'criteria', action.criterion.get('id')], action.criterion)
          .updateIn(
            ['entities', 'items', action.itemId, 'searchParameters'],
            List(),
            paramList => paramList.includes(action.criterion.get('id'))
              ? paramList
              : paramList.push(action.criterion.get('id'))
          );

      case REMOVE_ITEM:
        return state
          .updateIn(
            ['entities', 'groups', action.groupId, 'items'],
            List(),
            itemList => itemList.filterNot(id => id === action.itemId)
          )
          .deleteIn(['entities', 'items', action.itemId]);

      case REMOVE_GROUP:
        return state
          .updateIn(
            ['entities', 'searchRequests', SR_ID, action.role],
            List(),
            groupList => groupList.filterNot(id => id === action.groupId)
          )
          .deleteIn(['entities', 'groups', action.groupId]);

      case REMOVE_CRITERION:
        return state
          .updateIn(
            ['entities', 'items', action.itemId, 'searchParameters'],
            List(),
            critList => critList.filterNot(id => id === action.criterionId)
          )
          .deleteIn(['entities', 'criteria', action.criterionId]);

      case SET_WIZARD_OPEN:
        return state.setIn(['context', 'wizardOpen'], true);

      case SET_WIZARD_CLOSED:
        return state.setIn(['context', 'wizardOpen'], false);

      case SET_ACTIVE_CONTEXT:
        return state.mergeIn(['context', 'active'], action.context);

      case CLEAR_ACTIVE_CONTEXT:
        return state.setIn(['context', 'active'], Map());

      case RESET_STATE:
        return action.state;

      default: return state;
    }
};
