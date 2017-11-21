import { Component, OnInit, Input } from '@angular/core';
import { AchillesService } from '../services/achilles.service';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Location } from '@angular/common';
import 'rxjs/add/operator/switchMap';
import { Analysis, AnalysisSection, IAnalysis, IAnalysisResult } from '../analysisClasses';
import { AnalysisResult } from '../analysisClasses';
import { AnalysisDist } from '../analysisSubClasses'
import * as highcharts from 'highcharts';
import { LocalStorageService } from 'angular-2-local-storage';

@Component({
  selector: 'app-achilles-detail',
  templateUrl: './achilles-detail.component.html',
  styleUrls: ['./achilles-detail.component.css']
})
export class AchillesDetailComponent implements OnInit {
  analyses: IAnalysis[];
  // distribution : AnalysisDist.results[];
  chartSeries = [];
  section: AnalysisSection;
  alreadyRan = [];
  chartOptions = {};
  routeId;
  redraw;
  hidden = false;


  constructor(
    private achillesService: AchillesService,
    private route: ActivatedRoute,
    private location: Location,
    private localStorageService: LocalStorageService

  ) {
    // Get section from section id in route
    this.route.params.subscribe(params => {
      this.achillesService.getSections('data-browser')
        .then(data => {
          this.section = data[0];
          /* todo   for (let s of data) {
              if (s.name === this.routeId) {
                this.section = s;
                break;
              }
            } */
        });
    });

  }


  ngOnInit() {
    /* Load up the data */
    /*this.route.paramMap
      .switchMap((params: ParamMap) => this.achillesService.getSectionAnalyses(+params.get('id')))
      .then(analyses => {
        this.analyses = analyses;
        for (let a of this.analyses) {
          //
          //
        }
        // //
      });
      */

  }


  runAnalysis(analysis: IAnalysis): void {
    //
    //   // make it an array of numebrs:
    // var numbersArr=  analysis.stratum.toString().split(',').map(Number)
    // //
    //
    // // push onto array, then make the array only contain unique numbers.
    // for (let i = 0; i < numbersArr.length; i++) {
    //     this.alreadyRan.push(numbersArr[i]);
    // }
    // //
    // var unique = this.alreadyRan.filter(function(itm, i, a) {
    //   return i == a.indexOf(itm);
    // });
    // this.alreadyRan = unique;
    // //
    // // assign input to a variable
    // //can now display variable

    //
    //
    this.achillesService.getAnalysisResults(analysis)
      .then(results => {
        analysis.results = results;
        analysis.status = 'Done';
        // Toggle redraw so that the app-chart component gets a change and redraws the map
        this.redraw = !this.redraw;
        //
      });

  }

}
