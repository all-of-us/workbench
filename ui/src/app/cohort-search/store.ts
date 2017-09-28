import {combineReducers, Action, Reducer} from 'redux';
import {SearchRequest, SubjectListResponse as SubjectList} from 'generated';


export interface CohortSearchState {
  search: SearchRequest;
  subjects?: SubjectList;
}

export const InitialState = {
  search: {include: []}
};

export const rootReducer: Reducer<CohortSearchState> =
  (state: CohortSearchState = InitialState, action: Action): CohortSearchState => {
    switch (action.type) {
      default: return state;
    }
};
