import {Component, Input, OnChanges} from '@angular/core';

@Component({
  selector: 'app-participants-charts',
  templateUrl: './participant-charts.html',
  styleUrls: ['./participant-charts.css'],

})
export class ParticipantsChartsComponent implements  OnChanges {

  @Input() chartItems = [];
  @Input() totalCount: any;
  @Input() domainName = '';
  constructor() {}

  ngOnChanges() {
    if (this.chartItems) {
      this.chartItems.forEach(itemCount => {
        const percentCount = ((itemCount.count / this.totalCount) * 100);
        Object.assign(itemCount, {percentCount: percentCount});
      });
    }
  }

}

