import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Input,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
} from '../redux';

import {SearchGroupItem} from 'generated';


@Component({
  selector: 'app-search-group-item',
  templateUrl: './search-group-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchGroupItemComponent implements OnInit, OnDestroy {
  @Input() item;
  @Input() role: string;
  @Input() index: number;
  @Input() itemIndex: number;

  private count = 0;
  private loading = true;
  private subscriptions: Subscription[];

  constructor(private cd: ChangeDetectorRef,
              private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const countSelector = (state) => state.getIn(
      ['results', this.role, this.index, this.itemIndex, 'count'],
      0  // specifies default; otherwise returns emtpy map
    );

    const loadingSelector = (state) => 
      state.get('requests').has(List([this.role, this.index, this.itemIndex]));
    const countSub = this.ngRedux.select(countSelector).subscribe(
      count => {
        this.count = count;
        this.cd.markForCheck();
    });

    const loadSub = this.ngRedux.select(loadingSelector).subscribe(
      loading => {
        this.loading = loading;
        this.cd.markForCheck();
    });

    this.subscriptions = [countSub, loadSub];
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  get description() {
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }
}
