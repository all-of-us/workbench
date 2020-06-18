import {Component, HostListener, ViewChild} from '@angular/core';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';

import {CBModal} from 'app/cohort-search/modal/modal.component';
import {ListOverview} from 'app/cohort-search/overview/overview.component';
import {SearchGroupList} from 'app/cohort-search/search-group-list/search-group-list.component';
import {idsInUse, searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest, parseCohortDefinition} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, queryParamsStore} from 'app/utils/navigation';
import {Cohort, SearchRequest} from 'generated/fetch';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

interface State {
  loading: boolean;
  overview: boolean;
  criteria: SearchRequest;
  updateCount: number;
  cohort: Cohort;
  modalPromise: Promise<boolean> | null;
  modalOpen: boolean;
  updatingCohort: boolean;
  updateGroupListsCount: number;
  cohortChanged: boolean;
  searchContext: any;
}

export class CohortSearch extends React.Component<{}, State> {

  @ViewChild('wrapper') wrapper;

  private subscription;
  resolve: Function;

  constructor(props: any) {
    super(props);
    this.state = {
      loading: false,
      overview: false,
      criteria: {includes: [], excludes: [], dataFilters: []},
      updateCount: 0,
      cohort: undefined,
      modalPromise:  null,
      modalOpen: false,
      updatingCohort: false,
      updateGroupListsCount: 0,
      cohortChanged: false,
      searchContext: undefined
    };
  }

  componentDidMount() {
    this.subscription = Observable.combineLatest(
      queryParamsStore, currentWorkspaceStore
    ).subscribe(([params, workspace]) => {
      /* If a cohort id is given in the route, we initialize state with
       * it */
      const cohortId = params.cohortId;
      if (cohortId) {
        this.setState({loading: true});
        cohortsApi().getCohort(workspace.namespace, workspace.id, cohortId)
          .then(cohort => {
            this.setState({cohort, loading: false});
            currentCohortStore.next(cohort);
            if (cohort.criteria) {
              searchRequestStore.next(parseCohortDefinition(cohort.criteria));
            }
          });
      } else {
        this.setState({cohort: {criteria: `{'includes':[],'excludes':[],'dataFilters':[]}`, name: '', type: ''}});
      }
    });

    this.subscription.add(searchRequestStore.subscribe(sr => {
      this.setState({
        criteria: sr,
        overview: sr.includes.length > 0 || sr.excludes.length > 0,
        cohortChanged: !!this.state.cohort && this.state.cohort.criteria !== JSON.stringify(mapRequest(sr)),
        updateGroupListsCount: this.state.updateGroupListsCount + 1
      });
    }));
    // this.updateWrapperDimensions();
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
    idsInUse.next(new Set());
    currentCohortStore.next(undefined);
    searchRequestStore.next({includes: [], excludes: [], dataFilters: []} as SearchRequest);
  }

  canDeactivate(): Promise<boolean> | boolean {
    return !this.state.cohortChanged || this.state.updatingCohort || this.showWarningModal();
  }

  async showWarningModal() {
    this.setState({modalOpen: true});
    return await new Promise<boolean>((resolve => this.resolve = resolve));
  }

  getModalResponse(res: boolean) {
    this.setState({modalOpen: false});
    this.resolve(res);
  }

  // @HostListener('window:resize')
  // onResize() {
  //   this.updateWrapperDimensions();
  // }
  //
  // updateWrapperDimensions() {
  //   const wrapper = this.wrapper.nativeElement;
  //
  //   const {top} = wrapper.getBoundingClientRect();
  //   wrapper.style.minHeight = pixel(window.innerHeight - top - ONE_REM);
  // }

  updateRequest = () => {
    // timeout prevents Angular 'Expression changed after checked' error
    setTimeout(() => this.setState({updateCount: this.state.updateCount + 1}));
  }

  render() {
    const {cohort, cohortChanged, criteria, loading, modalOpen, overview, searchContext, updateCount, updateGroupListsCount} = this.state;
    return <React.Fragment>
      <div id='wrapper' className='cohort-search-wrapper'>
      <div className='row'>
        <div className='col-xl-8 col-lg-12'>
          <div className='row'>
            <div className='col-xs-12' style={{height: '1.5rem'}}>
              {!!cohort && <span className='cohort-name'>{cohort.name}</span>}
            </div>
            <div className='col-xl-6 col-lg-12' id='list-include-groups'>
              <SearchGroupList groups={criteria.includes}
                               setSearchContext={(c) => this.setState({searchContext: c})}
                               role='includes'
                               updated={updateGroupListsCount}
                               updateRequest={() => this.updateRequest()}/>
              </div>
              <div className='col-xl-6 col-lg-12' id='list-exclude-groups'>
                {overview && <SearchGroupList groups={criteria.excludes}
                                 setSearchContext={(c) => this.setState({searchContext: c})}
                                 role='excludes'
                                 updated={updateGroupListsCount}
                                 updateRequest={() => this.updateRequest()}/>}
              </div>
            </div>
          </div>
          <div className='col-xl-4 col-lg-12' id='list-charts'>
            {overview && <ListOverview
              cohort={cohort}
              cohortChanged={cohortChanged}
              searchRequest={criteria}
              updateCount={updateCount}
              updating={() => this.setState({updatingCohort: true})}/>}
          </div>
              {loading && <div className='spinner root-spinner'>
            Loading...
          </div>}
        </div>
      </div>
      {searchContext && <CBModal
        closeSearch={() => this.setState({searchContext: undefined})}
        searchContext={searchContext}
        setSearchContext={(c) => this.setState({searchContext: c})}/>}
      {modalOpen && <Modal>
        <ModalTitle>Warning! </ModalTitle>
        <ModalBody>
          Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL
          and {cohort && cohort.id ? 'use Save or Save As' : 'click CREATE COHORT'} to save your criteria.
        </ModalBody>
        <div className='modal-footer'>
          <Button type='link' onClick={() => this.getModalResponse(false)}>Cancel</Button>
          <Button type='primary' onClick={() => this.getModalResponse(true)}>Discard Changes</Button>
        </div>
      </Modal>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-cohort-search',
  template: '<div #root></div>'
})
export class CohortSearchComponent extends ReactWrapperBase {
  constructor() {
    super(CohortSearch, []);
  }
}
