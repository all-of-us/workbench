import {Map, List, Set, fromJS} from 'immutable';
import {Criteria, SearchRequest} from 'generated';

/**
 * InitialState
 */
export const SR_ID = 'searchReqest0';

export const initialState = fromJS({
  entities: {
    searchRequests: {
      [SR_ID]: {
        includes: ['include0'],
        excludes: ['exclude0'],
        count: 0,
        isRequesting: false
      }
    },
    groups: {
      include0: {
        id: 'include0',
        items: [],
        count: null,
        isRequesting: false,
      },
      exclude0: {
        id: 'exclude0',
        items: [],
        count: null,
        isRequesting: false,
      },
    },
    items: {},
    criteria: {},
  },

  wizard: {
    open: false,
    item: {},
  },

  criteria: {
    tree: {},
    requests: {},
  },
});


export type CohortSearchState = Map<any, any>;

/**
 * Selectors: Used to query information from the state
 */

/**
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
    critId => state.getIn(['entities', 'criteria', critId], Map()));

export const getItem = itemId => state =>
  state.getIn(['entities', 'items', itemId], Map());

export const getGroup = groupId => state =>
  state.getIn(['entities', 'groups', groupId], Map());

export const getSearchRequest = searchRequestId => state =>
  state.getIn(['entities', 'searchRequests', searchRequestId], Map());

/**
 * Criteria
 */
export const activeCriteriaList = (state): List<any> =>
  parameterList(activeItemId(state))(state);

export const criteriaChildren = (kind, parentId) => state =>
  state.getIn(['criteria', 'tree', kind, parentId], List());

export const isCriteriaLoading =
  (kind: string, parentId: number) =>
  (state): boolean =>
  state.getIn(['criteria', 'requests', kind, parentId]) === true;


/**
 * Generic information
 */
export const countFor = (kind, id) => state =>
  state.getIn(['entities', kind, id, 'count'], null);

export const totalCount = state =>
  countFor('searchRequests', SR_ID)(state);

export const isRequesting = (kind, id) => state =>
  state.getIn(['entities', kind, id, 'isRequesting'], false);

export const isRequstingTotal = state =>
  isRequesting('searchRequests', SR_ID)(state);


/**
 * Get Context
 */
export const wizardOpen = state =>
  state.getIn(['wizard', 'open'], false);

export const activeItemId = (state): string =>
  state.getIn(['wizard', 'active', 'itemId']);

export const activeGroupId = (state): string =>
  state.getIn(['wizard', 'active', 'groupId']);

export const activeRole = (state): keyof SearchRequest =>
  state.getIn(['wizard', 'active', 'role']);

export const activeCriteriaType = (state): string =>
  state.getIn(['wizard', 'active', 'criteriaType']);
