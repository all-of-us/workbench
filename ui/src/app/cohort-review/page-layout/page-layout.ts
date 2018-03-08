import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  Renderer2,
  ViewChild,
} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {CreateReviewComponent} from '../create-review/create-review.component';
import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit {
  @ViewChild('createReviewModal') createReviewModal: CreateReviewComponent;
  @ViewChild('fullPageDiv') fullPageDiv: ElementRef;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private renderer: Renderer2,
  ) {}

  ngOnInit() {
    this.updateWrapperDimensions();

    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.createReviewModal.modal.open();
    }
  }

  @HostListener('window:resize')
  onResize() {
    this.updateWrapperDimensions();
  }

  updateWrapperDimensions() {
    const nativeEl = this.fullPageDiv.nativeElement;
    const {top} = nativeEl.getBoundingClientRect();

    // margin-top, margin-bottom each one rem, see the css file
    const margins = ONE_REM * 2;

    const minHeight = pixel(window.innerHeight - top - margins);
    this.renderer.setStyle(nativeEl, 'min-height', minHeight);
  }
}
