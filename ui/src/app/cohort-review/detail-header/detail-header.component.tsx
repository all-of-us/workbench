import {Component, Input} from '@angular/core';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore, filterStateStore} from 'app/cohort-review/review-state.service';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore, currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';

import {CohortReview, PageFilterRequest, PageFilterType, ParticipantCohortStatus, SortOrder} from 'generated/fetch';
import {RadioButton} from 'primereact/radiobutton';
import * as React from 'react';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';
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
  }
});
const navBtnStyles = {
  navigation: {
    ...styles.headerSection,
    padding: '1.15rem 0.4rem',
  },
  radios: {
    ...styles.headerSection,
    padding: '0.5rem',
  },
  navBtnActive: {
    ...styles.navBtn,
    color: '#2691D0',
    border: '1px solid #2691D0',
    cursor: 'pointer',
  },
  navBtnDisabled: {
    ...styles.navBtn,
    color: '#cccccc',
    border: '1px solid #cccccc',
    cursor: 'not-allowed',
  },
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
        filterState: filterStateStore.getValue()
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

    render() {
      const {participant} = this.props;
      const {filterState, isFirstParticipant, isLastParticipant} = this.state;
      const cohort = currentCohortStore.getValue();
      return <div className='detail-header'>
        <button
          style={styles.backBtn}
          type='button'
          title='Go Back to the review set table'
          onClick={() => this.backToTable()}>
          Back to review set
        </button>
        <h4 style={styles.title}>{cohort.name}</h4>
        <div style={styles.description}>{cohort.description}</div>
        <div className='p-grid' style={{height: '3.5rem'}}>
          <div className='p-col' style={navBtnStyles.navigation}>
            <button
              style={isFirstParticipant ? navBtnStyles.navBtnDisabled : navBtnStyles.navBtnActive}
              type='button'
              title='Go To the Prior Participant'
              disabled={isFirstParticipant}
              onClick={() => this.previous()}>
              <i style={styles.icon} className='pi pi-angle-left' />
            </button>
            <span style={styles.participantText}>Participant { participant.id }</span>
            <button
              style={isLastParticipant ? navBtnStyles.navBtnDisabled : navBtnStyles.navBtnActive}
              type='button'
              title='Go To the Next Participant'
              disabled={isLastParticipant}
              onClick={() => this.next()}>
              <i style={styles.icon} className='pi pi-angle-right' />
            </button>
          </div>
          <div className='p-col' style={navBtnStyles.radios}>
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
