import {Component, Input, OnInit, OnDestroy} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchState, isCriteriaLoading} from '../../redux';

@Component({
  selector: 'app-criteria-wizard-root-spinner',
  template: `
    <div *ngIf="loading" id="spinner-container">
      <div class="spinner" [style.left]="'50%'">Loading...</div>
    </div>
  `,
})
export class RootSpinnerComponent implements OnInit, OnDestroy {
  @Input() node;
  private loading: boolean;
  private subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
  ) {}

  ngOnInit() {
    const kind = this.node.get('type');
    const id = this.node.get('id');
    const selectorFunc = isCriteriaLoading(kind, id);
    const loading$ = this.ngRedux.select(selectorFunc);

    this.loading = true;
    this.subscription = loading$.subscribe(value => this.loading = value);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
