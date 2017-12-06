import {Component, HostListener, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {CohortReviewService} from 'generated';

const CDR_VERSION = 1;

const pixel = (n: number) => `${n}px`;

const subjects$ = Observable.of([
  {id: 1, status: 'NR'},
  {id: 2, status: 'NR'},
  {id: 4, status: 'NR'},
  {id: 5, status: 'NR'},
  {id: 5, status: 'NR'},
  {id: 6, status: 'NR'},
  {id: 7, status: 'NR'},
  {id: 8, status: 'NR'},
  {id: 9, status: 'NR'},
  {id: 10, status: 'NR'},
  {id: 11, status: 'NR'},
  {id: 12, status: 'NR'},
]);

@Component({
  selector: 'app-cohort-review',
  templateUrl: './cohort-review.component.html',
  styleUrls: ['./cohort-review.component.css']
})
export class CohortReviewComponent implements OnInit {
  private subjects$ = subjects$;
  private open = false;

  @ViewChild('wrapper') _wrapper;
  @ViewChild('subjectNav') _subjectNav;
  @ViewChild('openNav') _openNav;

  get wrapper() { return this._wrapper.nativeElement; }
  get openNav() { return this._openNav.nativeElement; }
  get subjectNav() { return this._subjectNav.nativeElement; }

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
    this.wrapper.style.minHeight = pixel(window.innerHeight - top);
  }
}
