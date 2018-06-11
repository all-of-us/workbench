import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import {Concept} from '../../../publicGenerated/model/concept';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {ISubscription} from "rxjs/Subscription";


@Component({
  selector: 'app-concept-charts',
  templateUrl: './concept-charts.component.html',
  styleUrls: ['./concept-charts.component.css']
})
export class ConceptChartsComponent implements OnInit, OnDestroy {
  @Input() concept: Concept;
  @Input() backgroundColor = '#ECF1F4'; // background color to pass to the chart component
  results;
  loading = false;
  ageAnalysis = null;
  genderAnalysis = null;
  sourceConcepts = null;
  showSources = false;
  private subscription: ISubscription;
  private subscription2: ISubscription;

  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    this.loading = true;
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.subscription = this.api.getConceptAnalysisResults([conceptIdStr]).subscribe(results =>  {
      this.results = results.items;
      console.log(this.results);
      this.ageAnalysis = this.results[0].ageAnalysis;
      this.genderAnalysis = this.results[0].genderAnalysis;

      this.loading = false;
    } );

    this.getSourceConcepts();
  }

  ngOnDestroy() {
    console.log("unsubscribing concept-charts");
    this.subscription.unsubscribe();
    this.subscription2.unsubscribe();
  }
  toggleSourceConcepts() {
    // Get source concepts for first time if we don't have them
    if (this.sourceConcepts === null) {
      this.getSourceConcepts();
    }
    this.showSources = true;
  }
  getSourceConcepts() {
    this.subscription2 = this.api.getSourceConcepts(this.concept.conceptId).subscribe(
      results => this.sourceConcepts = results.items
      );

  }
}
