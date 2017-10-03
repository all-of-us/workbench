import {Injectable} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {AnyAction} from 'redux';

import {CohortSearchState} from './store';
import {CohortBuilderService, Criteria, SearchRequest} from 'generated';


@Injectable()
export class CohortSearchActions {

  /** Action type symbols */
  static INIT_SEARCH_GROUP = 'INIT_SEARCH_GROUP';
  static REMOVE_SEARCH_GROUP = 'REMOVE_SEARCH_GROUP';

  static INIT_GROUP_ITEM = 'INIT_GROUP_ITEM';
  static REMOVE_GROUP_ITEM = 'REMOVE_GROUP_ITEM';

  static SET_WIZARD_CONTEXT = 'SET_WIZARD_CONTEXT';
  static OPEN_WIZARD = 'OPEN_WIZARD';
  static FINISH_WIZARD = 'FINISH_WIZARD';
  static CANCEL_WIZARD = 'CANCEL_WIZARD';

  static LOAD_CRITERIA = 'LOAD_CRITERIA';
  static FETCH_CRITERIA = 'FETCH_CRITERIA';
  static SELECT_CRITERIA = 'SELECT_CRITERIA';

  static SET_REQUEST_CONTEXT = 'SET_REQUEST_CONTEXT';
  static FETCH_SEARCH_RESULTS = 'FETCH_SEARCH_RESULTS';
  static LOAD_SEARCH_RESULTS = 'LOAD_SEARCH_RESULTS';

  static ERROR = 'ERROR';

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private cohortBuilderService: CohortBuilderService) {}

  public initGroup(sgRole: keyof SearchRequest): void {
    this.ngRedux.dispatch({type: CohortSearchActions.INIT_SEARCH_GROUP, sgRole});
  }

  public removeGroup(sgRole: keyof SearchRequest, sgIndex: number): void {
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_SEARCH_GROUP, sgRole, sgIndex});
  }

  public removeGroupItem(sgRole: keyof SearchRequest, sgIndex: number, sgItemIndex: number): void {
    this.ngRedux.dispatch({
      type: CohortSearchActions.REMOVE_GROUP_ITEM, sgRole, sgIndex, sgItemIndex
    });
  }

  public openWizard(criteriaType: string, sgRole: keyof SearchRequest, sgIndex: number): void {
    this.ngRedux.dispatch({type: CohortSearchActions.SET_WIZARD_CONTEXT, criteriaType, sgRole, sgIndex});
    this.ngRedux.dispatch({type: CohortSearchActions.OPEN_WIZARD});
    this.ngRedux.dispatch({type: CohortSearchActions.INIT_GROUP_ITEM});
  }

  public finishWizard(): void {
    this.ngRedux.dispatch({type: CohortSearchActions.FINISH_WIZARD});
  }

  public cancelWizard(): void {
    this.ngRedux.dispatch({type: CohortSearchActions.CANCEL_WIZARD});
  }

  public fetchCriteria(critType: string, parentId: number): void {
    critType = critType.toLowerCase();
    this.ngRedux.dispatch({type: CohortSearchActions.FETCH_CRITERIA, critType, parentId});
  }

  public selectCriteria(criteria: Criteria): void {
    this.ngRedux.dispatch({type: CohortSearchActions.SELECT_CRITERIA, criteria});
  }

  /**
   * TODO(jms) this is provisional; waiting on a decision about in-memory sets
   * vs triple queries: the code should be able to go either way
   */
  public fetchSearchResults(contextObj): void {
    this.ngRedux.dispatch({type: CohortSearchActions.SET_REQUEST_CONTEXT, contextObj});
    const store = this.ngRedux.getState();
    const request = this.prepareRequest(store.get('search'));
    this.ngRedux.dispatch({type: CohortSearchActions.FETCH_SEARCH_RESULTS, request});
  }

  public prepareRequest(request): SearchRequest {
    const store = this.ngRedux.getState();

    const context = store.getIn(['context', 'request']);
    const sgRole = context.get('sgRole');
    const sgIndex = context.get('sgIndex');
    const sgItemIndex = context.get('sgItemIndex');
    const sgPath = ['search', sgRole, sgIndex, sgItemIndex];

    const searchGoupItem = store.getIn(sgPath);
    const newRequest = {include: [], exclude: []};

    newRequest[sgRole][0].push(searchGoupItem.toJS());
    return newRequest;
  }
}
