import {AnyAction, Reducer} from 'redux';
import {Map, List, Set, fromJS} from 'immutable';
import {
  CohortSearchState,
  InitialState,
  getActiveSGIPath,
  activeCriteriaType,
  activeSGRole,
  activeSGIndex,
  activeSGItemIndex,
} from './store';
import {CohortSearchActions as Actions} from './actions';

/**
 * the Root Reducer
 * Performs pure (and ideally very simple, atomic) transformations
 * of the application state
 */
export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      case Actions.INIT_SEARCH_GROUP: {
        return state.updateIn(['search', action.sgRole], list => list.push(List()));
      }

      case Actions.REMOVE_GROUP_ITEM:
      case Actions.REMOVE_SEARCH_GROUP:
      case Actions.REMOVE_CRITERIA: {
        return state.deleteIn(action.path.unshift('search'));
      }

      case Actions.INIT_GROUP_ITEM: {
        const criteriaType = state.getIn(activeCriteriaType);
        const sgRole = state.getIn(activeSGRole);
        const sgIndex = state.getIn(activeSGIndex);
        const newItem = Map({
          type: criteriaType,
          searchParameters: List(),
          modifiers: List(),
        });
        const sgPath = ['search', sgRole, sgIndex];
        return state.updateIn(sgPath, group => group.push(newItem))
          .setIn(activeSGItemIndex, state.getIn(sgPath).size);
      }

      case Actions.SET_WIZARD_CONTEXT: {
        const {criteriaType, sgIndex, sgRole} = action;
        const newContext = Map({criteriaType, sgIndex, sgRole});
        return state.mergeIn(['context', 'active'], newContext);
      }

      case Actions.OPEN_WIZARD: {
        return state.setIn(['context', 'wizardOpen'], true);
      }

      case Actions.FINISH_WIZARD: {
        return state.setIn(['context', 'wizardOpen'], false);
      }

      case Actions.CANCEL_WIZARD: {
        const path = getActiveSGIPath(state);
        return state
          .deleteIn(path.unshift('search'))
          .setIn(['context', 'wizardOpen'], false);
      }

      case Actions.FETCH_CRITERIA: {
        const {critType: _type, parentId} = action;
        return state.setIn(['loading', _type, parentId], true);
      }

      case Actions.LOAD_CRITERIA: {
        const {children, critType: _type, parentId} = action;
        state = state
          .setIn(['criteriaTree', _type, parentId], children)
          .deleteIn(['loading', _type, parentId]);
        if (state.getIn(['loading', _type]).isEmpty()) {
          state = state.deleteIn(['loading', _type]);
        }
        return state;
      }

      case Actions.SELECT_CRITERIA: {
        const path = getActiveSGIPath(state).unshift('search').push('searchParameters');
        return state.updateIn(path, List(), critlist => critlist.push(action.criteria));
      }

        /* Annotates the search tree with results
         * NOTE: as of now this only loads results for a SearchGroupItem
         */
      case Actions.LOAD_SEARCH_RESULTS: {
        const result = Set(action.results);
        const path = action.sgiPath.unshift('search');
        return state
          .setIn(path.push('count'), result.size)
          .setIn(path.push('subjects'), result);
      }

      /* NOTE: this would normally be a prime candidate for selectors, given that the
       * count for any given group, group item, or the total are pure functions of the
       * size and members of each subcohort.  However, these numbers are needed in enough
       * places and expensive enough to calculate that we do them all once, whenever new
       * results are populated.
       */
      case Actions.RECALCULATE_COUNTS: {
        return state;
      }

      case Actions.ERROR: {
        // TODO (jms)
        return state;
      }

      default: return state;
    }
};
