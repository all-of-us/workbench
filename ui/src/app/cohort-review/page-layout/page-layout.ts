import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStatus} from 'generated';
import {ReviewStateService} from '../review-state.service';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit {
  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.router.navigate(['create'], {relativeTo: this.route});
    }
  }
}
