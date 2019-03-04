import {Component, OnDestroy, OnInit} from '@angular/core';

import {cohortReviewStore, filterStateStore} from 'app/cohort-review/review-state.service';
import {navigate, urlParamsStore} from 'app/utils/navigation';
import {ReviewStatus} from 'generated';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnDestroy, OnInit {

  subscription: any;
  create = false;
  constructor() {}

  ngOnInit(): void {
    const review = cohortReviewStore.getValue();

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.create = true;
    } else {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
    }
  }

  ngOnDestroy(): void {
    filterStateStore.next(null);
  }

  reviewCreated(created: boolean) {
    this.create = !created;
  }
}
