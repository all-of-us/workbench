import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {activeParameterList, CohortSearchActions, CohortSearchState} from 'app/cohort-search/redux';
import {TreeType} from 'generated';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'crit-list-multi-select',
  templateUrl: './list-multi-select.component.html',
  styleUrls: ['./list-multi-select.component.css']
})
export class ListMultiSelectComponent implements OnInit, OnDestroy {
  @Input() options = [];
  @Input() select: Function;
  @Input() selections = [];
  @Input() loading: boolean;
  selected = new Set();
  subscription: Subscription;
  selectedOption: any;
  constructor(private actions: CohortSearchActions,
    private ngRedux: NgRedux<CohortSearchState>) {}


  ngOnInit() {
    this.subscription = this.ngRedux
      .select(activeParameterList)
      .subscribe(val => {
        this.selectedOption = [];
        val.forEach( paramList => {
          if (paramList.get('type') === TreeType.DEMO) {
            this.selectedOption.push(paramList.get('parameterId'));
          }
        });
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
