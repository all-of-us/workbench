import {Component, Input, OnChanges} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';

import {
  CohortReview,
  CohortReviewService,
  PageFilterType,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns,
  SortOrder
} from 'generated';


@Component({
  selector: 'app-detail-header',
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css']
})
export class DetailHeaderComponent implements OnChanges {
  @Input() participant: Participant;
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;

  constructor(
    private reviewAPI: CohortReviewService,
  ) {}

  ngOnChanges(changes) {
    this.update(cohortReviewStore.getValue());
  }

  update(review) {
    const participant = this.participant;
    const statuses = review.participantCohortStatuses;
    const id = participant && participant.participantId;
    const index = statuses.findIndex(({participantId}) => participantId === id);

    // The participant is not on the current page... for now, just log it and ignore it
    // We get here by URL (when a direct link to a detail page is shared, for example)
    if (index < 0) {
      console.log('Participant not on page');
      // For now, disable next / prev entirely
      this.isFirstParticipant = true;
      this.isLastParticipant = true;
      return;
    }

    const totalPages = Math.floor(review.reviewSize / review.pageSize);

    this.isFirstParticipant =
      review.page === 0   // first page
      && index === 0;     // first person on page

    this.isLastParticipant =
      (review.page + 1) === totalPages    // last page
      && (index + 1) === statuses.length; // last person on page

    this.priorId = statuses[index - 1] && statuses[index - 1]['participantId'];
    this.afterId = statuses[index + 1] && statuses[index + 1]['participantId'];
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
    const id = left ? this.priorId : this.afterId;
    const hasNext = !(left ? this.isFirstParticipant : this.isLastParticipant);

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
    return this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, cdrid, request);
  }

  private navigateById = (id: number): void => {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    navigate(['/workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants', id]);
  }
}
