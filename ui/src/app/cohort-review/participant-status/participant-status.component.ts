import {Component, OnInit} from '@angular/core';

import {
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit {

  private activeID: ParticipantCohortStatus['participantId'];

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.activeID = this.route.params
      .map(params => params.participantId);
  }
}
