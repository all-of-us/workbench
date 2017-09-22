import {Component, OnInit, ViewChild, Input, OnDestroy} from '@angular/core';
import { GoogleChartDirective } from '../google-chart/google-chart.directive';
import 'rxjs/add/operator/takeWhile';
import { BroadcastService } from '../broadcast.service';

@Component({
  selector: 'app-gender-chart',
  templateUrl: 'gender-chart.component.html',
  styleUrls: ['gender-chart.component.css']
})
export class GenderChartComponent implements OnInit, OnDestroy {

  @ViewChild(GoogleChartDirective) genderChartDirective: GoogleChartDirective;
  public type = 'BarChart';
  private alive = true;

  public data = [
    ['Gender', 'Count', { role: 'style' }],
    ['Unknown', 0, 'gray']
  ];

  public options = {
    title: 'Results By Gender',
    chartArea: {width: '80%'},
    isStacked: true,
    legend: { position: 'none' },
    hAxis: {
      title: 'Total Count',
      minValue: 0,
      width: '100%',
      height: '300'
    }
  };

  constructor(private broadcastService: BroadcastService) {}

  ngOnInit() {
    this.broadcastService.updatedCharts$
      .takeWhile(() => this.alive)
      .subscribe(change => {
        this.redraw(change.gender);
      });
  }

  redraw(data: any) {
    this.genderChartDirective.drawGraph(this.options,
        this.type,
        data,
        this.genderChartDirective._element);
  }

  ngOnDestroy() {
    this.alive = false;
  }

}
