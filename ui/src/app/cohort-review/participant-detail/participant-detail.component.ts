import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {dummyData} from './dummy-data';

import {
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns,
    ParticipantCohortStatusesRequest,
  SortOrder
} from 'generated';

// TODO make this dynamic (jms)
const CDR_VERSION = 1;

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
})
export class ParticipantDetailComponent implements OnInit, OnDestroy {
  participant: Participant;
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
  subscription: Subscription;

  DUMMY_CONDITIONS = dummyData;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.subscription = this.state.participant$
      .merge(this.route.data.pluck('participant'))
      .do(participant => this.participant = <Participant>participant)
      .withLatestFrom(this.state.review$)
      .subscribe(([participant, review]: [Participant, CohortReview]) => {
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
      });
  }

  ngOnDestroy() {
    this.state.sidebarOpen.next(false);
    this.state.participant.next(null);
    this.subscription.unsubscribe();
  }

  toggleSidebar() {
    this.state.sidebarOpen$
      .take(1)
      .subscribe(val => this.state.sidebarOpen.next(!val));
  }

  up() {
    this.router.navigate(['..'], {relativeTo: this.route});
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

      this.state.review$
        .take(1)
        .map(({page, pageSize}) => ({page: adjustPage(page), size: pageSize}))
        .mergeMap(({page, size}) => this.callAPI(page, size))
        .subscribe(review => {
          this.state.review.next(review);
          const stat = statusGetter(review.participantCohortStatuses);
          this.navigateById(stat.participantId);
        });
    }
  }

  private callAPI = (page: number, size: number): Observable<CohortReview> => {
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const request = {
        page: page,
        pageSize: size,
        sortColumn: ParticipantCohortStatusColumns.ParticipantId,
        sortOrder: SortOrder.Asc
    };
    return this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION, request);
  }

  private navigateById = (id: number): void => {
    this.router.navigate(['..', id], {relativeTo: this.route});
  }
}
