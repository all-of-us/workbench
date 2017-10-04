import {Map, List, Set, fromJS} from 'immutable';

/*
 * NOTE (jms)
 * immutable.js simplifies the reducer internals significantly,
 * and is nice in its own right, *however* it does come with some
 * overhead.  Currently we're storing each result set in the `results`
 * tree, however if this becomes too heavy on memory there are several
 * options we've discussed:
 *
 *    - abandon the idea of keeping the results in memory and grab the
 *      counts per group item, per group, and for the toal set from the API
 *    - build out a service to handle the result data at a much lower level
 *      while presenting an immutable face (metadata) to the store
 *
 * TODO (jms) implement immutable.js compatible types and strongly type the
 * store, the reducers, the actions, all of it
 */
export const InitialState = fromJS({
  search: {include: [[]], exclude: [[]]},
  results: {include: [[]], exlude: [[]]},
  context: {
    wizardOpen: false,
  },
  criteriaTree: {},
  loading: {},
});

export type CohortSearchState = Map<string, any>;

// Pathspecs & selectors
export const inclusionGroups = ['search', 'include'];
export const exclusionGroups = ['search', 'exclude'];
export const wizardOpen = ['context', 'wizardOpen'];
export const activeCriteriaType = ['context', 'active', 'criteriaType'];
export const activeSGRole = ['context', 'active', 'sgRole'];
export const activeSGIndex = ['context', 'active', 'sgIndex'];
export const activeSGItemIndex = ['context', 'active', 'sgItemIndex'];

// The actual, not symbolic path
// NOTE: the returned path does NOT include the search | results prefix
// or a criteria index suffix
export const getActiveSGIPath = (state) => List([
  state.getIn(activeSGRole),
  state.getIn(activeSGIndex),
  state.getIn(activeSGItemIndex),
]);

// TODO (jms) memoize this by state.results so it isn't recomputed
// with *every* single change to the state
export const subjects = (state) => {
  const unions = (role) => (
    state
      .getIn(['results', role], List())
      .map(group => Set.union(group.map(item => item.get('subjects', Set()))))
  );
  const include = Set.intersect(unions('include'));
  const exclude = Set.intersect(unions('exclude'));
  return include.subtract(exclude);
};
