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
  @Input() backgroundColor = '#ECF1F4'; // background color to pass to the chart component
  results;
  loading = false;
  ageAnalysis = null;
  genderAnalysis = null;
  selectedResult;
  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    this.loading = true;
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.api.getConceptAnalysisResults([conceptIdStr]).subscribe(results =>  {
      this.results = results.items;
      console.log(this.results);
      this.ageAnalysis = this.results[0].ageAnalysis;
      this.genderAnalysis = this.results[0].genderAnalysis;

      this.loading = false;
    } );
  }

}
