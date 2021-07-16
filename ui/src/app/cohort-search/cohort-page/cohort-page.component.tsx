import * as fp from 'lodash/fp';
import * as React from 'react';

import {CohortSearch} from 'app/cohort-search/cohort-search/cohort-search.component';
import {ListOverview} from 'app/cohort-search/overview/overview.component';
import {SearchGroupList} from 'app/cohort-search/search-group-list/search-group-list.component';
import {idsInUse, searchRequestStore} from 'app/cohort-search/search-state.service';
import {mapRequest, parseCohortDefinition} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {FlexRowWrap} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {LOCAL_STORAGE_KEY_COHORT_CONTEXT} from 'app/pages/data/criteria-search';
import {cohortsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortSearchContext,
  withCurrentWorkspace
} from 'app/utils';
import {
  currentCohortSearchContextStore,
  currentCohortStore,
  queryParamsStore
} from 'app/utils/navigation';
import {navigationGuardStore} from 'app/utils/stores';
import {WorkspaceData} from 'app/utils/workspace-data';
import {Cohort, SearchRequest} from 'generated/fetch';

const LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST = 'CURRENT_COHORT_SEARCH_REQUEST';

const styles = reactStyles({
  cohortError: {
    background: colors.warning,
    color: colors.white,
    padding: '0.25rem 0.5rem',
    borderRadius: '5px',
    marginBottom: '0.5rem'
  }
});

function colStyle(percentage: string) {
  return {
    flex: `0 0 ${percentage}%`,
    maxWidth: `${percentage}%`,
    minHeight: '1px',
    padding: '0 0.5rem',
    position: 'relative',
    width: '100%'
  } as React.CSSProperties;
}

interface Props extends WithSpinnerOverlayProps {
  cohortContext: any;
  workspace: WorkspaceData;
}

interface State {
  loading: boolean;
  overview: boolean;
  criteria: SearchRequest;
  updateCount: number;
  cohort: Cohort;
  cohortError: boolean;
  minHeight: string;
  modalPromise: Promise<boolean> | null;
  modalOpen: boolean;
  updateGroupListsCount: number;
  cohortChanged: boolean;
  updatingCohort: boolean;
  unsavedSelections: boolean;
  searchContext: any;
}

export const CohortPage = fp.flow(withCurrentWorkspace(), withCurrentCohortSearchContext()) (
  class extends React.Component<Props, State> {
    private subscription;
    resolve: Function;

    constructor(props: any) {
      super(props);
      this.state = {
        loading: false,
        overview: false,
        criteria: {dataFilters: [], includes: [], excludes: []},
        updateCount: 0,
        cohort: undefined,
        cohortError: false,
        minHeight: '10rem',
        modalPromise:  null,
        modalOpen: false,
        updateGroupListsCount: 0,
        cohortChanged: false,
        updatingCohort: false,
        unsavedSelections: false,
        searchContext: undefined
      };
      this.showWarningModal = this.showWarningModal.bind(this);
    }

    componentDidMount() {
      const {workspace: {id}, hideSpinner} = this.props;
      hideSpinner();
      this.subscription = queryParamsStore.subscribe(params => this.initCohort(params.cohortId));
      this.subscription.add(searchRequestStore.subscribe(searchRequest => {
        const {cohort} = this.state;
        const cohortChanged = !!cohort && cohort.criteria !== JSON.stringify(mapRequest(searchRequest));
        this.setState({
          criteria: searchRequest,
          overview: searchRequest.includes.length > 0 || searchRequest.excludes.length > 0,
          cohortChanged,
          updateGroupListsCount: this.state.updateGroupListsCount + 1
        });
        const localStorageCohort = {
          workspaceId: id,
          cohortId: !!cohort ? cohort.id : null,
          searchRequest
        };
        localStorage.setItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST, JSON.stringify(localStorageCohort));
      }));
      navigationGuardStore.set({component: this});
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (prevProps.cohortContext && !this.props.cohortContext) {
        this.setState({unsavedSelections: false});
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
      idsInUse.next(new Set());
      currentCohortStore.next(undefined);
      currentCohortSearchContextStore.next(undefined);
      searchRequestStore.next({includes: [], excludes: [], dataFilters: []} as SearchRequest);
      localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST);
      navigationGuardStore.set(null);
    }

    initCohort(cid: number) {
      const {workspace: {id, namespace}} = this.props;
      const existingCohort = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST));
      const existingContext = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT));
      /* If a cohort id is given in the route, we initialize state with it */
      if (cid) {
        this.setState({loading: true});
        cohortsApi().getCohort(namespace, id, cid).then(cohort => {
          this.setState({cohort, loading: false});
          currentCohortStore.next(cohort);
          if (existingCohort && existingCohort.workspaceId === id && existingCohort.cohortId === +cid) {
            searchRequestStore.next(existingCohort.searchRequest);
          } else if (cohort.criteria) {
            searchRequestStore.next(parseCohortDefinition(cohort.criteria));
          }
        }).catch(error => {
          console.error(error);
          this.setState({cohortError: true, loading: false});
        });
      } else {
        this.setState(
          {cohort: {criteria: `{'includes':[],'excludes':[],'dataFilters':[]}`, name: '', type: ''}},
          () => {
            if (existingCohort && existingCohort.workspaceId === id && !existingCohort.cohortId) {
              searchRequestStore.next(existingCohort.searchRequest);
            } else {
              localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST);
            }
          }
        );
      }
      if (existingContext) {
        const {workspaceId, cohortId, cohortContext} = existingContext;
        if (workspaceId === id && ((!cid && !cohortId) || +cid === cohortId)) {
          this.setSearchContext(cohortContext);
        } else {
          localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT);
        }
      }
    }

    async showWarningModal() {
      this.setState({modalOpen: true});
      return await new Promise<boolean>((resolve => this.resolve = resolve));
    }

    canDeactivate(): Promise<boolean> | boolean {
      return !(this.state.cohortChanged || this.state.unsavedSelections) || this.state.updatingCohort || this.showWarningModal();
    }

    getModalResponse(res: boolean) {
      this.setState({modalOpen: false});
      this.resolve(res);
    }

    updateRequest = () => {
      // timeout prevents Angular 'Expression changed after checked' error
      setTimeout(() => this.setState({updateCount: this.state.updateCount + 1}));
    }

    setSearchContext(context: any) {
      context.source = 'cohort';
      currentCohortSearchContextStore.next(context);
      this.setState({searchContext: context});
    }

    get showCohortSearch() {
      const {cohortContext} = this.props;
      return !!cohortContext && cohortContext.source === 'cohort';
    }

    render() {
      const {cohort, cohortChanged, cohortError, criteria, loading, modalOpen, overview, updateCount, updateGroupListsCount} = this.state;
      return <React.Fragment>
        <div style={{minHeight: '28rem', padding: '0.5rem'}}>
          {cohortError
            ? <div style={styles.cohortError}>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={22} />
              Sorry, the cohort could not be loaded. Please try again or contact Support in the left hand navigation.
            </div>
            : <React.Fragment>
              <FlexRowWrap style={{margin: '1rem 0 2rem', ...(this.showCohortSearch ? {display: 'none'} : {})}}>
                <div style={colStyle('66.66667')}>
                  <FlexRowWrap style={{margin: '0 -0.5rem'}}>
                    {!!cohort && !!cohort.name && <div style={{height: '1.5rem', padding: '0 0.5rem', width: '100%'}}>
                      <h3 style={{marginTop: 0}}>{cohort.name}</h3>
                    </div>}
                    <div id='list-include-groups' style={colStyle('50')}>
                      <SearchGroupList groups={criteria.includes}
                                       setSearchContext={(c) => this.setSearchContext(c)}
                                       role='includes'
                                       updated={updateGroupListsCount}
                                       updateRequest={() => this.updateRequest()}/>
                    </div>
                    <div id='list-exclude-groups' style={colStyle('50')}>
                      {overview && <SearchGroupList groups={criteria.excludes}
                                                    setSearchContext={(c) => this.setSearchContext(c)}
                                                    role='excludes'
                                                    updated={updateGroupListsCount}
                                                    updateRequest={() => this.updateRequest()}/>}
                    </div>
                  </FlexRowWrap>
                </div>
                <div style={colStyle('33.33333')}>
                  {overview && <ListOverview
                      cohort={cohort}
                      cohortChanged={cohortChanged}
                      searchRequest={criteria}
                      updateCount={updateCount}
                      updating={() => this.setState({updatingCohort: true})}/>}
                </div>
                {loading && <SpinnerOverlay/>}
              </FlexRowWrap>
              {this.showCohortSearch && <CohortSearch setUnsavedChanges={(unsaved) => this.setState({unsavedSelections: unsaved})}/>}
            </React.Fragment>
          }
        </div>
        {modalOpen && <Modal>
          <ModalTitle>Warning! </ModalTitle>
          <ModalBody>
            Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL
            and {this.showCohortSearch ? 'save your changes in the right sidebar.' :
            cohort && cohort.id ? 'use Save or Save As' : 'click CREATE COHORT'} to save your criteria.
          </ModalBody>
          <ModalFooter>
            <Button type='link' onClick={() => this.getModalResponse(false)}>Cancel</Button>
            <Button type='primary' onClick={() => this.getModalResponse(true)}>Discard Changes</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  }
);
