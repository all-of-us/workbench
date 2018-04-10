import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  Renderer2,
  ViewChild,
} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {ReviewStatus} from 'generated';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels


@Component({
  templateUrl: './page-layout.html',
  styleUrls: ['./page-layout.css']
})
export class PageLayout implements OnInit {
  @ViewChild('wrapper') wrapper: ElementRef;

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
    private renderer: Renderer2,
  ) {}

  ngOnInit() {
    this.updateWrapperDimensions();

    const {annotationDefinitions, cohort, review} = this.route.snapshot.data;
    this.state.annotationDefinitions.next(annotationDefinitions);
    this.state.cohort.next(cohort);
    this.state.review.next(review);

    if (review.reviewStatus === ReviewStatus.NONE) {
      this.router.navigate(['create'], {relativeTo: this.route});
    }
  }

  @HostListener('window:resize')
  onResize() {
    this.updateWrapperDimensions();
  }

  updateWrapperDimensions() {
    const nativeEl = this.wrapper.nativeElement;
    const {top} = nativeEl.getBoundingClientRect();

    // margin-top, margin-bottom each one rem, see the css file
    const margins = ONE_REM * 2;

    const minHeight = pixel(window.innerHeight - top - margins);
    this.renderer.setStyle(nativeEl, 'min-height', minHeight);
  }
}
