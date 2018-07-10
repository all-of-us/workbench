import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import {ISubscription} from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Concept} from '../../../publicGenerated/model/concept';
import {ChartComponent} from "../chart/chart.component";


@Component({
  selector: 'app-concept-charts',
  templateUrl: './concept-charts.component.html',
  styleUrls: ['./concept-charts.component.css']
})
export class ConceptChartsComponent implements OnInit, OnDestroy {
  @Input() concept: Concept;
  @Input() backgroundColor = '#ECF1F4'; // background color to pass to the chart component
  @Input() showSources = true;
  @Input() showGender = true;
  @Input() showAge = true;
  results;
  loading = false;
  ageAnalysis = null;
  genderAnalysis = null;
  sourceConcepts = null;

  private subscription: ISubscription;
  private subscription2: ISubscription;

  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    // Get chart results for concept
    this.loading = true;
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.subscription = this.api.getConceptAnalysisResults([conceptIdStr]).subscribe(results =>  {
      this.results = results.items;
      this.ageAnalysis = this.results[0].ageAnalysis;
      this.genderAnalysis = this.results[0].genderAnalysis;
      this.loading = false;
    });

    this.getSourceConcepts();
  }

  ngOnDestroy() {
    console.log('unsubscribing concept-charts');
    this.subscription.unsubscribe();
    this.subscription2.unsubscribe();
  }

  getSourceConcepts() {
    this.subscription2 = this.api.getSourceConcepts(this.concept.conceptId).subscribe(
      results => this.sourceConcepts = results.items
      );

  }
}
