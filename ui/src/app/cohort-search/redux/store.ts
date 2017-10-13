import {Map, List, Set, fromJS} from 'immutable';
import {KeyPath} from './actions/types';

import {Criteria, SearchRequest} from 'generated';

/**
 * InitialState:
 *    - search: This holds the data used to generate SearchRequests
 *    - context: Contains a variety of contextual data for the active wizard
 *    - criteria: Models the criteria tree loaded so far
 *    - requests: Table {KeyPath => boolean}; keeps tabs on what requests are in flight
 *      * Implemented as a Set, not a Map
 *    - counts: Table {KeyPath => number} tracking entity counts
 */
export const initialState = fromJS({
  search: {
    includes: [
      {items: []},
    ],
    excludes: [
      {items: []},
    ],
  },
  context: {
    wizardOpen: false,
    activePath: [],
  },
  criteria: {},
  requests: Set(),
  counts: {},
});


export type CohortSearchState = Map<any, any>;
export type ImmCriteria = Map<keyof Criteria, string|number|boolean>;

/**
 * Selectors: Used to query information from the state
 */

export const activeSearchGroupPath = (state): KeyPath =>
  List().push(
    'search',
    state.getIn(['context', 'active', 'role']),
    state.getIn(['context', 'active', 'groupIndex'])
  );

export const activeSearchGroupItemPath = (state): KeyPath =>
  activeSearchGroupPath(state).push(
    'items',
    state.getIn(['context', 'active', 'groupItemIndex'])
  );

export const activeCriteriaType = (state): string =>
  state.getIn(['context', 'active', 'criteriaType']);

export const countFor = (objId: string|KeyPath) => (state): number =>
  state.getIn(['counts', objId], objId === 'total' ? 0 : null);

export const criteriaPath = (kind: string, parentId: number): KeyPath =>
  List().push('criteria', kind, parentId);

export const isLoading = (path: KeyPath) => (state): boolean =>
  state.get('requests').has(path);
