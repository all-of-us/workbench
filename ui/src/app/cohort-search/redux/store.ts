/*
 * TODO (jms): strongly type the store, reducers, etc
 */
import {Map, List, Set, fromJS, isCollection} from 'immutable';
import {SearchRequest} from 'generated';

export const InitialState = fromJS({
  search: {includes: [[]], excludes: [[]]},
  results: Map(),
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  requests: Set(),
});

export type CohortSearchState = Map<string, any>;

// Pathspecs & selectors
export const subjects = ['results', 'subjects'];
export const inclusionGroups = ['search', 'includes'];
export const exclusionGroups = ['search', 'excludes'];
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
