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
    {conceptId: '903118', conceptName: 'Systlic mean bp', analyses: null},
    {conceptId: '903115', conceptName: 'Diastolic mean bp', analyses: null },
    {conceptId: '903133', conceptName: 'Height', analyses: null },
    {conceptId: '903121', conceptName: 'Weight', analyses: null },
    {conceptId: '903135', conceptName: 'Mean waist circumference', analyses: null },
    {conceptId: '903136', conceptName: 'Mean hip circumference', analyses: null },
    {conceptId: '903126', conceptName: 'Mean heart rate', analyses: null },
    {conceptId: '903111', conceptName: 'wheel chair use', analyses: null },
    {conceptId: '903120', conceptName: 'Pregnancy', analyses: null }
  ];
  selectedConcept = this.concepts[0];

  measurement: MeasurementAnalysisListResponse;

  constructor(private api: DataBrowserService) { }

  ngOnInit() {
    this.subscriptions.push(this.api.getMeasurementAnalysisResults(this.selectedConcept.conceptId)
      .subscribe( result => {
        this.selectedConcept.analyses = result;
        console.log(result);
      }));
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  showMeasurement(concept:any) {
    this.selectedConcept = concept;
    if (!this.selectedConcept.analyses) {
      this.subscriptions.push(this.api.getMeasurementAnalysisResults(this.selectedConcept.conceptId)
        .subscribe(result => {
          this.selectedConcept.analyses = result;
          console.log(result);
      }));
    }

  }

}
