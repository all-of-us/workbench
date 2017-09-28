import {Injectable} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {AnyAction} from 'redux';

import {CohortSearchState} from './store';


type ListType = 'include' | 'exclude';


@Injectable()
export class CohortSearchActions {
  static INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
  static REMOVE_SEARCH_GROUP = 'REMOVE_SEARCH_GROUP';

  constructor(private ngRedux: NgRedux<CohortSearchState>) {}

  initGroup(key: ListType): AnyAction {
    return {type: CohortSearchActions.INIT_SEARCH_GROUP, key};
  }

  removeGroup(key: ListType, index: number): void {
    // using dispatch lets us dispatch *multiple* actions from the one action creator
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_SEARCH_GROUP, key, index});
  }
}
