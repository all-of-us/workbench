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
export class PhysicalMeasurementsComponent implements OnInit {
  title = 'Browse Program Physical Measurements';
  private subscriptions: ISubscription[] = [];
  concepts = [
    {conceptId: '903118', conceptName: 'Systolic ', boxTitle: 'Mean Blood Pressure', analyses: null,
      conceptAnalyses: null},
    {conceptId: '903115', conceptName: 'Diastolic', boxTitle: 'Mean Blood Pressure', analyses: null, conceptAnalyses: null},
    {conceptId: '903133', conceptName: 'Height', analyses: null, conceptAnalyses: null },
    {conceptId: '903121', conceptName: 'Weight', analyses: null, conceptAnalyses: null },
    {conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null, conceptAnalyses: null },
    {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null, conceptAnalyses: null },
    {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null, conceptAnalyses: null },
    {conceptId: '903111', conceptName: 'wheel chair use', analyses: null, conceptAnalyses: null },
    {conceptId: '903120', conceptName: 'Pregnancy', analyses: null, conceptAnalyses: null }
  ];
  selectedConcept = this.concepts[0];
  femaleCount = 0;
  maleCount = 0;
  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    this.showMeasurement(this.selectedConcept);
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  showMeasurement(concept:any) {
    this.selectedConcept = concept;
    if (!this.selectedConcept.analyses) {
      this.subscriptions.push(this.api.getMeasurementAnalysisResults([this.selectedConcept.conceptId])
        .subscribe(result => {
          this.selectedConcept.analyses = result.items[0];
          console.log(result);
      }));
    }
    if (!this.selectedConcept.conceptAnalyses) {
      this.subscriptions.push(this.api.getConceptAnalysisResults([this.selectedConcept.conceptId])
        .subscribe( result => {
          this.selectedConcept.conceptAnalyses = result.items[0];
          console.log(this.selectedConcept);
        }));
    }

  }

}
