import {Injectable} from '@angular/core';
import {NgRedux, dispatch} from '@angular-redux/store';
import {AnyAction} from 'redux';
import {List} from 'immutable';

import {environment} from 'environments/environment';

import {
  CohortSearchState,
  activeSearchGroupItemPath
} from '../store';
import {KeyPath} from './types';
import * as ActionFuncs from './creators';

import {
  Criteria,
  CohortBuilderService,
  SearchRequest,
  SearchParameter,
  SearchGroup
} from 'generated';


@Injectable()
export class CohortSearchActions {

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private service: CohortBuilderService) {}

  /*
   * Auto-dispatched action creators:
   * We wrap the bare action creators in dispatch here so that we
   * (A) provide a unified action dispatching interface to components and
   * (B) can easily perform multi-step, complex actions from this service
   */
  @dispatch() startRequest = ActionFuncs.startRequest;
  @dispatch() cancelRequest = ActionFuncs.cancelRequest;
  @dispatch() cleanupRequest = ActionFuncs.cleanupRequest;

  @dispatch() requestCriteria = ActionFuncs.requestCriteria;
  @dispatch() loadCriteriaRequestResults = ActionFuncs.loadCriteriaRequestResults;
  @dispatch() requestCounts = ActionFuncs.requestCounts;
  @dispatch() loadCountRequestResults = ActionFuncs.loadCountRequestResults;

  @dispatch() initGroup = ActionFuncs.initGroup;
  @dispatch() initGroupItem = ActionFuncs.initGroupItem;
  @dispatch() remove = ActionFuncs.remove;
  @dispatch() selectCriteria = ActionFuncs.selectCriteria;

  @dispatch() setWizardOpen = ActionFuncs.setWizardOpen;
  @dispatch() setWizardClosed = ActionFuncs.setWizardClosed;
  @dispatch() setActiveContext = ActionFuncs.setActiveContext;
  @dispatch() clearActiveContext = ActionFuncs.clearActiveContext;

  /* Higher order actions - actions composed of other actions or providing
   * alternate interfaces for a simpler action.
   */

  // TODO(jms)  removals affect in-flight count requests: implement that affect
  removeGroup(role: keyof SearchRequest, groupIndex: number): void {
    this.remove(List(['search', role, groupIndex]));
  }

  removeGroupItem(role: keyof SearchRequest, groupIndex: number, groupItemIndex: number): void {
    const path = List().push(role, groupIndex, groupItemIndex);
    const state = this.ngRedux.getState();
    const isloading = state.get('requests').has(path);
    if (isloading) {
      this.cancelRequest(path);
    }
    this.remove(path.unshift('search'));
  }

  removeCriterion(
    role: keyof SearchRequest,
    groupIndex: number,
    groupItemIndex: number,
    criterionIndex: number,
  ): void {
    this.remove(List().push(
      'search',
      role,
      groupIndex,
      'items',
      groupItemIndex,
      'searchParameters',
      criterionIndex
    ));
  }

  openWizard(criteriaType: string, role: keyof SearchRequest, groupIndex: number): void {
    this.setActiveContext({criteriaType, role, groupIndex});
    this.setWizardOpen();
    this.initGroupItem(role, groupIndex);
  }

  finishWizard(): void {
    const path = activeSearchGroupItemPath(this.ngRedux.getState());

    this.getCounts(path, 'ITEM');
    this.getCounts(path, 'GROUP');
    this.getCounts(path, 'TOTAL');

    this.clearActiveContext();
    this.setWizardClosed();
  }

  getCounts = (path: KeyPath, scope: string) => {
    const request = this.prepareSearchRequest(path, scope);
    this.startRequest(path.push(scope));
    this.requestCounts(path.push(scope), request);
  }

  cancelWizard(): void {
    const path = activeSearchGroupItemPath(this.ngRedux.getState());
    this.remove(path);
    this.clearActiveContext();
    this.setWizardClosed();
  }

  fetchCriteria(kind: string, parentId: number): void {
    /* Don't reload already loaded criteria subtrees */
    if (this.ngRedux.getState().getIn(['criteria', kind, parentId])) {
      return;
    }
    this.startRequest(List([kind, parentId]));
    this.requestCriteria(kind, parentId);
  }

  prepareSearchRequest(sgiPath, scope): SearchRequest {
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
