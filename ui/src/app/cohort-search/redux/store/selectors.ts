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

export const getTemporalGroupItems = (groupId) => (state) => {
  const itemObj = {
    nonTemporalItems: [],
    temporalItems: [],
    type: []
  };
  const isTemporal = state.getIn(['entities', 'groups', groupId, 'temporal']);
  state.getIn(['entities', 'groups', groupId, 'items'], List()).forEach(itemId => {
    const item = state.getIn(['entities', 'items', itemId], Map());
    if (item.get('temporalGroup') === 1 && isTemporal) {
      itemObj.temporalItems.push(item);
    } else {
      itemObj.nonTemporalItems.push(item);
      itemObj.type.push(item.get('type'));
    }
  });
  return itemObj;
};

export const parameterList = itemId => state =>
  state.getIn(['entities', 'items', itemId, 'searchParameters'], List()).map(
    critId => state.getIn(['entities', 'parameters', critId], Map()));

export const getItem = itemId => state =>
  state.getIn(['entities', 'items', itemId], Map());

export const getTempItem = itemId => state =>
  state.getIn(['entities', 'items', itemId, 'temporalGroup'], false);

export const getGroup = groupId => state =>
  state.getIn(['entities', 'groups', groupId], Map());

export const getSearchRequest = searchRequestId => state =>
  state.getIn(['entities', 'searchRequests', searchRequestId], Map());

export const searchRequestError = (state): boolean =>
  state.getIn(['entities', 'searchRequests', SR_ID, 'error'], false);

export const groupError = (groupId) => (state): boolean =>
  state.getIn(['entities', 'groups', groupId, 'error'], false);

export const itemError = (itemId) => (state): boolean =>
  state.getIn(['entities', 'items', itemId, 'error'], false);

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

export const activeCriteriaSubtype = (state): string =>
  state.getIn(['wizard', 'criteriaSubtype']);

export const codeDropdownOptions = (state): List<any> =>
  state.getIn(['wizard', 'codes'], List());

export const activeCriteriaTreeType = (state): string =>
state.getIn(['wizard', 'fullTree']);

export const activeRole = (state): keyof SearchRequest =>
  state.getIn(['wizard', 'role']);

export const activeGroupId = (state): string =>
  state.getIn(['wizard', 'groupId']);

export const activeParameterList = (state): List<any> =>
  state
    .getIn(['wizard', 'item', 'searchParameters'], List())
    .map(id => state.getIn(['wizard', 'selections', id]));

export const activeModifierList = (state): List<any> =>
  state.getIn(['wizard', 'item', 'modifiers'], List());

export const isParameterActive = paramId => (state): boolean =>
  state
    .getIn(['wizard', 'item', 'searchParameters'], List())
    .includes(paramId);

export const selectedGroups = (state): List<any> =>
  state.getIn(['wizard', 'item', 'selectedGroups'], List());

export const activeItem = (state) =>
  state.getIn(['wizard', 'item'], Map());

export const focusedCriterion = (state) =>
  state.getIn(['wizard', 'focused'], Map());

export const previewStatus = (state) =>
  state.getIn(['wizard', 'preview'], Map());

export const attributesPreviewStatus = (state) =>
  state.getIn(['wizard', 'calculate'], Map());

export const previewError = (state): boolean =>
  state.getIn(['wizard', 'preview', 'error'], false);

export const nodeAttributes = (state): any =>
    state.getIn(['wizard', 'item', 'attributes', 'node'], Map());

export const isAttributeLoading = (state): boolean =>
    state.getIn(['wizard', 'preview', 'requesting'], false);

export const participantsCount = (state): any =>
  state.getIn(['wizard', 'item', 'count'], false);

export const isDomainNameExists = (cid: any, domain: string) => (state): boolean => {
  const domainExists = state.getIn(['reviewChartData', 'domainCharts']).has(cid);
  return domainExists ?
    state.getIn(['reviewChartData', 'domainCharts', cid]).has(domain) : false;
};
/**
 * Criteria
 */
export const criteriaChildren =
  (kind: string, subtype: string, parentId: number) =>
  (state): List<any> => {
    if (subtype) {
      return state.getIn(['criteria', 'tree', kind, subtype, parentId], List());
    } else {
      return state.getIn(['criteria', 'tree', kind, parentId], List());
    }
  };

export const ppiAnswers = (path) => (state) => {
  const ppiPath = path.split('.');
  const [grandParent, parent] = ppiPath.splice(ppiPath.length - 2);
  return state.getIn(['criteria', 'tree', 'PPI', parseInt(grandParent, 10)],
    List()).find( i => i.get('id') === parseInt(parent, 10));
};

export const isEmpty =
  (kind: string, id: number) => (state): boolean =>
    state.getIn(['criteria', 'tree', 'empty', kind, id], false);

export const demoCriteriaChildren =
  (kind: string, subtype: string) =>
  (state): List<any> =>
  state.getIn(['criteria', 'tree', kind, subtype], List());

export const criteriaSearchTerms =
  () => (state): Array<string> =>
  state.getIn(['criteria', 'search', 'terms'], null);

export const criteriaSubtree =
  (kind: string) => (state): List<any> =>
  state.getIn(['criteria', 'subtree', kind], List());

export const isCriteriaLoading =
  (kind: string, parentId: number) =>
  (state): boolean =>
  state.getIn(['criteria', 'requests', kind, parentId], false);

export const autocompleteOptions =
  () => (state): Array<any> =>
  state.getIn(['criteria', 'search', 'options'], null);

export const ingredientsForBrand =
  () => (state): Array<any> =>
  state.getIn(['criteria', 'search', 'ingredients'], []);

export const isAutocompleteLoading =
  () => (state): boolean =>
  state.getIn(['criteria', 'search', 'autocomplete'], false);

export const autocompleteError =
  () => (state): any =>
  state.getIn(['criteria', 'search', 'error']);

export const criteriaError =
  (kind: string, parentId: number) =>
  (state): any =>
  state.getIn(['criteria', 'errors', List([kind, parentId])]);

export const criteriaLoadErrors =
  (state): any =>
  state.getIn(['criteria', 'errors'], Map());

export const subtreeSelected = (state) =>
  state.getIn(['criteria', 'subtree', 'selected'], null);

export const scrollId = (state) =>
  state.getIn(['criteria', 'tree', 'scroll'], null);

/**
 * Other
 */
export const chartData =
  (state): List<any> =>
  state.get('chartData', List());

