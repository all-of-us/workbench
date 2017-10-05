/*
 * TODO (jms): strongly type the store, reducers, etc
 */
import {Map, List, Set, fromJS} from 'immutable';
import {SearchRequest} from 'generated';

export const InitialState = fromJS({
  search: {include: [[]], exclude: [[]]},
  results: Map(),
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  loading: {},
});

export type CohortSearchState = Map<string, any>;

// Pathspecs & selectors
export const subjects = ['results', 'subjects'];
export const inclusionGroups = ['search', 'include'];
export const exclusionGroups = ['search', 'exclude'];
export const wizardOpen = ['context', 'wizardOpen'];
export const activeCriteriaType = ['context', 'active', 'critType'];
export const activeSGRole = ['context', 'active', 'sgRole'];
export const activeSGIndex = ['context', 'active', 'sgIndex'];
export const activeSGItemIndex = ['context', 'active', 'sgItemIndex'];

// The actual, not symbolic path
// export type SGIPath = [keyof SearchRequest, number, number];
export const getActiveSGIPath = (state) => List([
  state.getIn(activeSGRole),
  state.getIn(activeSGIndex),
  state.getIn(activeSGItemIndex),
]);
