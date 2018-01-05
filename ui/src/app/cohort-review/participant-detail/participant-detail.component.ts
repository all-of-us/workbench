import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatus
} from 'generated';

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
  styleUrls: ['./participant-detail.component.css']
})
export class ParticipantDetailComponent implements OnInit, OnDestroy {
  participant: Participant;
  isFirstParticipant: boolean;
  isLastParticipant: boolean;
  priorId: number;
  afterId: number;
  subscription: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const participant$ = this.state.participant$.merge(this.route.data.pluck('participant'));

    this.subscription = participant$
      .subscribe(participant => this.participant = <Participant>participant);

    const sub = Observable.combineLatest(participant$, this.state.review$)
      .subscribe(
        ([participant, review]: [Participant, CohortReview]) => {
          const statuses = review.participantCohortStatuses;
          const id = participant && participant.participantId;
          const index = statuses.findIndex(({participantId}) => participantId === id);

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

    this.subscription.add(sub);
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
    if (this.priorId !== undefined) {
      this.router.navigate(['..', this.priorId], {relativeTo: this.route});
    } else if (!this.isFirstParticipant) {
      const CDR_VERSION = 1;
      const {ns, wsid, cid} = this.route.parent.snapshot.params;
      this.state.review$
        .take(1)
        .mergeMap(({page, pageSize}) => this.reviewAPI.getParticipantCohortStatuses(
          ns, wsid, cid, CDR_VERSION, page - 1, pageSize
        ))
        .subscribe(review => {
          this.state.review.next(review);
          const lastStatus = review.participantCohortStatuses.pop();
          this.router.navigate(['..', lastStatus.participantId], {relativeTo: this.route});
        });
    }
  }

  next() {
    if (this.afterId !== undefined) {
      this.router.navigate(['..', this.afterId], {relativeTo: this.route});
    } else if (!this.isLastParticipant) {
      const CDR_VERSION = 1;
      const {ns, wsid, cid} = this.route.parent.snapshot.params;
      this.state.review$
        .take(1)
        .mergeMap(({page, pageSize}) => this.reviewAPI.getParticipantCohortStatuses(
          ns, wsid, cid, CDR_VERSION, page + 1, pageSize
        ))
        .subscribe(review => {
          this.state.review.next(review);
          const firstStatus = review.participantCohortStatuses[0];
          this.router.navigate(['..', firstStatus.participantId], {relativeTo: this.route});
        });
    }
  }
}
