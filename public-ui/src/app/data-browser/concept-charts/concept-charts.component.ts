import { Component, Input, OnInit } from '@angular/core';
import {Concept} from '../../../publicGenerated/model/concept';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
@Component({
  selector: 'app-concept-charts',
  templateUrl: './concept-charts.component.html',
  styleUrls: ['./concept-charts.component.css']
})
export class ConceptChartsComponent implements OnInit {
  @Input() concept: Concept;
  results;
  loading = false;
  ageAnalysis = null;
  genderAnalysis = null;
  selectedResult;
  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    this.loading = true;
    this.api.getSurveyResults('1585855').subscribe(results =>  {
      this.results = results.items;
      console.log(this.results);
      this.ageAnalysis = this.results[0].ageAnalysis;
      this.selectedResult = this.results.countAnalysis.results[0]
      this.genderAnalysis = this.results[0].genderAnalysis;

      this.loading = false;
    } );
  }

}
