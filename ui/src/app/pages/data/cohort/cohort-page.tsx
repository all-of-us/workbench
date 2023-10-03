import * as React from 'react';
import { Prompt, RouteComponentProps, withRouter } from 'react-router';
import * as fp from 'lodash/fp';

import { Cohort, CohortDefinition } from 'generated/fetch';

import { parseQueryParams } from 'app/components/app-router';
import { FlexRowWrap } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { CohortSearch } from 'app/pages/data/cohort/cohort-search';
import { ListOverview } from 'app/pages/data/cohort/overview';
import { SearchGroupList } from 'app/pages/data/cohort/search-group-list';
import {
  idsInUse,
  searchRequestStore,
} from 'app/pages/data/cohort/search-state.service';
import { mapRequest, parseCohortDefinition } from 'app/pages/data/cohort/utils';
import { LOCAL_STORAGE_KEY_COHORT_CONTEXT } from 'app/pages/data/criteria-search';
import { cohortsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  hasNewValidProps,
  reactStyles,
  withCurrentCohortSearchContext,
  withCurrentWorkspace,
} from 'app/utils';
import {
  currentCohortSearchContextStore,
  currentCohortStore,
  currentGroupCountsStore,
} from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

const LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST = 'CURRENT_COHORT_SEARCH_REQUEST';

const styles = reactStyles({
  cohortError: {
    background: colors.warning,
    color: colors.white,
    padding: '0.375rem 0.75rem',
    borderRadius: '5px',
    marginBottom: '0.75rem',
  },
});

const colStyle = (percentage: string) => {
  return {
    flex: `0 0 ${percentage}%`,
    maxWidth: `${percentage}%`,
    minHeight: '1px',
    padding: '0 0.75rem',
    position: 'relative',
    width: '100%',
  } as React.CSSProperties;
};

const clearCohort = () => {
  idsInUse.next(new Set());
  currentCohortStore.next(undefined);
  currentCohortSearchContextStore.next(undefined);
  currentGroupCountsStore.next([]);
  searchRequestStore.next({
    includes: [],
    excludes: [],
    dataFilters: [],
  } as CohortDefinition);
  localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST);
};

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {
  cohortContext: any;
  workspace: WorkspaceData;
}

interface State {
  cohort: Cohort;
  cohortChanged: boolean;
  cohortError: boolean;
  criteria: CohortDefinition;
  loading: boolean;
  minHeight: string;
  overview: boolean;
  searchContext: any;
  unsavedSelections: boolean;
  updateCount: number;
  updateGroupListsCount: number;
  userClickedSaveRequest: boolean;
}

export const CohortPage = fp.flow(
  withCurrentWorkspace(),
  withCurrentCohortSearchContext(),
  withRouter
)(
  class extends React.Component<Props, State> {
    private subscription;
    resolve: Function;

    constructor(props: any) {
      super(props);
      this.state = {
        cohort: undefined,
        cohortChanged: false,
        cohortError: false,
        criteria: { dataFilters: [], includes: [], excludes: [] },
        loading: false,
        minHeight: '15rem',
        overview: false,
        searchContext: undefined,
        unsavedSelections: false,
        updateCount: 0,
        updateGroupListsCount: 0,
        userClickedSaveRequest: false,
      };
    }

    componentDidMount() {
      const {
        workspace: { id },
        hideSpinner,
      } = this.props;
      hideSpinner();
      this.initCohort();
      this.subscription = searchRequestStore.subscribe((searchRequest) => {
        const { cohort } = this.state;
        const cohortChanged =
          !!cohort &&
          cohort.criteria !== JSON.stringify(mapRequest(searchRequest));
        this.setState({
          criteria: searchRequest,
          overview:
            searchRequest.includes.length > 0 ||
            searchRequest.excludes.length > 0,
          cohortChanged,
          updateGroupListsCount: this.state.updateGroupListsCount + 1,
        });
        const localStorageCohort = {
          workspaceId: id,
          cohortId: !!cohort ? cohort.id : null,
          searchRequest,
        };
        localStorage.setItem(
          LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST,
          JSON.stringify(localStorageCohort)
        );
      });
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (prevProps.cohortContext && !this.props.cohortContext) {
        this.setState({ unsavedSelections: false });
      }
      if (hasNewValidProps(this.props, prevProps, [(p) => p.location.search])) {
        this.initCohort();
      }
    }

    componentWillUnmount() {
      clearCohort();
      this.subscription.unsubscribe();
    }

    initCohort() {
      const {
        workspace: { id, namespace },
      } = this.props;
      const cid = parseQueryParams(this.props.location.search).get('cohortId');
      const existingCohort = JSON.parse(
        localStorage.getItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST)
      );
      const existingContext = JSON.parse(
        localStorage.getItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT)
      );
      /* If a cohort id is given in the route, we initialize state with it */
      if (cid) {
        this.setState({ loading: true });
        cohortsApi()
          .getCohort(namespace, id, +cid)
          .then((cohort) => {
            this.setState({ cohort, loading: false });
            currentCohortStore.next(cohort);
            if (
              existingCohort &&
              existingCohort.workspaceId === id &&
              existingCohort.cohortId === +cid
            ) {
              searchRequestStore.next(existingCohort.searchRequest);
            } else if (cohort.criteria) {
              searchRequestStore.next(parseCohortDefinition(cohort.criteria));
            }
          })
          .catch((error) => {
            console.error(error);
            this.setState({ cohortError: true, loading: false });
          });
      } else {
        this.setState(
          {
            cohort: {
              criteria: "{'includes':[],'excludes':[],'dataFilters':[]}",
              name: '',
              type: '',
            },
          },
          () => {
            if (
              existingCohort &&
              existingCohort.workspaceId === id &&
              !existingCohort.cohortId
            ) {
              searchRequestStore.next(existingCohort.searchRequest);
            } else {
              localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_SEARCH_REQUEST);
            }
          }
        );
      }
      if (existingContext) {
        const { workspaceId, cohortId, cohortContext } = existingContext;
        if (workspaceId === id && ((!cid && !cohortId) || +cid === cohortId)) {
          this.setSearchContext(cohortContext);
        } else {
          localStorage.removeItem(LOCAL_STORAGE_KEY_COHORT_CONTEXT);
        }
      }
    }

    onCohortClear() {
      const {
        history,
        location: { search },
        match: {
          params: { ns, wsid },
        },
      } = this.props;
      clearCohort();
      this.setState(
        {
          cohort: {
            criteria: "{'includes':[],'excludes':[],'dataFilters':[]}",
            name: '',
            type: '',
          },
          cohortChanged: false,
          unsavedSelections: false,
        },
        () => {
          // Clear cohortId query param if exists in url
          if (search.indexOf('cohortId') > -1) {
            history.push(`/workspaces/${ns}/${wsid}/data/cohorts/build`);
          }
        }
      );
    }

    onDiscardCohortChnages() {
      searchRequestStore.next(
        parseCohortDefinition(this.state.cohort.criteria)
      );
    }

    showUnsavedChangesModal(): boolean {
      // (cohortChanged || unsavedSelections) is the important bit that indicates if there are changes that need to be saved
      // updatingCohort should really be renamed to savingCohort and it indicates that a child component is making a
      // save cohort API call. We don't want to show the warning modal in this case because the user is intentionally
      // navigating away by calling save.
      return (
        !this.state.userClickedSaveRequest &&
        (this.state.cohortChanged || this.state.unsavedSelections)
      );
    }

    unsavedChangesMessage() {
      return `Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL and \
      ${
        this.showCohortSearch
          ? 'save your changes in the right sidebar.'
          : this.state.cohort?.id
          ? 'use Save or Save As'
          : 'click CREATE COHORT'
      } to save your criteria.`;
    }

    updateRequest = () => {
      this.setState({ updateCount: this.state.updateCount + 1 });
    };

    setSearchContext(context: any) {
      context.source = 'cohort';
      currentCohortSearchContextStore.next(context);
      this.setState({ searchContext: context });
    }

    get showCohortSearch() {
      const { cohortContext } = this.props;
      return !!cohortContext && cohortContext.source === 'cohort';
    }

    render() {
      const {
        cohort,
        cohortChanged,
        cohortError,
        criteria,
        loading,
        overview,
        updateCount,
        updateGroupListsCount,
      } = this.state;
      return (
        <React.Fragment>
          <Prompt
            when={this.showUnsavedChangesModal()}
            message={this.unsavedChangesMessage()}
          />

          <div style={{ minHeight: '42rem', padding: '0.75rem' }}>
            {cohortError ? (
              <div style={styles.cohortError}>
                <ClrIcon
                  className='is-solid'
                  shape='exclamation-triangle'
                  size={22}
                />
                Sorry, the cohort could not be loaded. Please try again or
                contact Support in the left hand navigation.
              </div>
            ) : (
              <React.Fragment>
                <FlexRowWrap
                  style={{
                    margin: '1.5rem 0 3rem',
                    ...(this.showCohortSearch ? { display: 'none' } : {}),
                  }}
                >
                  <div style={colStyle('66.66667')}>
                    <FlexRowWrap style={{ margin: '0 -0.75rem' }}>
                      {!!cohort && !!cohort.name && (
                        <div
                          style={{
                            height: '2.25rem',
                            padding: '0 0.75rem',
                            width: '100%',
                          }}
                        >
                          <h3 style={{ marginTop: 0 }}>{cohort.name}</h3>
                        </div>
                      )}
                      <div id='list-include-groups' style={colStyle('50')}>
                        <SearchGroupList
                          groups={criteria.includes}
                          setSearchContext={(c) => this.setSearchContext(c)}
                          role='includes'
                          updated={updateGroupListsCount}
                          updateRequest={() => this.updateRequest()}
                        />
                      </div>
                      <div id='list-exclude-groups' style={colStyle('50')}>
                        {overview && (
                          <SearchGroupList
                            groups={criteria.excludes}
                            setSearchContext={(c) => this.setSearchContext(c)}
                            role='excludes'
                            updated={updateGroupListsCount}
                            updateRequest={() => this.updateRequest()}
                          />
                        )}
                      </div>
                    </FlexRowWrap>
                  </div>
                  <div style={colStyle('33.33333')}>
                    {!!cohort && overview && (
                      <ListOverview
                        cohort={cohort}
                        cohortChanged={cohortChanged}
                        onCreateNewCohort={() => this.onCohortClear()}
                        onDiscardCohortChanges={() =>
                          searchRequestStore.next(
                            parseCohortDefinition(this.state.cohort.criteria)
                          )
                        }
                        searchRequest={criteria}
                        updateCount={updateCount}
                        updating={() =>
                          this.setState({ userClickedSaveRequest: true })
                        }
                      />
                    )}
                  </div>
                  {loading && <SpinnerOverlay />}
                </FlexRowWrap>
                {this.showCohortSearch && (
                  <CohortSearch
                    setUnsavedChanges={(unsaved) =>
                      this.setState({ unsavedSelections: unsaved })
                    }
                  />
                )}
              </React.Fragment>
            )}
          </div>
        </React.Fragment>
      );
    }
  }
);
