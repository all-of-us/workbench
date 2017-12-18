import {
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CohortStatus,
  ParticipantCohortStatus,
} from 'generated';

@Component({
  selector: 'app-participant-pager',
  templateUrl: './participant-pager.component.html',
  styleUrls: ['./participant-pager.component.css']
})
export class ParticipantPagerComponent implements OnInit, OnDestroy {

  @Output() onSelection = new EventEmitter<ParticipantCohortStatus>();

  private page: number;
  private pageSize: number;
  private lastPage: number;

  private sortOrder: string;
  private sortColumn: string;

  private participants: ParticipantCohortStatus[];
  private sub: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
  ) {}

  ngOnInit() {
    this.sub = this.state.review
      .subscribe(review => {
        this.page = review.page;
        this.pageSize = review.pageSize;
        this.lastPage = (review.reviewSize / review.pageSize) - 1;
        this.sortOrder = review.sortOrder;
        this.sortColumn = review.sortColumn;
        this.participants = review.participantCohortStatuses;
      });
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
  }

  goToPage(pageNo: number) {
    this.state.context$
      .mergeMap(context => this.reviewAPI.getParticipantCohortStatuses(
        context.workspaceNamespace,
        context.workspaceId,
        context.cohortId,
        context.cdrVersion,
        pageNo,
        this.pageSize,
        this.sortOrder,
        this.sortColumn))
      .subscribe(this.state.review);
  }

  statusText(stat: CohortStatus): string {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Undecided',
      [CohortStatus.NOTREVIEWED]: 'Unreviewed',
    }[stat];
  }

  statusClass(stat: CohortStatus): object {
    if (stat === CohortStatus.INCLUDED) {
      return {'label-success': true};
    } else if (stat === CohortStatus.EXCLUDED) {
      return {'label-warning': true};
    } else {
      return {'label-info': true};
    }
  }

  onSelect(selected: ParticipantCohortStatus): void {
    this.onSelection.emit(selected);
    this.state.participant.next(selected);
  }
}
