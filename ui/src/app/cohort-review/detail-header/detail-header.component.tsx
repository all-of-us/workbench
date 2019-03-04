import {Component, Input} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';

import {
  CohortReview,
  PageFilterType,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns,
  ReviewStatus,
  SortOrder
} from 'generated/fetch';
import * as React from 'react';

export interface DetailHeaderProps {
  participant: Participant;
  workspace: WorkspaceData;
}

export interface DetailHeaderState {
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
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

    update() {
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
        afterId: statuses[index - 1] && statuses[index - 1]['participantId'],
        isFirstParticipant: review.page === 0 && index === 0,
        isLastParticipant: (review.page + 1) === totalPages && (index + 1) === statuses.length,
        priorId: statuses[index - 1] && statuses[index - 1]['participantId']
      });
    }

    backToTable() {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
    }

    previous() {
      this.navigate(true);
    }

    next() {
      this.navigate(false);
    }

    private navigate(left: boolean) {
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

    private callAPI = (page: number, size: number): Observable<CohortReview> => {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
      const request = {
        page: page,
        pageSize: size,
        sortColumn: ParticipantCohortStatusColumns.PARTICIPANTID,
        sortOrder: SortOrder.Asc,
        pageFilterType: PageFilterType.ParticipantCohortStatuses
      };
      return cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, cdrid, request);
    }

    private navigateById = (id: number): void => {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants', id]);
    }

    render() {
      const {participant} = this.props;
      const {isFirstParticipant, isLastParticipant} = this.state;
      return <div className='detail-header'>
        <button
          type='button'
          className='btn btn-sm'
          title='Go Back to the Participant Table'
          onClick={() => this.backToTable()}>
          Table
        </button>
        <h4>Participant { participant.id }</h4>
        <div className='btn-group btn-sm'>
          <button
            type='button'
            className='btn btn-prev'
            title='Go To the Prior Participant'
            disabled={isFirstParticipant}
            onClick={() => this.previous()}>
            Prev
          </button>
          <button
            type='button'
            className='btn btn-next'
            title='Go To the Next Participant'
            disabled={isLastParticipant}
            onClick={() => this.next()}>
            Next
          </button>
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
