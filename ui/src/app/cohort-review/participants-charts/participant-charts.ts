import {Component, Input, OnChanges} from '@angular/core';

@Component({
  selector: 'app-participants-charts',
  templateUrl: './participant-charts.html',
  styleUrls: ['./participant-charts.css'],

})
export class ParticipantsCharts implements  OnChanges {

  @Input() chartItems = [];
  @Input() totalCount: any;
  constructor() {}

  ngOnChanges() {
    if (this.chartItems) {
      console.log('here')
      this.chartItems.forEach(itemCount => {
        const percentCount = ((itemCount.count / this.totalCount) * 100);
        Object.assign(itemCount, {percentCount: percentCount});
      });
    }
  }

}

