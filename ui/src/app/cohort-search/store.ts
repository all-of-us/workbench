import {combineReducers, AnyAction, Reducer} from 'redux';
import {CohortSearchActions as Actions} from './actions';
import {SearchRequest, SubjectListResponse as SubjectList} from 'generated';


export interface CohortSearchState {
  search: SearchRequest;
  subjects?: SubjectList;
}

export const InitialState = {
  search: {include: []}
};

export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: AnyAction): CohortSearchState => {
    switch (action.type) {

      /* Appends a new empty list to the end of whichever seearch group list is
       * specified by action.list */
      case Actions.INIT_SEARCH_GROUP: {
        const key = action.key;
        const newList = [...state.search[key], []];
        return {...state, search: {...state.search, [key]: newList}};
      }

      case Actions.REMOVE_SEARCH_GROUP: {
        const {key, index} = action;
        const newList = [
          ...state.search[key].slice(0, index),
          ...state.search[key].slice(index + 1)
        ];
        return {...state, search: {...state.search, [key]: newList}};
      }

      default: return state;
    }
};
