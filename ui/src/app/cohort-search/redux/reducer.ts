import {TemporalMention, TemporalTime, TreeSubType} from 'generated';
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
  SR_ID
} from './store';

/* tslint:disable:ordered-imports */
import {
  BEGIN_CRITERIA_REQUEST,
  BEGIN_SUBTYPE_CRITERIA_REQUEST,
  BEGIN_ALL_CRITERIA_REQUEST,
  BEGIN_DRUG_CRITERIA_REQUEST,
  LOAD_CRITERIA_RESULTS,
  LOAD_CRITERIA_SUBTYPE_RESULTS,
  LOAD_DEMO_CRITERIA_RESULTS,
  LOAD_CRITERIA_SUBTREE,
  CANCEL_CRITERIA_REQUEST,
  SET_CRITERIA_SEARCH,
  BEGIN_AUTOCOMPLETE_REQUEST,
  CANCEL_AUTOCOMPLETE_REQUEST,
  BEGIN_INGREDIENT_REQUEST,
  LOAD_AUTOCOMPLETE_OPTIONS,
  LOAD_INGREDIENT_LIST,
  LOAD_CHILDREN_LIST,
  SELECT_CHILDREN_LIST,
  LOAD_ATTRIBUTE_LIST,
  AUTOCOMPLETE_REQUEST_ERROR,
  ATTRIBUTE_REQUEST_ERROR,
  CRITERIA_REQUEST_ERROR,
  CHANGE_CODE_OPTION,
  SET_SCROLL_ID,

  BEGIN_COUNT_REQUEST,
  BEGIN_ATTR_PREVIEW_REQUEST,
  LOAD_ATTR_PREVIEW_RESULTS,
  LOAD_COUNT_RESULTS,
  CANCEL_COUNT_REQUEST,
  COUNT_REQUEST_ERROR,
  CLEAR_TOTAL_COUNT,
  CLEAR_GROUP_COUNT,

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
  HIDE_ITEM,
  HIDE_GROUP,
  ENABLE_ENTITY,
  REMOVE_ITEM,
  REMOVE_GROUP,
  SET_ENTITY_TIMEOUT,
  OPEN_WIZARD,
  REOPEN_WIZARD,
  WIZARD_FINISH,
  UPDATE_TEMPORAL,
  UPDATE_WHICH_MENTION,
  UPDATE_TEMPORAL_TIME,
  UPDATE_TEMPORAL_TIME_VALUE,
  WIZARD_CANCEL,
  SET_WIZARD_CONTEXT,
  SHOW_ATTRIBUTES_PAGE,
  HIDE_ATTRIBUTES_PAGE,

  LOAD_ENTITIES,
  RESET_STORE,
  CLEAR_STORE,
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

      case BEGIN_SUBTYPE_CRITERIA_REQUEST:
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
          .setIn(
            ['criteria', 'tree', 'empty', action.kind, action.parentId],
            action.results.length === 0
          )
          .deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case LOAD_CRITERIA_SUBTYPE_RESULTS:
        return state
          .setIn(
            ['criteria', 'tree', action.kind, action.subtype, action.parentId],
            fromJS(action.results)
          )
          .setIn(
            ['criteria', 'tree', 'empty', action.kind, action.parentId],
            action.results.length === 0
          )
          .deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case LOAD_DEMO_CRITERIA_RESULTS:
        return state
          .setIn(['criteria', 'tree', action.kind, action.subtype], action.results)
          .deleteIn(['criteria', 'requests', action.kind, action.subtype]);

      case LOAD_CRITERIA_SUBTREE:
        if (action.subtype !== TreeSubType[TreeSubType.BRAND]) {
          state = state.deleteIn(['criteria', 'search', 'ingredients']);
        }
        return state
          .setIn(['criteria', 'subtree', action.kind], fromJS(action.path))
          .setIn(['criteria', 'subtree', 'selected'], action.ids)
          .deleteIn(['criteria', 'search', 'options'])
          .deleteIn(['criteria', 'requests', action.kind]);

      case CANCEL_CRITERIA_REQUEST:
        return state.deleteIn(['criteria', 'requests', action.kind, action.parentId]);

      case SET_CRITERIA_SEARCH:
        return state.setIn(['criteria', 'search', 'terms'], action.searchTerms);

      case BEGIN_AUTOCOMPLETE_REQUEST:
        return state
          .deleteIn(['criteria', 'search', 'errors'])
          .setIn(['criteria', 'search', 'autocomplete'], true);

      case CANCEL_AUTOCOMPLETE_REQUEST:
        return state.deleteIn(['criteria', 'search', 'autocomplete']);

      case BEGIN_INGREDIENT_REQUEST:
        return state
          .deleteIn(['criteria', 'search', 'errors'])
          .deleteIn(['criteria', 'search', 'options'])
          .setIn(['criteria', 'search', 'autocomplete'], true);

      case LOAD_AUTOCOMPLETE_OPTIONS:
        return state
          .setIn(['criteria', 'search', 'options'], action.options)
          .deleteIn(['criteria', 'search', 'autocomplete']);

      case LOAD_INGREDIENT_LIST:
        return state
          .setIn(['criteria', 'search', 'ingredients'], action.ingredients)
          .deleteIn(['criteria', 'search', 'autocomplete']);

      case LOAD_CHILDREN_LIST:
        action.children.forEach(child => {
          child.parameterId = `param${(child.conceptId ?
            (child.conceptId + child.code) : child.id)}`;
          state = state
            .setIn(['wizard', 'selections', child.parameterId], fromJS(child))
            .updateIn(
              ['wizard', 'item', 'searchParameters'],
              List(),
              paramList => paramList.includes(child.parameterId)
                ? paramList
                : paramList.push(child.parameterId));
        });
        return state;

      case SELECT_CHILDREN_LIST:
        const parentId = action.parentId.toString();
        return state
          .updateIn(
            ['wizard', 'item', 'selectedGroups'],
            List(),
            groupIdList => groupIdList.includes(parentId)
              ? groupIdList
              : groupIdList.push(parentId));

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

      case CHANGE_CODE_OPTION:
        return state
          .deleteIn(['criteria', 'subtree'])
          .deleteIn(['criteria', 'search']);


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
          .setIn(['wizard', 'preview', 'error'], true)
          .setIn(['wizard', 'preview', 'requesting'], false);

      case BEGIN_CHARTS_REQUEST:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true)
          .deleteIn(['entities', action.entityType, action.entityId, 'error'])
          .deleteIn(['entities', action.entityType, action.entityId, 'count'])
          .set('initShowChart', true);

      case BEGIN_COUNT_REQUEST:
        return state
          .setIn(['entities', action.entityType, action.entityId, 'isRequesting'], true)
          .deleteIn(['entities', action.entityType, action.entityId, 'error'])
          .deleteIn(['entities', action.entityType, action.entityId, 'count'])
          .set('initShowChart', true);

      case BEGIN_ATTR_PREVIEW_REQUEST:
        return state
          .deleteIn(['wizard', 'preview', 'error'])
          .deleteIn(['wizard', 'calculate', 'count'])
          .setIn(['wizard', 'preview', 'requesting'], true);

      case LOAD_ATTR_PREVIEW_RESULTS:
        return state
          .setIn(['wizard', 'calculate', 'count'], action.count)
          .setIn(['wizard', 'preview', 'requesting'], false);

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

      case CLEAR_TOTAL_COUNT:
        const temporalActive = state.getIn(['entities', 'groups', action.groupId, 'temporal']);
        if (temporalActive) {
          return state.setIn(['entities', 'searchRequests', SR_ID, 'count'], null);
        } else {
          return state.setIn(['entities', 'searchRequests', SR_ID, 'count'], -1);
        }

      case CLEAR_GROUP_COUNT:
        return state.setIn(['entities', 'groups', action.groupId, 'count'], -1);

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
              temporal: false,
              mention: '',
              time: '',
              timeValue: 0,
              timeFrame: '',
              isRequesting: false,
              status: 'active'
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
            action.parameter.set('status', 'active')
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
          )
          .updateIn(
            ['wizard', 'item', 'selectedGroups'],
            List(),
            groupIdList => groupIdList.filter(id => id !== action.id.toString())
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
        return state
          .setIn(['wizard', 'item', 'attributes', 'node'], Map())
          .deleteIn(['wizard', 'calculate', 'count']);

      case HIDE_ITEM: {
        return state = state.setIn(['entities', 'groups', action.groupId, 'count'], null)
          .setIn(['entities', 'items', action.itemId, 'status'], action.status);
      }

      case HIDE_GROUP:
        return state.setIn(['entities', 'groups', action.groupId, 'status'], action.status);

      case ENABLE_ENTITY: {
        return state.setIn(['entities', action.entity, action.entityId, 'status'], 'active');
      }

      case REMOVE_ITEM: {
        state = state
          .updateIn(
            ['entities', 'groups', action.groupId, 'items'],
            List(),
            itemList => itemList.filterNot(id => id === action.itemId)
          )
          .setIn(['entities', 'items', action.itemId, 'status'], 'deleted');

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
          .setIn(['entities', 'groups', action.groupId, 'status'], 'deleted');

      case OPEN_WIZARD:
        return state.mergeIn(['wizard'], fromJS({
          open: true,
          item: {
            id: action.itemId,
            type: action.itemType,
            fullTree: action.context.fullTree,
            searchParameters: [],
            modifiers: [],
            count: null,
            temporalGroup: action.tempGroup ? action.tempGroup : 0,
            isRequesting: false,
            status: 'active'
          },
          selections: {},
          ...action.context
        }));

      case SET_ENTITY_TIMEOUT:
        return state.setIn(
          ['entities', action.entity, action.entityId, 'timeout'], action.timeoutId
        );

      case REOPEN_WIZARD:
        const selections = state.getIn(['entities', 'parameters'], Map()).filter(
          (_, key) => action.item.get('searchParameters', List()).includes(key)
        );
        return state.mergeIn(['wizard'], Map({
          open: true,
          item: action.item,
          selections: selections,

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
          .set('criteria', Map({tree: {}, requests: {}, errors: {}}));
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
          .set('criteria', Map({tree: {}, requests: {}, errors: {}}));
      }

      case SET_WIZARD_CONTEXT:
        return state
          .mergeDeepIn(['wizard'], action.context)
          .deleteIn(['criteria', 'subtree']);

      case UPDATE_TEMPORAL: {
        const groupItems = ['entities', 'groups', action.groupId, 'temporal'];
        const timeValue = ['entities', 'groups', action.groupId, 'timeValue'];
        const time = ['entities', 'groups', action.groupId, 'time'];
        const mention = ['entities', 'groups', action.groupId, 'mention'];
        const group = getGroup(action.groupId)(state);
        // below if is to set the state in default values
        if (group.get('timeValue') === 0 && group.get('time') === ''
          && group.get('mention') === '') {
          return state
            .setIn(timeValue, '')
            .setIn(time, TemporalTime.DURINGSAMEENCOUNTERAS)
            .setIn(mention, TemporalMention.ANYMENTION)
            .setIn(groupItems, action.flag);
        }
        return state
          .setIn(groupItems, action.flag);
      }

      case UPDATE_WHICH_MENTION: {
        const groupItems = ['entities', 'groups', action.groupId, 'mention'];
        return state.setIn(groupItems, action.mention);
      }

      case UPDATE_TEMPORAL_TIME: {
        const groupItems = ['entities', 'groups', action.groupId, 'time'];
        const timeValue = ['entities', 'groups', action.groupId, 'timeValue'];
        if ((action.time) !== TemporalTime.DURINGSAMEENCOUNTERAS) {
          return state
            .setIn(groupItems, action.time)
            .setIn(timeValue, 1);
        }
        return state
          .setIn(groupItems, action.time)
          .setIn(timeValue, '');
      }

      case UPDATE_TEMPORAL_TIME_VALUE: {
        const groupItems = ['entities', 'groups', action.groupId, 'timeValue'];
        return state.setIn(groupItems, action.timeValue);
      }

      case LOAD_ENTITIES:
        return state.set('entities', action.entities);

      case RESET_STORE:
        return initialState;

      case CLEAR_STORE:
        return fromJS({});
      default: return state;
    }

  };
