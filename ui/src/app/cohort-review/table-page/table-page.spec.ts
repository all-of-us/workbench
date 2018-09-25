import {NgRedux} from '@angular-redux/store';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortAnnotationDefinitionService, CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {CohortReviewServiceStub} from 'testing/stubs/cohort-review-service-stub';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import  {OverviewPage} from "../overview-page/overview-page";
import {ChoiceFilterComponent} from '../choice-filter/choice-filter.component';
import {ReviewNavComponent} from '../review-nav/review-nav.component';
import {ReviewStateService} from '../review-state.service';
import {SetAnnotationCreateComponent} from '../set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from '../set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from '../set-annotation-modal/set-annotation-modal.component';
import {StatusFilterComponent} from '../status-filter/status-filter.component';
import {TablePage} from './table-page';
import { APP_BASE_HREF } from '@angular/common';
import {CohortSearchActions} from "../../cohort-search/redux";
import {CohortBuilderService} from "../../../generated";
import {CohortBuilderServiceStub} from "../../../testing/stubs/cohort-builder-service-stub";
describe('TablePage', () => {
  let component: TablePage;
  let fixture: ComponentFixture<TablePage>;

  const activatedRouteStub = {
      parent: {
      snapshot: {
          data: {
              concepts: {
                  raceList: [],
                  genderList: [],
                  ethnicityList: [],
              }
          },
          pathFromRoot: [{data: {workspace: {cdrVersionId: 1}}}]
      }
  }

  };
  let route;


    // const activatedRouteStub = {
    //     parent: {
    //         snapshot: {
    //             data: {
    //                 workspace: {
    //                     cdrVersionId: 1
    //                 },
    //             },
    //             data: {
    //                 concepts: {
    //                     raceList: [{
    //                         conceptId: 38003565,
    //                         conceptName: "Hispanic or Latino"
    //                     },
    //                         {
    //                             conceptId: 38003566,
    //                             conceptName: "Not Hispanic or Latino"
    //                         }],
    //                     genderList: [{
    //                         conceptId: 38003567,
    //                         conceptName: "FEMALE"
    //                     }, {
    //                         conceptId: 38003568,
    //                         conceptName: "MALE"
    //                     }],
    //                     ethnicityList: [{
    //                         conceptId: 38003569,
    //                         conceptName: "Hispanic or Latino"
    //                     }, {
    //                         conceptId: 38003570,
    //                         conceptName: "Not Hispanic or Latino"
    //                     }],
    //                 }
    //             }
    //         }
    //     },
    // };

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        ChoiceFilterComponent,
        ReviewNavComponent,
        TablePage,
        SetAnnotationCreateComponent,
        SetAnnotationItemComponent,
        SetAnnotationListComponent,
        SetAnnotationModalComponent,
        StatusFilterComponent,
        ComboChartComponent
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule, NgxPopperModule, NgxChartsModule],
      providers: [
        {provide: NgRedux},
        {provide: CohortReviewService},
        {provide: CohortSearchActions},
        {provide: APP_BASE_HREF, useValue: '/'},
        {provide: CohortBuilderService, useValue: new CohortBuilderServiceStub()},
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TablePage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
