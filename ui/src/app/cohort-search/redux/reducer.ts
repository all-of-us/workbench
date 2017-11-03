import {Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';

import {
  initialState,
  CohortSearchState,
  SR_ID,
  activeCriteriaType,
  activeGroupId,
  activeItem,
  activeCriteriaList,
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
  UNSELECT_CRITERIA,
  REMOVE_ITEM,
  REMOVE_GROUP,
  OPEN_WIZARD,
  REOPEN_WIZARD,
  WIZARD_FINISH,
  WIZARD_CANCEL,
  SET_WIZARD_CONTEXT,
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
          .deleteIn(['criteria', 'errors', List([action.kind, action.parentId])])
          .setIn(['criteria', 'requests', action.kind, action.parentId], true);

      case LOAD_CRITERIA_RESULTS:
        return state
          .setIn(['criteria', 'tree', action.kind, action.parentId], fromJS(action.results))
          .deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case CANCEL_CRITERIA_REQUEST:
        return state.deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case CRITERIA_REQUEST_ERROR:
        return state
          .deleteIn(['criteria', 'requests', action.kind, action.parentId])
          .setIn(
            ['criteria', 'errors', List([action.kind, action.parentId])],
            fromJS({error: action.error})
          );

      case BEGIN_COUNT_REQUEST:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true)
          .deleteIn(['entities', action.entityType, action.entityId, 'error']);

      case LOAD_COUNT_RESULTS:
        return state.mergeIn(
          ['entities', action.entityType, action.entityId],
          fromJS({count: action.count, isRequesting: false})
        );

      case CANCEL_COUNT_REQUEST:
        return state.setIn(['entities', action.entityType, action.entityId, 'isRequesting'], false);

      case COUNT_REQUEST_ERROR:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], false)
          .setIn(
            ['entities', action.entityType, action.entityId, 'error'],
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

      case SELECT_CRITERIA:
        return state
          .setIn(
            ['wizard', 'selections', action.criterion.get('id')],
            action.criterion
          )
          .updateIn(
            ['wizard', 'item', 'searchParameters'],
            List(),
            paramList => paramList.includes(action.criterion.get('id'))
              ? paramList
              : paramList.push(action.criterion.get('id'))
          );

      case UNSELECT_CRITERIA:
        return state
          .deleteIn(['wizard', 'selections', action.criterionId || action.criterion.get('id')])
          .updateIn(
            ['wizard', 'item', 'searchParameters'],
            List(),
            paramList => paramList.filterNot(id =>
              id === (action.criterionId || action.criterion.get('id'))
            )
          );

      case REMOVE_ITEM: {
        state = state
          .updateIn(
            ['entities', 'groups', action.groupId, 'items'],
            List(),
            itemList => itemList.filterNot(id => id === action.itemId)
          )
          .deleteIn(['entities', 'items', action.itemId]);

        const critsInUse = state
          .getIn(['entities', 'items'], Map())
          .reduce(
            (ids, item) => ids.union(item.get('searchParameters', List())),
            Set()
          );

        return state.updateIn(
          ['entities', 'criteria'], Map(),
          critMap => critMap.filter((_, key) => critsInUse.has(key))
        );
      }

      case REMOVE_GROUP:
        return state
          .updateIn(
            ['entities', 'searchRequests', SR_ID, action.role],
            List(),
            groupList => groupList.filterNot(id => id === action.groupId)
          )
          .deleteIn(['entities', 'groups', action.groupId]);

      case OPEN_WIZARD:
        return state.mergeIn(['wizard'], fromJS({
          open: true,
          item: {
            id: action.itemId,
            type: action.context.criteriaType,
            searchParameters: [],
            modifiers: [],
            count: null,
            isRequesting: false,
          },
          selections: {},
          ...action.context
        }));

      case REOPEN_WIZARD:
        return state.mergeIn(['wizard'], Map({
          open: true,
          item: action.item,
          selections: state.getIn(['entities', 'criteria'], Map()).filter(
            (_, key) => action.item.get('searchParameters', List()).includes(key)
          ),
          ...action.context
        }));

      case WIZARD_FINISH: {
        const item = activeItem(state);
        const itemId = item.get('id');
        const groupId = activeGroupId(state);
        const groupItems = ['entities', 'groups', groupId, 'items'];

        if (item.get('searchParameters', List()).isEmpty()) {
          return state
            .updateIn(groupItems, List(),
              items => items.filterNot(
                id => id === itemId)
            )
            .deleteIn(['entities', 'items', itemId])
            .set('wizard', Map({open: false}));
        }

        const setUnique = element => list =>
          list.includes(element) ? list : list.push(element);

        const mergeCriteria = (criteria) =>
          activeCriteriaList(state).reduce(
            (critset, crit) => critset.set(crit.get('id'), crit),
            criteria
          );

        return state
          .updateIn(groupItems, List(), setUnique(itemId))
          .setIn(['entities', 'items', itemId], item)
          .updateIn(['entities', 'criteria'], Map(), mergeCriteria)
          .set('wizard', Map({open: false}));
      }

      case WIZARD_CANCEL:
        return state.set('wizard', Map({open: false}));

      case SET_WIZARD_CONTEXT:
        return state.mergeDeepIn(['wizard'], action.context);

      default: return state;
    }
};
