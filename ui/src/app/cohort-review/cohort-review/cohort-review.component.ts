/* TODO(jms) re-enable linting before merge to master */
/* tslint:disable */
import {Component, HostListener, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReviewService, CohortStatus} from 'generated';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels
const CDR_VERSION = 1;

const subjects$ = Observable.of([
  {id: 1, status: CohortStatus.INCLUDED},
  {id: 2, status: CohortStatus.INCLUDED},
  {id: 4, status: CohortStatus.EXCLUDED},
  {id: 5, status: CohortStatus.NEEDSFURTHERREVIEW},
  {id: 6, status: CohortStatus.NEEDSFURTHERREVIEW},
  {id: 7, status: CohortStatus.EXCLUDED},
  {id: 8, status: CohortStatus.NOTREVIEWED},
  {id: 9, status: CohortStatus.NOTREVIEWED},
  {id: 10, status: CohortStatus.EXCLUDED},
  {id: 11, status: CohortStatus.NOTREVIEWED},
  {id: 12, status: CohortStatus.EXCLUDED},
]);

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  private subjects$ = subjects$;

  private open = false;
  private loading = false;

  @ViewChild('wrapper') _wrapper;
  @ViewChild('subjectNav') _subjectNav;
  @ViewChild('openNav') _openNav;
  @ViewChild('createCohortModal') createCohortModal;

  get wrapper() { return this._wrapper.nativeElement; }
  get openNav() { return this._openNav.nativeElement; }
  get subjectNav() { return this._subjectNav.nativeElement; }

  constructor(
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this._updateWrapperDimensions();
  }

  @HostListener('document:click', ['$event'])
  onClick(event) {
    if (this.subjectNav.contains(event.target)
        || this.openNav.contains(event.target)) {
      return;
    }
    this.open = false;
  }

  @HostListener('window:resize')
  onResize() {
    this._updateWrapperDimensions();
  }

  _updateWrapperDimensions() {
    const {top} = this.wrapper.getBoundingClientRect();
    this.wrapper.style.minHeight = pixel(window.innerHeight - top - ONE_REM);
  }

  statusText(stat: CohortStatus): string {
    return {
      [CohortStatus.EXCLUDED]: 'Excluded',
      [CohortStatus.INCLUDED]: 'Included',
      [CohortStatus.NEEDSFURTHERREVIEW]: 'Undecided',
      [CohortStatus.NOTREVIEWED]: 'Unreviewed',
    }[stat];
  }

  statusClass(stat: CohortStatus) {
    if (stat === CohortStatus.INCLUDED) {
      return {'label-success': true};
    } else if (stat === CohortStatus.EXCLUDED) {
      return {'label-warning': true};
    } else {
      return {'label-info': true};
    }
  }
}
