import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'angular2-highcharts';
import { MapService } from '../services/map.service';
import { AchillesService } from '../services/achilles.service';
import { PersonService } from '../services/person.service'; // temporary service location to make US population call

import { AnalysisMap } from "../analysisMap";


//import * as Highcharts from 'highcharts';
declare var require: any;
//require ('highcharts/modules/map')(Highcharts);

//import '../../../assets/highcharts.mapdata.countries.us.us-all.js';



//const Highcharts = require('highcharts');
@Component({
  selector: 'app-geo-chart',
  templateUrl: './geo-chart.component.html',
  styles: [`
      chart {
        display: block;
      }
    `]

})
export class GeoChartComponent implements OnInit {
  options: any;
  chart;
  data: any;
  change = false;

  analysis = new AnalysisMap({
    'analysis_id': 1101,
    'analysis_name': 'Person by state',
    'dataType': 'counts',
    'chartType': 'map',
    'results': [],
  });
  saveInstance(chartInstance) {
    this.chart = chartInstance;
  }
  constructor(
    private mapService: MapService,
    private achillesService: AchillesService,
    private personService: PersonService, // temporary service location to make US population call
  ) {
    this.achillesService.getAnalysisResults(this.analysis)
      .then(results => {
        this.analysis.results = results;
        this.analysis.status = 'Done';

      });
  }
  ngOnChanges() {

  }
  ngOnInit() {
  }
}
