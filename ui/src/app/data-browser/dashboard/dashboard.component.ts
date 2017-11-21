import { Component, OnInit, Input } from '@angular/core';
import { Analysis, AnalysisSection, IAnalysis, IAnalysisResult } from '../analysisClasses';
import { AchillesService } from '../services/achilles.service';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Location } from '@angular/common';


@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  section
  sections: AnalysisSection[];
  analyses
  redraw
  dontChart = [1, 12]
  dataToggle

  constructor(private achillesService: AchillesService) {
    this.achillesService.getSections('data-browser')
      .then(data => {
        this.sections = data;
        this.section = this.sections[0]
        //
        //


        if (this.section) {
          this.achillesService.getSectionAnalyses(this.section.section_id, this.section.analyses)
            .then(analyses => {

              this.analyses = analyses;

              //
              for (let b of this.analyses) {
                // //
                // getAnalysisResults to get r


                this.achillesService.getAnalysisResults(b)
                  .then(results => {
                    b.results = results;
                    b.status = 'Done';
                    //
                    // Toggle redraw so that the app-chart component gets a change and redraws the map
                    this.redraw = !this.redraw;


                  });//end of .then
              }
            });//end of .subscribe

        }
      });
  }

  ngOnInit() {

  }

  dontShow(id) {
    if (this.dontChart.indexOf(id) == -1) {
      return true
    } else
      return false
  }
  dataSelected() {
    this.dataToggle = !this.dataToggle;
    if (this.dataToggle) {
      //
    }
    else {
      //
    }
  }



}
