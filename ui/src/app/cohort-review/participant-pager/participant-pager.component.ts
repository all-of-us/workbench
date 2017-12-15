import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  CohortStatus,
  ParticipantCohortStatus,
} from 'generated';

@Component({
  selector: 'app-participant-pager',
  templateUrl: './participant-pager.component.html',
  styleUrls: ['./participant-pager.component.css']
})
export class ParticipantPagerComponent implements OnInit {

  @Output() onSelection = new EventEmitter<ParticipantCohortStatus>();
  private pageSize: number;
  private page: number;
  private participants: ParticipantCohortStatus[];
  private sub: Subscription;

  constructor(
    private state: ReviewStateService,
  ) {}

  ngOnInit() {
    this.sub = this.state.review
      .pluck('participantCohortStatuses')
      .subscribe(statuses =>
        this.participants = <ParticipantCohortStatus[]>statuses);
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
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
