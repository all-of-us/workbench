import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {ConceptAnalysis} from '../../../publicGenerated/model/conceptAnalysis';
import {ConceptAnalysisListResponse} from '../../../publicGenerated/model/conceptAnalysisListResponse';

@Component({
  selector: 'app-physical-measurements',
  templateUrl: './pm.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './pm.component.css']
})
export class PhysicalMeasurementsComponent implements OnInit, OnDestroy {
  title = 'Browse Program Physical Measurements';
  private subscriptions: ISubscription[] = [];
  chartType = 'histogram';
  MALE_GENDER_ID = '8507';
  FEMALE_GENDER_ID = '8532';
  PREGNANCY_CONCEPT_ID = '903120';
  WHEEL_CHAIR_CONCEPT_ID = '903111';
  conceptGroups = [
    { group: 'blood-pressure', groupName: 'Mean Blood Pressure', concepts: [
      {conceptId: '903118', conceptName: 'Systolic ', analyses: null, chartType: this.chartType},
      {conceptId: '903115', conceptName: 'Diastolic', analyses: null, chartType: this.chartType},
    ]},
    { group: 'height', groupName: 'Height', concepts: [
      {conceptId: '903133', conceptName: 'Height', analyses: null, chartType: this.chartType }
    ]},
    { group: 'weight', groupName: 'Weight', concepts: [
      {conceptId: '903121', conceptName: 'Weight', analyses: null, chartType: this.chartType },
    ]},
    { group: 'mean-waist', groupName: 'Mean waist circumference', concepts: [
      { conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null,
        chartType: this.chartType},
    ]},
    { group: 'mean-hip', groupName: 'Mean hip circumference', concepts: [
      {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null, chartType: this.chartType},
    ]},
    { group: 'mean-heart-rate', groupName: 'Mean heart rate', concepts: [
      {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null, chartType: this.chartType},
    ]},
    { group: 'wheel-chair', groupName: 'Wheel chair use', concepts: [
      {conceptId: '903111', conceptName: 'Wheel chair use', analyses: null, chartType: 'column' },
    ]},
    { group: 'pregnancy', groupName: 'Pregnancy', concepts: [
      {conceptId: '903120', conceptName: 'Pregnancy', analyses: null, chartType: 'column'},
    ]}
  ];

  loading = false;

  // Initialize to first group and concept, adjust order in groups array above
  selectedGroup = this.conceptGroups[0];
  selectedConcept = this.selectedGroup.concepts[0];


  selectedFemaleCount = 0;
  selectedMaleCount = 0;
  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    this.showMeasurement(this.selectedGroup, this.selectedConcept);
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  showMeasurement(group: any, concept: any) {
    console.log(group, concept);
    this.selectedGroup = group;
    this.selectedConcept = concept;
    if (!this.selectedConcept.analyses) {
      this.loading = true;
      this.subscriptions.push(this.api.getConceptAnalysisResults(
        [this.selectedConcept.conceptId])
        .subscribe(result => {
          this.selectedConcept.analyses = result.items[0];
          console.log('Before organize' , this.selectedConcept.analyses);
          // Organize, massage the data for ui graphing, for example, pregnant has only 1 result for pregnant,
          // we add a not pregnant to make display better
          this.arrangeConceptAnalyses(this.selectedConcept);

          this.loading = false;
      }));
    } else {
      console.log('already have analyses ', this.selectedConcept.analyses);
    }
  }

  arrangeConceptAnalyses(concept) {
    if (concept.analyses.genderAnalysis) {
      concept.analyses.genderAnalysis.results =
        this.organizeGenders(concept.analyses.genderAnalysis);
    }

    if (concept.conceptId === this.PREGNANCY_CONCEPT_ID) {
      // Delete any male results so we don't look dumb with dumb data
      concept.analyses.measurementValueMaleAnalysis = null;
      let femaleCount = concept.analyses.genderAnalysis.results[0];
      // Add not pregnant result to the female value results because this concept is just a one value Yes
      let results = concept.analyses.measurementValueFemaleAnalysis.results;
      console.log('female results',results, femaleCount);
      let pregnantCount = results[0].countValue;
      let notPregnantResult = results[0];
      notPregnantResult.stratum4 = 'Not Pregnant';
      notPregnantResult.countValue = femaleCount.countValue - pregnantCount;

      //results.push(notPregnantResult);

    }
    if (concept.conceptId === this.WHEEL_CHAIR_CONCEPT_ID) {

    }

  }
  // Put the gender analysis in the order we want to show them
  organizeGenders(analysis) {

    let male = null;
    let female = null;
    let others = [];

    let results = [];
    for (const g of analysis.results) {
      if (g.stratum2 === this.MALE_GENDER_ID) {
        male = g;
      } else if (g.stratum2 === this.FEMALE_GENDER_ID) {
        female = g;
      } else {
        others.push(g);
      }
    }
    // Order like Male, Female , Others
    if (male) { results.push(male); }
    if (female) { results.push(female); }

    // Todo, sum up others
    results.concat(others);
    return results;
  }

}
