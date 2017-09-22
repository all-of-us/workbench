import {Component, OnInit, ViewChild, OnDestroy} from '@angular/core';
import 'rxjs/add/operator/takeWhile';

import {BroadcastService} from '../broadcast.service';
import {GoogleChartDirective} from '../google-chart/google-chart.directive';

@Component({
  selector: 'app-race-chart',
  templateUrl: 'race-chart.component.html',
  styleUrls: ['race-chart.component.css']
})
export class RaceChartComponent implements OnInit, OnDestroy {

  @ViewChild(GoogleChartDirective) raceChartDirective: GoogleChartDirective;
  public type = 'PieChart';
  private alive = true;

  public data = [
    ['Race', 'Count Per'],
    ['Unknown', 0]];

  public options  = {
    title: 'Results By Race',
    chartArea: {width: '80%'},
    width: '100%',
    height: '300'
  };

  constructor(private broadcastService: BroadcastService) { }

  ngOnInit() {
    this.broadcastService.updatedCharts$
      .takeWhile(() => this.alive)
      .subscribe(change => {
        this.redraw(change.race);
      });
  }

  redraw(data: any) {
    this.raceChartDirective.drawGraph(this.options,
        this.type,
        data,
        this.raceChartDirective._element);
  }

  ngOnDestroy() {
    this.alive = false;
  }
}
