import {Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'app-descriptive-stats',
  templateUrl: './query-descriptive-stats.component.html',
  styleUrls: ['./query-descriptive-stats.component.css']
})
export class QueryDescriptiveStatsComponent implements OnInit, OnChanges, OnDestroy {
  @Input() demoData: Observable<List<any>>;
  graphData = {};
  subscription: Subscription;
  groupKeys = [
    'gender', 'ageRange', 'race'
  ];
  totalCount: number;
  enablePrint = false;
  constructor() {}

  ngOnChanges() {
    if (this.demoData) {
      const data = this.demoData;
      this.groupKeys.forEach( k => {
        const groupBy = k;
        this.getChartGroupedData(data, groupBy);
      });
      this.enablePrint = true;
    }
  }

  ngOnInit() {
    this.subscription = cohortReviewStore.subscribe(review => {
      this.totalCount = review.matchedParticipantCount;
    });
  }

  getFormattedGroup(group) {
    switch (group) {
      case 'F' :
        return 'Female';
      case 'M' :
        return 'Male';
      default:
        return group;
    }
  }

  getChartGroupedData(data, groupBy) {
    const chartData = data.reduce((acc, i) => {
      const key = i[groupBy]; // F or M
      acc[key] = acc[key] || { data: []};
      acc[key].data.push(i);
      return acc;
    }, {});
    this.graphData[groupBy] = Object.keys(chartData).map(k => {
      const newData = chartData[k].data;
      const count = newData.reduce((sum, d) => {
        return  sum + d.count;
      }, 0);
      const percentage = (count / this.totalCount) * 100;
      return {
        group: this.getFormattedGroup(k),
        data: newData,
        count: count,
        percentage: percentage
      };
    });
  }

  onPrint() {
    window.print();
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
