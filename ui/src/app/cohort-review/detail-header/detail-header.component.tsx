import {Component, Input} from '@angular/core';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, filterStateStore} from 'app/cohort-review/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';

import {CohortReview, PageFilterRequest, PageFilterType, ParticipantCohortStatus, SortOrder} from 'generated/fetch';
import {Calendar} from 'primereact/calendar';
import {RadioButton} from 'primereact/radiobutton';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';
import * as moment from 'moment';
const css = `
  body .p-calendar.p-calendar-w-btn > .p-inputtext,
  body .p-calendar.p-calendar-w-btn > .p-inputtext:enabled:hover:not(.p-error) {
    width: 140px;
    border-top-right-radius: 3px;
    border-bottom-right-radius: 3px;
    border-right: 1px solid #212121;
  }
  .p-calendar > .p-calendar-button,
   .p-calendar > .p-calendar-button:enabled:hover {
    color: #216FB4;
    background: transparent;
    border: 0;
  }
`;
const styles = reactStyles({
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: '#2691d0',
    background: 'transparent',
    cursor: 'pointer',
  },
  title: {
    marginTop: 0,
    fontSize: '20px',
    color: '#262262',
  },
  description: {
    margin: '0.75rem 0',
    color: '#000000',
  },
  headerSection: {
    float: 'left',
    height: '100%',
    marginRight: '1rem',
    border: '1px solid #cccccc',
    borderRadius: '5px',
    background: '#fafafa',
  },
  navBtn: {
    display: 'inline-block',
    fontSize: '12px',
    padding: '5px',
    borderRadius: '3px',
  },
  icon: {
    display: 'block',
    fontSize: '12px',
  },
  participantText: {
    fontSize: '14px',
    color: '#262262',
    padding: '0 1rem'
  },
  filterHeader: {
    height: '33%',
    marginBottom: '0.5rem',
    borderBottom: '1px solid #216fb4',
  },
  filterTab: {
    margin: '0 0.25rem',
    fontSize: '12px',
    color: '#2691d0',
    border: 0,
    background: 'transparent',
    cursor: 'pointer',
  },
  filterBody: {
    paddingLeft: '0.5rem',
  },
  resetBtn: {
    float: 'right',
    lineHeight: '16px',
    padding: '0 2px',
    margin: '5px 5px 0 0',
    fontSize: '11px',
    color: '#2691d0',
    border: '1px solid #2691d0',
    borderRadius: '3px',
    background: 'transparent',
  },
});
const otherStyles = {
  navigation: {
    ...styles.headerSection,
    width: '20%',
    padding: '1.15rem 0.4rem',
  },
  filters: {
    ...styles.headerSection,
    width: '40%',
  },
  radios: {
    ...styles.headerSection,
    width: '20%',
    padding: '0.5rem',
  },
  navBtnActive: {
    ...styles.navBtn,
    color: '#2691D0',
    border: '1px solid #2691d0',
    cursor: 'pointer',
  },
  navBtnDisabled: {
    ...styles.navBtn,
    color: '#cccccc',
    border: '1px solid #cccccc',
    cursor: 'not-allowed',
  },
  tabActive: {
    ...styles.filterTab,
    borderBottom: '2px solid #216FB4',
    fontWeight: 600,
  }
};
export interface DetailHeaderProps {
  participant: Participant;
  workspace: WorkspaceData;
}

export interface DetailHeaderState {
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
  filterState: any;
  filterTab: string;
}

export const DetailHeader = withCurrentWorkspace()(
  class extends React.Component<DetailHeaderProps, DetailHeaderState> {
    constructor(props: DetailHeaderProps) {
      super(props);
      this.state = {
        isFirstParticipant: undefined,
        isLastParticipant: undefined,
        priorId: undefined,
        afterId: undefined,
        filterState: filterStateStore.getValue(),
        filterTab: 'date'
      };
    }

    componentDidMount() {
      this.update();
    }

    componentDidUpdate(prevProps: any) {
      if (prevProps.participant !== this.props.participant) {
        this.update();
      }
    }

    update = () => {
      const review = cohortReviewStore.getValue();
      const participant = this.props.participant;
      const statuses = review.participantCohortStatuses;
      const id = participant && participant.participantId;
      const index = statuses.findIndex(({participantId}) => participantId === id);

      // The participant is not on the current page... for now, just log it and ignore it
      // We get here by URL (when a direct link to a detail page is shared, for example)
      if (index < 0) {
        console.log('Participant not on page');
        // For now, disable next / prev entirely
        this.setState({
          isFirstParticipant: true,
          isLastParticipant: true,
        });
        return;
      }

      const totalPages = Math.floor(review.reviewSize / review.pageSize);

      this.setState({
        afterId: statuses[index + 1] && statuses[index + 1]['participantId'],
        isFirstParticipant: review.page === 0 && index === 0,
        isLastParticipant: (review.page + 1) === totalPages && (index + 1) === statuses.length,
        priorId: statuses[index - 1] && statuses[index - 1]['participantId']
      });
    }

    backToTable() {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
    }

    previous = () => {
      this.navigate(true);
    }

    next = () => {
      this.navigate(false);
    }

    navigate = (left: boolean) => {
      const {afterId, isFirstParticipant, isLastParticipant, priorId} = this.state;
      const id = left ? priorId : afterId;
      const hasNext = !(left ? isFirstParticipant : isLastParticipant);

      if (id !== undefined) {
        this.navigateById(id);
      } else if (hasNext) {
        const statusGetter = (statuses: ParticipantCohortStatus[]) => left
          ? statuses[statuses.length - 1]
          : statuses[0];

        const adjustPage = (page: number) => left
          ? page - 1
          : page + 1;

        cohortReviewStore
          .take(1)
          .map(({page, pageSize}) => ({page: adjustPage(page), size: pageSize}))
          .mergeMap(({page, size}) => this.callAPI(page, size))
          .subscribe(review => {
            cohortReviewStore.next(review);
            const stat = statusGetter(review.participantCohortStatuses);
            this.navigateById(stat.participantId);

          });
      }
    }

    callAPI = (page: number, size: number): Observable<CohortReview> => {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
      const request = {
        page: page,
        pageSize: size,
        sortOrder: SortOrder.Asc,
        pageFilterType: PageFilterType.ParticipantCohortStatuses
      } as PageFilterRequest;
      return from(cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, cdrid, request));
    }

    navigateById = (id: number): void => {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants', id]);
    }

    vocabChange = (event: any) => {
      const {filterState} = this.state;
      filterState.vocab = event.value;
      filterStateStore.next(filterState);
      this.setState({filterState: filterState});
    }

    setFilter = (value: any, type: string, field?: string) => {
      const {filterState} = this.state;
      switch (type) {
        case 'age':
          filterState.global[type][field] = value;
          filterStateStore.next(filterState);
          break;
        case 'date':
          filterState.global[type][field] = value;
          if (typeof value === 'object') {
            filterStateStore.next(filterState);
          }
          break;
        case 'visits':
          filterState.global[type] = value;
          filterStateStore.next(filterState);
          break;
      }
      this.setState({filterState: filterState});
    }

    clearFilters = () => {
      const {filterState} = this.state;
      filterState.global = {
        date: {min: null, max: null},
        age: {min: null, max: null},
        visits: null
      };
      filterStateStore.next(filterState);
      this.setState({filterState: filterState});
    }

    render() {
      const {participant} = this.props;
      const {
        filterState: {global: {age, date, visits}},
        filterState,
        filterTab,
        isFirstParticipant,
        isLastParticipant
      } = this.state;
      const cohort = currentCohortStore.getValue();
      return <div className='detail-header'>
        <style>{css}</style>
        <button
          style={styles.backBtn}
          type='button'
          title='Go Back to the review set table'
          onClick={() => this.backToTable()}>
          Back to review set
        </button>
        <h4 style={styles.title}>{cohort.name}</h4>
        <div style={styles.description}>{cohort.description}</div>
        <div style={{height: '3.5rem'}}>
          <div style={otherStyles.navigation}>
            <button
              style={isFirstParticipant ? otherStyles.navBtnDisabled : otherStyles.navBtnActive}
              type='button'
              title='Go To the Prior Participant'
              disabled={isFirstParticipant}
              onClick={() => this.previous()}>
              <i style={styles.icon} className='pi pi-angle-left' />
            </button>
            <span style={styles.participantText}>Participant { participant.id }</span>
            <button
              style={isLastParticipant ? otherStyles.navBtnDisabled : otherStyles.navBtnActive}
              type='button'
              title='Go To the Next Participant'
              disabled={isLastParticipant}
              onClick={() => this.next()}>
              <i style={styles.icon} className='pi pi-angle-right' />
            </button>
          </div>
          <div style={otherStyles.filters}>
            <div style={styles.filterHeader}>
              <button
                style={filterTab === 'date' ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setState({filterTab: 'date'})}>
                Date Range
              </button>
              <button
                style={filterTab === 'age' ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setState({filterTab: 'age'})}>
                Age Range
              </button>
              <button
                style={filterTab === 'visits' ? otherStyles.tabActive : styles.filterTab}
                onClick={() => this.setState({filterTab: 'visits'})}>
                Visits
              </button>
              <button
                style={styles.resetBtn}
                onClick={() => this.clearFilters()}>
                RESET FILTER
              </button>
            </div>
            <div style={styles.filterBody}>
              {filterTab === 'date' && <div>
                <div style={{float: 'left', width: '25%'}}>
                  Select Date Range:
                </div>
                <div style={{float: 'left', width: '30%'}}>
                  <Calendar
                    style={{width: '140px'}}
                    dateFormat='yy-mm-dd'
                    value={date.min}
                    onChange={(e) => this.setFilter(e.value, 'date', 'min')}
                    monthNavigator={true}
                    yearNavigator={true}
                    yearRange='1941:2018'
                    showIcon={true}
                  />
                </div>
                <div style={{float: 'left', width: '10%', marginLeft: '0.5rem'}}>
                  and
                </div>
                <div style={{float: 'left', width: '30%'}}>
                  <Calendar
                    style={{width: '140px'}}
                    dateFormat='yy-mm-dd'
                    value={date.max}
                    onChange={(e) => this.setFilter(e.value, 'date', 'max')}
                    monthNavigator={true}
                    yearNavigator={true}
                    yearRange='1940:2018'
                    showIcon={true}
                  />
                </div>
              </div>}
              {filterTab === 'age' && <div>Age Range</div>}
              {filterTab === 'visits' && <div>Visits</div>}
            </div>
          </div>
          <div style={otherStyles.radios}>
            <div>
              <RadioButton
                name='vocab'
                value='source'
                onChange={this.vocabChange}
                checked={filterState.vocab === 'source'} />
              <label className='p-radiobutton-label'>View Source Concepts</label>
            </div>
            <div>
              <RadioButton
                name='vocab'
                value='standard'
                onChange={this.vocabChange}
                checked={filterState.vocab === 'standard'} />
              <label className='p-radiobutton-label'>View Standard Concepts</label>
            </div>
          </div>
        </div>
      </div>;
    }
  }
);

@Component({
  selector: 'app-detail-header',
  template: '<div #root></div>'
})
export class DetailHeaderComponent extends ReactWrapperBase {
  @Input('participant') participant: DetailHeaderProps['participant'];

  constructor() {
    super(DetailHeader, ['participant']);
  }
}
