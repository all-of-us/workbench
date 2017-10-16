import {Map, List, Set, fromJS, isCollection, is} from 'immutable';
import {KeyPath, CountRequestKind} from './actions/types';

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
  },
  criteria: {},
  requests: {
    total: false,
    group: Set(),
    item: Set(),
    criteria: Set(),
  },
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
  (role: string, index: number, itemIndex?: number, criterionIndex?: number): KeyPath => {
    let rval = List().push('search', role, index);
    if (typeof itemIndex === 'number') {
      rval = rval.push('items', itemIndex);
      if (typeof criterionIndex === 'number') {
        rval = rval.push('searchParameters', criterionIndex);
      }
    }
    return rval;
  };

/* Return Values */
export const countFor = (objId: string|KeyPath) => (state): number =>
  state.getIn(['counts', objId], objId === 'total' ? 0 : null);

export const isRequesting =
  (kind: 'criteria' | CountRequestKind, path?: KeyPath) =>
  (state): boolean =>
    kind === 'total'
      ? state.getIn(['requests', 'total'], false)
      : state.getIn(['requests', kind], Set()).has(path);

export const activeCriteriaType = (state): string =>
  state.getIn(['context', 'active', 'criteriaType']);

/**
 * State / Store Utilities
 */
/**
 * If the path is non-empty and leads to an empty collection,
 * remove that collection and check its parent.  If `halt` is given
 * and present in the KeyPath, do not prune beyond `halt`.
 */
export const prunePath = (path: KeyPath, state, halt?: any) => (
  !path.isEmpty()
  && !is(path.last(), halt)
  && isCollection(state.getIn(path))
  && state.getIn(path).isEmpty()
    ? prunePath(path.butLast(), state.deleteIn(path))
    : state
);
