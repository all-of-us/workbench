import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {MeasurementAnalysisListResponse} from '../../../publicGenerated/model/measurementAnalysisListResponse';

@Component({
  selector: 'app-physical-measurements',
  templateUrl: './pm.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './pm.component.css']
})
export class PhysicalMeasurementsComponent implements OnInit, OnDestroy {
  title = 'Browse Program Physical Measurements';
  private subscriptions: ISubscription[] = [];
  conceptGroups = [
    { group: 'blood-pressure', groupName: 'Mean Blood Pressure', concepts: [
      {conceptId: '903118', conceptName: 'Systolic ', analyses: null, conceptAnalyses: null},
      {conceptId: '903115', conceptName: 'Diastolic', analyses: null, conceptAnalyses: null},
    ]},
    { group: 'height', groupName: 'Height', concepts: [
      {conceptId: '903133', conceptName: 'Height', analyses: null, conceptAnalyses: null }
    ]},
    { group: 'weight', groupName: 'Weight', concepts: [
      {conceptId: '903121', conceptName: 'Weight', analyses: null, conceptAnalyses: null },
    ]},
    { group: 'mean-waist', groupName: 'Mean waist circumference', concepts: [
      { conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null,
        conceptAnalyses: null },
    ]},
    { group: 'mean-waist', groupName: 'Mean hip circumference', concepts: [
      {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null,
        conceptAnalyses: null },
    ]},
    { group: 'mean-waist', groupName: 'Mean heart rate', concepts: [
      {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null,
        conceptAnalyses: null },
    ]},
    { group: 'mean-waist', groupName: 'Wheel chair use', concepts: [
      {conceptId: '903111', conceptName: 'wheel chair use', analyses: null,
        conceptAnalyses: null },
    ]},
    { group: 'mean-waist', groupName: 'Pregnancy', concepts: [
      {conceptId: '903120', conceptName: 'Pregnancy', analyses: null,
        conceptAnalyses: null },
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
      this.subscriptions.push(this.api.getMeasurementAnalysisResults(
        [this.selectedConcept.conceptId])
        .subscribe(result => {
          this.selectedConcept.analyses = result.items[0];
          console.log(result);
          this.loading = false;
      }));
    } else {
      console.log('already have analyses ', this.selectedConcept.analyses);
    }
    if (!this.selectedConcept.conceptAnalyses) {
      this.loading = true;
      this.subscriptions.push(this.api.getConceptAnalysisResults([this.selectedConcept.conceptId])
        .subscribe( result => {
          this.selectedConcept.conceptAnalyses = result.items[0];
          this.loading = false;
        }));
    } else {
      console.log('already have concept analyses ', this.selectedConcept.conceptAnalyses);
    }

  }

}
