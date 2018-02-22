import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {CreateReviewComponent} from '../create-review/create-review.component';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  @ViewChild('createReviewModal') createReviewModal: CreateReviewComponent;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.createReviewModal.modal.open();
    }
  }

  get angleDir() {
    return this.sidebarOpen ? 'right' : 'left';
  }

  get sidebarOpen() {
    return this.state.sidebarOpen.getValue();
  }

  set sidebarOpen(value: boolean) {
    this.state.sidebarOpen.next(value);
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }
}
