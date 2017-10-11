import {Map, List, Set, fromJS, isCollection} from 'immutable';
import {SearchRequest} from 'generated';

export const InitialState = fromJS({
  // This holds the data used to generate SearchRequests
  search: {
    includes: [[]], 
    excludes: [[]],
  },
  // Contains a variety of contextual data for the active wizard
  context: {
    wizardOpen: false,
    activePath: [],
  },
  // Models the criteria tree loaded so far
  criteria: {},
  // Table of KeyPath => boolean keeping tabs on what requests are in flight
  requests: Set(),
  // Table of KeyPath => number tracking entity counts
  counts: Set(),
});

export const activeSearchGroupPath = (state) =>
  List().push(
    'search',
    state.getIn(['context', 'active', 'role']),
    state.getIn(['context', 'active', 'groupIndex'])
  );

export const activeSearchGroupItemPath = (state) =>
  activeSearchGroupPath(state).push(
    'items',
    state.getIn(['context', 'active', 'groupItemIndex'])
  );

export const activeCriteriaType = (state) =>
  state.getIn(['context', 'active', 'criteriaType']);
