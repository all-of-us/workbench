import {List, Map} from 'immutable';

import {SR_ID} from './initial';

import {SearchRequest} from 'generated';



/**
 * Entities
 * Resolve a list of ID's (or the id of a thing with a list of ids) to a list
 * of things
 */
export const groupList = kind => (state): List<any> =>
  state.getIn(['entities', 'searchRequests', SR_ID, kind], List()).map(
    groupId => state.getIn(['entities', 'groups', groupId], Map()));

export const includeGroups = groupList('includes');
export const excludeGroups = groupList('excludes');

export const itemList = groupId => state =>
  state.getIn(['entities', 'groups', groupId, 'items'], List()).map(itemId =>
    state.getIn(['entities', 'items', itemId], Map()));

export const parameterList = itemId => state =>
  state.getIn(['entities', 'items', itemId, 'searchParameters'], List()).map(
    critId => state.getIn(['entities', 'parameters', critId], Map()));

export const getItem = itemId => state =>
  state.getIn(['entities', 'items', itemId], Map());

export const getGroup = groupId => state =>
  state.getIn(['entities', 'groups', groupId], Map());

export const getSearchRequest = searchRequestId => state =>
  state.getIn(['entities', 'searchRequests', searchRequestId], Map());

export const countFor = (kind, id) => state =>
  state.getIn(['entities', kind, id, 'count'], null);

export const totalCount = state =>
  countFor('searchRequests', SR_ID)(state);

export const isRequesting = (kind, id) => state =>
  state.getIn(['entities', kind, id, 'isRequesting'], false);

export const isRequstingTotal = state =>
  isRequesting('searchRequests', SR_ID)(state);

/**
 * Wizard
 */
export const wizardOpen = (state): boolean =>
  state.getIn(['wizard', 'open'], false);

export const activeCriteriaType = (state): string =>
  state.getIn(['wizard', 'criteriaType']);

export const activeRole = (state): keyof SearchRequest =>
  state.getIn(['wizard', 'role']);

export const activeGroupId = (state): string =>
  state.getIn(['wizard', 'groupId']);

export const activeParameterList = (state): List<any> =>
  state
    .getIn(['wizard', 'item', 'searchParameters'], List())
    .map(id => state.getIn(['wizard', 'selections', id]));

export const isParameterActive = paramId => (state): boolean =>
  state
    .getIn(['wizard', 'item', 'searchParameters'], List())
    .includes(paramId);

export const activeItem = (state) =>
  state.getIn(['wizard', 'item'], Map());

export const focusedCriterion = (state) =>
  state.getIn(['wizard', 'focused'], Map());

/**
 * Criteria
 */
export const criteriaChildren =
  (kind: string, parentId: number) =>
  (state): List<any> =>
  state.getIn(['criteria', 'tree', kind, parentId], List());

export const isCriteriaLoading =
  (kind: string, parentId: number) =>
  (state): boolean =>
  state.getIn(['criteria', 'requests', kind, parentId], false);

export const criteriaError =
  (kind: string, parentId: number) =>
  (state): any =>
  state.getIn(['criteria', 'errors', List([kind, parentId])]);

export const criteriaLoadErrors =
  (state): any =>
  state.getIn(['criteria', 'errors'], Map());

/**
 * Other
 */
export const chartData =
  (state): List<any> =>
  state.get('chartData', List());
