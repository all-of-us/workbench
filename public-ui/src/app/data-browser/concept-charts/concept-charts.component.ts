import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import {ISubscription} from 'rxjs/Subscription';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';

import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {Concept} from '../../../publicGenerated/model/concept';
import {ConceptAnalysis} from '../../../publicGenerated/model/conceptAnalysis';
import {ConceptWithAnalysis} from '../../utils/conceptWithAnalysis';
import {DbConfigService} from '../../utils/db-config.service';
import {GraphType} from '../../utils/enum-defs';

@Component({
  selector: 'app-concept-charts',
  templateUrl: './concept-charts.component.html',
  styleUrls: ['./concept-charts.component.css']
})
export class ConceptChartsComponent implements OnChanges, OnInit, OnDestroy {
  @Input() concept: Concept;
  @Input() backgroundColor = '#ECF1F4'; // background color to pass to the chart component
  @Input() showGraph = GraphType.None;
  @Input() showRace = false;
  @Input() showEthnicity = false;

  private subscriptions: ISubscription[] = [];
  loadingStack: any = [];
  results;
  maleGenderResult: AchillesResult;
  femaleGenderResult: AchillesResult;
  intersexGenderResult: AchillesResult;
  noneGenderResult: AchillesResult;
  otherGenderResult: AchillesResult;
  maleGenderChartTitle =  '';
  femaleGenderChartTitle = '';
  intersexGenderChartTitle = '';
  noneGenderChartTitle = '';
  otherGenderChartTitle = '';
  sourceConcepts: Concept[] = null;
  analyses: ConceptAnalysis;
  unitNames: string[] = [];
  selectedUnit: string;
  genderResults: AchillesResult[] = [];
  displayMeasurementGraphs = false;
  toDisplayMeasurementGenderAnalysis: Analysis;
  toDisplayMeasurementGenderCountAnalysis: Analysis;
  graphType = GraphType;

  constructor(private api: DataBrowserService, public dbc: DbConfigService) { }

  loading() {
    return this.loadingStack.length > 0;
  }

  ngOnInit() {
    // Get chart results for concept
    this.loadingStack.push(true);
    const conceptIdStr = '' + this.concept.conceptId.toString();
    this.subscriptions.push( this.api.getConceptAnalysisResults([conceptIdStr],
      this.concept.domainId).subscribe(
    results =>  {
      this.results = results.items;
      this.analyses = results.items[0];
      this.organizeGenders(this.analyses.genderAnalysis);
      this.fetchMeasurementGenderResults();
      // Set this var to make template simpler. We can just loop through the results and show bins
      this.loadingStack.pop();
    }));

    this.loadingStack.push(true);
    this.subscriptions.push( this.api.getSourceConcepts(this.concept.conceptId).subscribe(
    results => {
      this.sourceConcepts = results.items;
      if (this.sourceConcepts.length > 10) {
        this.sourceConcepts = this.sourceConcepts.slice(0, 10);
      }
      this.loadingStack.pop();
      }));
  }

  public fetchMeasurementGenderResults() {
    if (!this.analyses) {
      return;
    }
    if (this.showGraph === GraphType.MeasurementBins) {
      this.genderResults = this.analyses.genderAnalysis.results;
    }
    this.unitNames = [];
    if (this.analyses && this.analyses.measurementValueGenderAnalysis
      && this.showGraph === GraphType.MeasurementBins) {
      this.displayMeasurementGraphs = true;
      for (const aa of this.analyses.measurementValueGenderAnalysis) {
        this.unitNames.push(aa.unitName);
      }
      this.showMeasurementGenderHistogram(this.unitNames[0]);
    }
  }
  ngOnChanges() {
    if (this.showGraph !== GraphType.MeasurementBins) {
      this.displayMeasurementGraphs = false;
    } else {
      this.displayMeasurementGraphs = true;
      this.fetchMeasurementGenderResults();
    }
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  // Organize genders and set the chart title for the gender charts for simple display
  organizeGenders(analysis: Analysis) {
      // No need to do anything if only one gender
    if (!analysis && analysis.results.length <= 1) {
      return;
    }
    const results = [];
    for (const g of analysis.results) {
      const chartTitle = g.analysisStratumName
        + ' - ' + g.countValue.toLocaleString();
      if (g.stratum2 === this.dbc.MALE_GENDER_ID) {
        this.maleGenderResult = g;
        this.maleGenderChartTitle = chartTitle;
      } else if (g.stratum2 === this.dbc.FEMALE_GENDER_ID) {
        this.femaleGenderResult = g;
        this.femaleGenderChartTitle = chartTitle;
      } else if (g.stratum2 === this.dbc.INTERSEX_GENDER_ID) {
        this.intersexGenderResult = g;
        this.intersexGenderChartTitle = chartTitle;
      } else if (g.stratum2 === this.dbc.NONE_GENDER_ID) {
        this.noneGenderResult = g;
        this.noneGenderChartTitle = chartTitle;
      } else if (g.stratum2 === this.dbc.OTHER_GENDER_ID) {
        this.otherGenderResult = g;
        this.otherGenderChartTitle = chartTitle;
      }
    }

    analysis.results = [];
    if (this.maleGenderResult) {
      analysis.results.push(this.maleGenderResult);
    }
    if (this.femaleGenderResult) {
      analysis.results.push(this.femaleGenderResult);
    }
    if (this.intersexGenderResult) {
      analysis.results.push(this.intersexGenderResult);
    }
    if (this.noneGenderResult) {
      analysis.results.push(this.noneGenderResult);
    }
    if (this.otherGenderResult) {
      analysis.results.push(this.otherGenderResult);
    }
  }

  showMeasurementGenderHistogram(unit: string) {
    this.selectedUnit = unit;
    this.toDisplayMeasurementGenderAnalysis = this.analyses.measurementValueGenderAnalysis.
    find(aa => aa.unitName === unit);
    if (this.analyses.measurementGenderCountAnalysis) {
      this.toDisplayMeasurementGenderCountAnalysis = this.analyses.measurementGenderCountAnalysis.
      find(aa => aa.unitName === unit);
    }
  }

  public fetchChartTitle(gender: any) {
    if (this.toDisplayMeasurementGenderCountAnalysis) {
      const genderResults = this.toDisplayMeasurementGenderCountAnalysis.results
        .filter(r => r.stratum3 === gender.stratum2)[0];
      if (genderResults) {
        return genderResults.countValue;
      } else {
        return 0;
      }
    } else {
      return gender.countValue;
    }
  }
}
