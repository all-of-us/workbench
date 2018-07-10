import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {async, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';
import {DataBrowserService} from 'publicGenerated';
import {DataBrowserModule} from '../../data-browser/data-browser.module';
/* TODO testing maybe
import {ChartComponent} from '../../data-browser/chart/chart.component';
import {ConceptChartsComponent} from '../../data-browser/concept-charts/concept-charts.component';

import { HighlightSearchPipe } from '../../utils/highlight-search.pipe';
import { DbHeaderComponent } from '../db-header/db-header.component';
import { DbHomeComponent } from '../db-home/db-home.component';
import { EhrViewComponent } from '../ehr-view/ehr-view.component';
import { PhysicalMeasurementsComponent } from '../pm/pm.component';
import { QuickSearchComponent } from '../quick-search/quick-search.component';
import { SurveyViewComponent } from '../survey-view/survey-view.component';
import { SurveysComponent } from '../surveys/surveys.component';*/
import {AppComponent} from './app.component';

describe('AppComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot(),
        DataBrowserModule
      ],
      declarations: [
        AppComponent,
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        {provide: DataBrowserService, useValue: {}}
      ] }).compileComponents();
  }));

  /* TODO Testing maybe Peter
  it('should create the app', async(() => {
   */
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }); )
  * /;

})
