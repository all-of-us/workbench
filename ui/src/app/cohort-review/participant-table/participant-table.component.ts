import {Component, OnInit} from '@angular/core';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {ParticipantCohortStatus} from 'generated';

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit {
  DUMMY_DATA: Participant[];
  reviewSize: number;
  matchedParticipantCount: number;
  pageSize: number;

  constructor(private state: ReviewStateService) {}

  ngOnInit() {
    this.state.review$
      .do(({reviewSize}) => this.reviewSize = reviewSize)
      .do(({matchedParticipantCount}) => this.matchedParticipantCount = matchedParticipantCount)
      .do(({pageSize}) => this.pageSize = pageSize)
      .pluck('participantCohortStatuses')
      .map(statusSet =>
        (<ParticipantCohortStatus[]>statusSet).map(Participant.makeRandomFromExisting))
      .subscribe(val => this.DUMMY_DATA = <Participant[]>val);
  }
}
