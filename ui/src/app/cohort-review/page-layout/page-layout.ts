import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ReviewStatus} from 'generated';
import {ReviewStateService} from '../review-state.service';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit {

  create = false;
  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.create = true;
    } else {
      this.router.navigate(['participants'], {relativeTo: this.route});
    }
  }

  reviewCreated(created: boolean) {
    this.create = !created;
  }
}
