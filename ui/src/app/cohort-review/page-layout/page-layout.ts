import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {currentCohortStore} from 'app/utils/navigation';
import {ReviewStatus} from 'generated';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit, OnDestroy {

  subscription: any;
  create = false;
  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const {review} = this.route.snapshot.data;
    this.subscription = this.route.data.subscribe(({cohort}) => {
      this.state.cohort.next(cohort);
      currentCohortStore.next(cohort);
    });
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.create = true;
    } else {
      this.router.navigate(['participants'], {relativeTo: this.route});
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  reviewCreated(created: boolean) {
    this.create = !created;
  }
}
