import {Component, OnInit} from '@angular/core';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {navigate, urlParamsStore} from 'app/utils/navigation';
import {ReviewStatus} from 'generated/fetch';

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit {

  subscription: any;
  create = false;
  constructor() {}

  ngOnInit() {
    const review = cohortReviewStore.getValue();

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.create = true;
    } else {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['workspaces', ns, wsid, 'cohorts', cid, 'review', 'participants']);
    }
  }

  reviewCreated(created: boolean) {
    this.create = !created;
  }
}
