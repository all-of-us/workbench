import {Injectable} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {AnyAction} from 'redux';
import {List} from 'immutable';

import {environment} from 'environments/environment';
import {CohortSearchState, getActiveSGIPath} from './store';
import {
  Criteria,
  CohortBuilderService,
  SearchRequest,
  SearchParameter,
  SearchGroup
} from 'generated';


@Injectable()
export class CohortSearchActions {

  /* Action type symbols */
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
  static REMOVE_CRITERIA = 'REMOVE_CRITERIA';

  static FETCH_SEARCH_RESULTS = 'FETCH_SEARCH_RESULTS';
  static REMOVE_SEARCH_RESULTS = 'REMOVE_SEARCH_RESULTS';
  static LOAD_SEARCH_RESULTS = 'LOAD_SEARCH_RESULTS';

  static RECALCULATE_COUNTS = 'RECALCULATE_COUNTS';
  static ERROR = 'ERROR';
  static CANCEL_FETCH = 'CANCEL_FETCH';
  static POST_CANCEL_FETCH = 'POST_CANCEL_FETCH';

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private cohortBuilderService: CohortBuilderService) {}

  /* Action creators
   *
   * These functions are responsible for actually dispatching actions to the redux store;
   * they are the interface by which components alter application state
   */
  public initGroup(sgRole: keyof SearchRequest): void {
    this.ngRedux.dispatch({type: CohortSearchActions.INIT_SEARCH_GROUP, sgRole});
  }

  public removeGroup(sgRole: keyof SearchRequest, sgIndex: number): void {
    const path = List([sgRole, sgIndex]);
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_SEARCH_GROUP, path});
  }

  public removeGroupItem(sgRole: keyof SearchRequest, sgIndex: number, sgItemIndex: number): void {
    const path = List().push(sgRole, sgIndex, sgItemIndex);
    if (this.ngRedux.getState().getIn(path.unshift('loading'), false)) {
      this.ngRedux.dispatch({type: CohortSearchActions.CANCEL_FETCH, path});
    }
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_GROUP_ITEM, path});
  }

  public openWizard(critType: string, sgRole: keyof SearchRequest, sgIndex: number): void {
    this.ngRedux.dispatch({
      type: CohortSearchActions.SET_WIZARD_CONTEXT,
      critType, sgRole, sgIndex
    });
    this.ngRedux.dispatch({type: CohortSearchActions.OPEN_WIZARD});
    this.ngRedux.dispatch({type: CohortSearchActions.INIT_GROUP_ITEM});
  }

  public finishWizard(): void {
    const sgiPath = getActiveSGIPath(this.ngRedux.getState());
    this.fetchSearchResults(sgiPath);
    this.ngRedux.dispatch({type: CohortSearchActions.FINISH_WIZARD});
  }

  public cancelWizard(): void {
    this.ngRedux.dispatch({type: CohortSearchActions.CANCEL_WIZARD});
  }

  public fetchCriteria(critType: string, parentId: number): void {
    /* Don't reload already loaded criteria subtrees */
    if (this.ngRedux.getState().getIn(['criteriaTree', critType, parentId])) {
      return;
    }
    this.ngRedux.dispatch({type: CohortSearchActions.FETCH_CRITERIA, critType, parentId});
  }

  public selectCriteria(criteria: Criteria): void {
    this.ngRedux.dispatch({type: CohortSearchActions.SELECT_CRITERIA, criteria});
  }

  public removeCriteria(path): void {
    this.ngRedux.dispatch({type: CohortSearchActions.REMOVE_CRITERIA, path});
  }

  /*
   * TODO(jms) the rest of these functions are provisional; waiting on a
   * decision about in-memory sets vs triple queries: the code should be able
   * to go either way. As is, they only handle SearchGroupItems, just as before
   */
  public fetchSearchResults(sgiPath): void {
    const request = this.prepareRequest(sgiPath);
    this.ngRedux.dispatch({type: CohortSearchActions.FETCH_SEARCH_RESULTS, request, sgiPath});
  }

  public prepareRequest(sgiPath): SearchRequest {
    const store = this.ngRedux.getState();
    let searchGoupItem = store.getIn(sgiPath.unshift('search'));

    const asICD = param => (<SearchParameter>{
      value: param.code, domain: param.domainId
    });

    const asDEMO = param => (<SearchParameter>{
      value: param.code, domain: param.type, conceptId: param.conceptId
    });

    /* TODO(jms) more flexible solution that handles all the different codes */
    const mapper = param => param.type.match(/^DEMO.*/i)
      ? asDEMO(param)
      : asICD(param);

    searchGoupItem = searchGoupItem
      .update('searchParameters', (params) => params.map(mapper))
      .update('type', _type => _type.toUpperCase());

    const role = sgiPath.first();

    const newRequest = {
      [role]: [
        {items: [searchGoupItem.toJS()]}
      ]
    };

    if (environment.debug) {
      console.log(`Created a new Request:`); console.dir(newRequest);
    }
    return newRequest;
  }
}
