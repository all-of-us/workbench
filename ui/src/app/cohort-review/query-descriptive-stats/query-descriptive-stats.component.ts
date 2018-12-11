import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {ReviewStateService} from '../review-state.service';

@Component({
  selector: 'app-descriptive-stats',
  templateUrl: './query-descriptive-stats.component.html',
  styleUrls: ['./query-descriptive-stats.component.css']
})
export class QueryDescriptiveStatsComponent implements OnInit, OnChanges {
 @Input() demoData: any;
 graphData = {};
  updateShape: any;
  subscription: Subscription;
  groupKeys = [
    'gender', 'ageRange', 'race'
  ];

  totalCount: number;
  constructor(private state: ReviewStateService) {}


  ngOnChanges() {
    if (this.demoData) {
      const data = this.demoData.toJS();
      this.groupKeys.forEach( k => {
        const groupBy = k;
        this.getGroupedData(data, groupBy);
      });
    }
  }

  getFormattedGroup(group) {
    switch (group) {
      case 'F' :
        return 'Female';
      case 'M' :
        return 'Male';
    }
  }
  ngOnInit() {
    this.subscription = this.state.review$.subscribe(review => {
      this.totalCount = review.matchedParticipantCount;
    });
  }

  getGroupedData(data, groupBy) {

    const test = data.reduce((acc, i) => {
      const key = i[groupBy]; // F or M
       acc[key] = acc[key] || { data: []};
       acc[key].data.push(i);
       return acc;
    }, {});

     this.updateShape = Object.keys(test).map(k => {

       if( k === 'F' || k === 'M') {
         return Object.assign({}, {
           group: this.getFormattedGroup(k),
           data: test[k].data
         });
       } else {
         return Object.assign({}, {
           group: k,
           data: test[k].data
         });
       }

    }).map(item => {
      return Object.assign({}, item, {
        count: item.data.reduce((sum, d) => {
          return sum = sum + d.count;
        }, 0)
      });
    }).map(item => {
       return Object.assign({}, item, {
         percentage: (item.count / this.totalCount) * 100
      });
   });
    this.graphData[groupBy] = this.updateShape;

    // console.log(this.graphData[groupBy].length);
  }
onPrint(){
  window.print();
}
}
