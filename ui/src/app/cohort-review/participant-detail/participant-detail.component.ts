import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReviewService} from 'generated';

@Component({
  selector: 'app-participant-detail',
  templateUrl: './participant-detail.component.html',
  styleUrls: ['./participant-detail.component.css']
})
export class ParticipantDetailComponent implements OnInit {
  private subjectId$: Observable<any>;

  constructor(
    private route: ActivatedRoute,
    /* tslint:disable-next-line:no-unused-variable */
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    // TODO: here we load the subject detail data
    this.subjectId$ = this.route.params
      .switchMap(params => Observable.of(params.participantId));
  }
}
