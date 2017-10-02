import {Injectable} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {AnyAction} from 'redux';

import {CohortSearchState, RequestKey} from './store';
import {CohortBuilderService, Criteria} from 'generated';


@Injectable()
export class CohortSearchActions {
  static INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
  static REMOVE_SEARCH_GROUP = 'REMOVE_SEARCH_GROUP';
  static REMOVE_GROUP_ITEM = 'REMOVE_GROUP_ITEM';
  static OPEN_WIZARD = 'OPEN_WIZARD';
  static CLOSE_WIZARD = 'CLOSE_WIZARD';
  static LOAD_CRITERIA = 'LOAD_CRITERIA';
  static FETCH_CRITERIA = 'FETCH_CRITERIA';
  static SELECT_CRITERIA = 'SELECT_CRITERIA';
  static ERROR = 'ERROR';

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private cohortBuilderService: CohortBuilderService) {}

  public initGroup(key: RequestKey): void {
    this.ngRedux.dispatch({type: CohortSearchActions.INIT_SEARCH_GROUP, key});
  }

  public removeGroup(key: RequestKey, index: number): void {
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_SEARCH_GROUP, key, index});
  }

  public removeGroupItem(key: RequestKey, groupIndex: number, itemIndex: number): void {
    this.ngRedux.dispatch({
      type: CohortSearchActions.REMOVE_GROUP_ITEM, key, groupIndex, itemIndex
    });
  }

  public openWizard(criteria: string): void {
    this.ngRedux.dispatch({type: CohortSearchActions.OPEN_WIZARD, criteria});
  }

  public closeWizard(): void {
    this.ngRedux.dispatch({type: CohortSearchActions.CLOSE_WIZARD});
  }

  public fetchCriteria(critType: string, parentId: number): void {
    critType = critType.toLowerCase();
    this.ngRedux.dispatch({type: CohortSearchActions.FETCH_CRITERIA, critType, parentId});
  }

  /**
   * Pushes a SearchParameter into the active SearchGroupItem
   */
  public selectCriteria(criteria: Criteria) {
    this.ngRedux.dispatch({type: CohortSearchActions.SELECT_CRITERIA, criteria});
  }
}
