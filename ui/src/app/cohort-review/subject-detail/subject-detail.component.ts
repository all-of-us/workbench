import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReviewService} from 'generated';

@Component({
  selector: 'app-subject-detail',
  templateUrl: './subject-detail.component.html',
  styleUrls: ['./subject-detail.component.css']
})
export class SubjectDetailComponent implements OnInit {
  private subjectId$: Observable<any>;

  constructor(
    private route: ActivatedRoute,
    /* tslint:disable-next-line:no-unused-variable */
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    // TODO: here we load the subject detail data
    this.subjectId$ = this.route.params
      .switchMap(params => Observable.of(params.subjectID));
  }
}
