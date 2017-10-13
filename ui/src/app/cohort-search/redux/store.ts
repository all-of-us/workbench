import {Map, List, fromJS, isCollection} from 'immutable';
import {KeyPath} from './actions/types';

import {Criteria, SearchRequest} from 'generated';

/**
 * InitialState:
 *    - search: This holds the data used to generate SearchRequests
 *    - context: Contains a variety of contextual data for the active wizard
 *    - criteria: Models the criteria tree loaded so far
 *    - requests: Table {KeyPath => boolean}; keeps tabs on what requests are in flight
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
  requests: {},
  counts: {},
});


export type CohortSearchState = Map<any, any>;
export type ImmCriteria = Map<keyof Criteria, string|number|boolean>;

/**
 * Selectors: Used to query information from the state
 */

/* Return Paths */
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

export const criteriaPath = (kind: string, parentId: number): KeyPath =>
  List().push('criteria', kind, parentId);

export const pathTo =
  (role: string, index: number, itemIndex?: number): KeyPath =>
  itemIndex 
    ? List().push('search', role, index, 'items', itemIndex)
    : List().push('search', role, index);

/* Return Values */
export const countFor = (objId: string|KeyPath) => (state): number =>
  state.getIn(['counts', objId], objId === 'total' ? 0 : null);

export const isLoading = (path: KeyPath) => (state): boolean =>
  state.getIn(path.unshift('requests'), false);

export const activeCriteriaType = (state): string =>
  state.getIn(['context', 'active', 'criteriaType']);

/**
 * If the path is non-empty and leads to an empty collection,
 * remove that collection and check its parent.
 */
export const prunePath = (path: KeyPath, state) => (
  !path.isEmpty() 
  && isCollection(state.getIn(path)) 
  && state.getIn(path).isEmpty()
    ? prunePath(path.butLast(), state.deleteIn(path))
    : state
);
