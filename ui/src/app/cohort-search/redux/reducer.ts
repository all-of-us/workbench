import {fromJS, List, Map, Set} from 'immutable';
import {Reducer} from 'redux';

import {
  activeGroupId,
  activeItem,
  activeParameterList,
  activeRole,
  CohortSearchState,
  getGroup,
  initialState,
  SR_ID,
} from './store';

/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  BEGIN_DRUG_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  LOAD_DEMO_CRITERIA_RESULTS,
  LOAD_CRITERIA_SUBTREE,
  CANCEL_CRITERIA_REQUEST,
  SET_CRITERIA_SEARCH,
  BEGIN_AUTOCOMPLETE_REQUEST,
  BEGIN_INGREDIENT_REQUEST,
  LOAD_AUTOCOMPLETE_OPTIONS,
  CLEAR_AUTOCOMPLETE_OPTIONS,
  LOAD_INGREDIENT_LIST,
  LOAD_ATTRIBUTE_LIST,
  AUTOCOMPLETE_REQUEST_ERROR,
  ATTRIBUTE_REQUEST_ERROR,
  CRITERIA_REQUEST_ERROR,
  SET_SCROLL_ID,

  BEGIN_COUNT_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,
  LOAD_ATTR_PREVIEW_RESULTS,
  ADD_ATTR_FOR_PREVIEW,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,

  BEGIN_PREVIEW_REQUEST,
  LOAD_PREVIEW_RESULTS,
  PREVIEW_REQUEST_ERROR,

  BEGIN_CHARTS_REQUEST,
  LOAD_CHARTS_RESULTS,
  CANCEL_CHARTS_REQUEST,
  CHARTS_REQUEST_ERROR,

  INIT_SEARCH_GROUP,
  ADD_PARAMETER,
  REMOVE_PARAMETER,
  ADD_MODIFIER,
  REMOVE_MODIFIER,
  SET_WIZARD_FOCUS,
  CLEAR_WIZARD_FOCUS,
  REMOVE_ITEM,
  REMOVE_GROUP,
  OPEN_WIZARD,
  REOPEN_WIZARD,
  WIZARD_FINISH,
  WIZARD_CANCEL,
  SET_WIZARD_CONTEXT,
  SHOW_ATTRIBUTES_PAGE,
  HIDE_ATTRIBUTES_PAGE,

  LOAD_ENTITIES,
  RESET_STORE,
  RootAction,
} from './actions/types';
/* tslint:enable:ordered-imports */

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

      case BEGIN_ALL_CRITERIA_REQUEST:
        return state
          .deleteIn(['criteria', 'errors', List([action.kind, action.parentId])])
          .setIn(['criteria', 'requests', action.kind, action.parentId], true);

      case BEGIN_DRUG_CRITERIA_REQUEST:
        return state
          .deleteIn(['criteria', 'errors', List([action.kind, action.parentId])])
          .setIn(['criteria', 'requests', action.kind, action.parentId], true);

      case LOAD_CRITERIA_RESULTS:
        return state
          .setIn(['criteria', 'tree', action.kind, action.parentId], fromJS(action.results))
          .deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case LOAD_DEMO_CRITERIA_RESULTS:
        return state
          .setIn(['criteria', 'tree', action.kind, action.subtype], action.results)
          .deleteIn(['criteria', 'requests', action.kind, action.subtype]);

      case LOAD_CRITERIA_SUBTREE:
        return state
          .setIn(['criteria', 'subtree', action.kind], fromJS(action.path))
          .setIn(['criteria', 'subtree', 'selected'], action.ids)
          .deleteIn(['criteria', 'requests', action.kind]);

      case CANCEL_CRITERIA_REQUEST:
        return state.deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case SET_CRITERIA_SEARCH:
        return state.setIn(['criteria', 'search', 'terms'], action.searchTerms);

      case BEGIN_AUTOCOMPLETE_REQUEST:
        return state
          .deleteIn(['criteria', 'search', 'errors'])
          .setIn(['criteria', 'search', 'autocomplete'], true);

      case BEGIN_INGREDIENT_REQUEST:
        return state
          .deleteIn(['criteria', 'search', 'errors'])
          .setIn(['criteria', 'search', 'autocomplete'], true);

      case LOAD_AUTOCOMPLETE_OPTIONS:
        return state
          .setIn(['criteria', 'search', 'options'], action.options)
          .deleteIn(['criteria', 'search', 'autocomplete']);

      case CLEAR_AUTOCOMPLETE_OPTIONS:
        return state
          .deleteIn(['criteria', 'search', 'options']);

      case LOAD_INGREDIENT_LIST:
        return state
          .setIn(['criteria', 'search', 'ingredients'], action.ingredients)
          .deleteIn(['criteria', 'search', 'autocomplete']);

      case LOAD_ATTRIBUTE_LIST:
        const node = action.node.set('attributes', action.attributes);
        return state
          .setIn(['wizard', 'item', 'attributes', 'node'], node)
          .deleteIn(['wizard', 'item', 'attributes', 'loading']);

      case AUTOCOMPLETE_REQUEST_ERROR:
        return state
          .deleteIn(['criteria', 'search', 'autocomplete'])
          .setIn(['criteria', 'search', 'errors'], fromJS({error: action.error}));

      case ATTRIBUTE_REQUEST_ERROR:
        return state
          .deleteIn(['wizard', 'item', 'attributes', 'loading'])
          .setIn(
            ['wizard', 'item', 'attributes', 'errors'], fromJS({error: action.error})
          );

      case CRITERIA_REQUEST_ERROR:
        return state
          .deleteIn(['criteria', 'requests', action.kind, action.parentId])
          .setIn(
            ['criteria', 'errors', List([action.kind, action.parentId])],
            fromJS({error: action.error})
          );

      case SET_SCROLL_ID:
        return state.setIn(['criteria', 'tree', 'scroll'], action.nodeId);

      case BEGIN_PREVIEW_REQUEST:
        return state
          .deleteIn(['wizard', 'preview', 'error'])
          .setIn(['wizard', 'preview', 'requesting'], true);

      case LOAD_PREVIEW_RESULTS:
        return state
          .setIn(['wizard', 'preview', 'count'], action.count)
          .setIn(['wizard', 'preview', 'requesting'], false);

      case PREVIEW_REQUEST_ERROR:
        return state
          .setIn(['wizard', 'preview', 'error'], action.error)
          .setIn(['wizard', 'preview', 'requesting'], false);

      case BEGIN_CHARTS_REQUEST:
        return state
            .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true)
            .set('initShowChart', true);

      case BEGIN_COUNT_REQUEST:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true)
          .deleteIn(['entities', action.entityType, action.entityId, 'error'])
          .set('initShowChart', true);

      case BEGIN_ATTR_PREVIEW_REQUEST:
        return state
          .deleteIn(['wizard', 'calculate', 'error'])
          .setIn(['wizard', 'calculate', 'requesting'], true);

      case LOAD_ATTR_PREVIEW_RESULTS:
        return state
          .setIn(['wizard', 'calculate', 'count'], action.count)
          .setIn(['wizard', 'calculate', 'requesting'], false);

      case ADD_ATTR_FOR_PREVIEW:
        return state.setIn(['wizard', 'count', 'parameters'], fromJS([action.parameter]));

      case CANCEL_CHARTS_REQUEST:
      case CANCEL_COUNT_REQUEST:
        return state.setIn(['entities', action.entityType, action.entityId, 'isRequesting'], false);

      case CHARTS_REQUEST_ERROR:
      case COUNT_REQUEST_ERROR:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], false)
          .setIn(
            ['entities', action.entityType, action.entityId, 'error'],
            fromJS({error: action.error})
          );

      case LOAD_CHARTS_RESULTS:
        return state
          .set('chartData', fromJS(action.chartData))
          .mergeIn(
            ['entities', action.entityType, action.entityId],
            fromJS({
              count: action.chartData.reduce((sum, data) => sum + data.count, 0),
              isRequesting: false,
            })
          );

      case LOAD_COUNT_RESULTS:
        return state
          .mergeIn(
            ['entities', action.entityType, action.entityId],
            fromJS({count: action.count, isRequesting: false})
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

      case ADD_PARAMETER:
        return state
          .setIn(
            ['wizard', 'selections', action.parameter.get('parameterId')],
            action.parameter
          )
          .updateIn(
            ['wizard', 'item', 'searchParameters'],
            List(),
            paramList => paramList.includes(action.parameter.get('parameterId'))
              ? paramList
              : paramList.push(action.parameter.get('parameterId'))
          );

      case REMOVE_PARAMETER:
        return state
          .deleteIn(['wizard', 'selections', action.parameterId])
          .updateIn(
            ['wizard', 'item', 'searchParameters'],
            List(),
            paramList => paramList.filterNot(id => id === action.parameterId)
          );

      case ADD_MODIFIER:
        return state
          .updateIn(
            ['wizard', 'item', 'modifiers'],
            List(),
            mods => (mods.includes(fromJS(action.modifier))
              ? mods
              : mods.push(fromJS(action.modifier)))
          );

      case REMOVE_MODIFIER:
        return state
          .updateIn(
            ['wizard', 'item', 'modifiers'],
            List(),
            mods => mods.filter(item => item !== fromJS(action.modifier))
          );

      case SET_WIZARD_FOCUS:
        return state.setIn(['wizard', 'focused'], action.criterion);

      case CLEAR_WIZARD_FOCUS:
        return state.setIn(['wizard', 'focused'], Map());

      case SHOW_ATTRIBUTES_PAGE:
        return state
          .setIn(['wizard', 'item', 'attributes', 'loading'], true);

      case HIDE_ATTRIBUTES_PAGE:
        console.log('hide');
        return state
          .setIn(['wizard', 'item', 'attributes', 'node'], Map())
          .deleteIn(['wizard', 'calculate', 'count']);

        case REMOVE_ITEM: {
        state = state
          .updateIn(
            ['entities', 'groups', action.groupId, 'items'],
            List(),
            itemList => itemList.filterNot(id => id === action.itemId)
          )
          .deleteIn(['entities', 'items', action.itemId]);

        const paramsInUse = state
          .getIn(['entities', 'items'], Map())
          .reduce(
            (ids, item) => ids.union(item.get('searchParameters', List())),
            Set()
          );

        return state.updateIn(
          ['entities', 'parameters'], Map(),
          params => params.filter((_, key) => paramsInUse.has(key))
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
            fullTree: action.context.fullTree,
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
          selections: state.getIn(['entities', 'parameters'], Map()).filter(
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

        const mergeParams = (parameter) =>
          activeParameterList(state).reduce(
            (paramset, param) => paramset.set(param.get('parameterId'), param),
            parameter
          );

        return state
          .updateIn(groupItems, List(), setUnique(itemId))
          .setIn(['entities', 'items', itemId], item)
          .updateIn(['entities', 'parameters'], Map(), mergeParams)
          .set('wizard', Map({open: false}))
          .deleteIn(['criteria', 'subtree'])
          .deleteIn(['criteria', 'search']);
      }

      case WIZARD_CANCEL: {
        const groupId = activeGroupId(state);
        const group = getGroup(groupId)(state);
        const count = group.get('items').size;
        const role = activeRole(state);
        if (count === 0) {
          state = state.deleteIn(['entities', 'groups', groupId]);
          const index = state.getIn(['entities', 'searchRequests', SR_ID, role]).indexOf(groupId);
          if (index > -1) {
            state = state.deleteIn(['entities', 'searchRequests', SR_ID, role, index]);
          }
        }
        return state
          .set('wizard', Map({open: false}))
          .deleteIn(['criteria', 'subtree'])
          .deleteIn(['criteria', 'search']);
      }

      case SET_WIZARD_CONTEXT:
        return state.mergeDeepIn(['wizard'], action.context);

      case LOAD_ENTITIES:
        return state.set('entities', action.entities);

      case RESET_STORE:
        return initialState;

      default: return state;
    }
};
