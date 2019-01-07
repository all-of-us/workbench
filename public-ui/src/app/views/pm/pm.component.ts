import { Component, OnDestroy, OnInit } from '@angular/core';
import { ISubscription } from 'rxjs/Subscription';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {AchillesResult} from '../../../publicGenerated/model/achillesResult';
import {Analysis} from '../../../publicGenerated/model/analysis';
import {ConceptGroup} from '../../utils/conceptGroup';
import {ConceptWithAnalysis} from '../../utils/conceptWithAnalysis';
import {DbConfigService} from '../../utils/db-config.service';

@Component({
  selector: 'app-physical-measurements',
  templateUrl: './pm.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './pm.component.css']
})
export class PhysicalMeasurementsComponent implements OnInit, OnDestroy {
  title = 'Browse Program Physical Measurements';
  pageImage = '/assets/db-images/man-standing.png';
  private subscriptions: ISubscription[] = [];
  loadingStack: any = [];

  // Todo put constants in a class for use in other views
  chartType = 'histogram';

  // Total analyses
  genderAnalysis: Analysis = null;
  raceAnalysis: Analysis = null;
  ethnicityAnalysis: Analysis = null;

  // Get the physical measurement groups array we display here
  conceptGroups: ConceptGroup[];
  // Initialize to first group and concept, adjust order in groups array above
  selectedGroup: ConceptGroup;
  selectedConcept: ConceptWithAnalysis;

  // we save the total gender counts
  femaleCount = 0;
  maleCount = 0;
  otherCount = 0;

  constructor(private api: DataBrowserService, public dbc: DbConfigService) {

  }

  loading() {
    return this.loadingStack.length > 0;
  }

  ngOnInit() {
    this.loadingStack.push(true);
    this.dbc.getPmGroups().subscribe(results => {
      this.conceptGroups = results;
      this.selectedGroup = this.conceptGroups[0];
      this.selectedConcept = this.selectedGroup.concepts[0];
      this.loadingStack.pop();
    });

    // Get demographic totals
    this.loadingStack.push(true);
    this.subscriptions.push(this.api.getRaceAnalysis()
      .subscribe({
          next: result => {
            this.raceAnalysis = result;
            this.loadingStack.pop();
          },
          error: err =>  {
            this.loadingStack.pop();
            console.log('Error: ', err);
          }
      }));

    this.loadingStack.push(true);
    this.subscriptions.push(this.api.getEthnicityAnalysis()
      .subscribe({
        next: result => {
          this.ethnicityAnalysis = result;
          this.loadingStack.pop();
        },
        error: err =>  {
          this.loadingStack.pop();
          console.log('Error: ', err);
        }
      }));
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  showMeasurement(group: any, concept: any) {
    this.selectedGroup = group;
    this.selectedConcept = concept;
  }
}
