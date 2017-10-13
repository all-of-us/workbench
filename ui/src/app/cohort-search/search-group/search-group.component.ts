import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Input,
  EventEmitter,
  Output,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchState, countFor, pathTo, isLoading} from '../redux';
import {SearchGroup, SearchRequest} from 'generated';


@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
  styleUrls: ['search-group.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchGroupComponent implements OnInit, OnDestroy {
  /* Passed through from cohort-builder to add-criteria */
  @Input() index: number;
  @Input() role: keyof SearchRequest;
  @Input() group: SearchGroup;
  @Output() onRemove = new EventEmitter<boolean>();

  private count = 0;
  private subscription: Subscription;

  constructor(private cd: ChangeDetectorRef,
              private ngRedux: NgRedux<CohortSearchState>) {}

  ngOnInit() {
    this.subscription = this.ngRedux.select(
      countFor(pathTo(this.role, this.index))
    ).subscribe(
      count => {
        this.count = count;
        this.cd.markForCheck();
      }
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  remove(event) { this.onRemove.emit(true); }
}
