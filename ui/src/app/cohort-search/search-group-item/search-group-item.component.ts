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

import {CohortSearchActions} from '../redux/actions';
import {CohortSearchState} from '../redux/store';
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
  private subscription: Subscription;

  constructor(private cd: ChangeDetectorRef,
              private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    const pathSelector = (state) => state.getIn(
      ['results', this.role, this.index, this.itemIndex, 'count'],
      0  // specifies default; otherwise returns emtpy map
    );
    this.subscription = this.ngRedux.select(pathSelector)
      .subscribe(count => {
        this.count = count;
        this.cd.markForCheck();
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get description() {
    const _type = this.item.get('type');
    return this.item.get('description', `${_type} Codes`);
  }
}
