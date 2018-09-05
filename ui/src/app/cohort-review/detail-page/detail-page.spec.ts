import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {AnnotationType, CohortAnnotationDefinition, CohortReviewService} from 'generated';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {AnnotationItemComponent} from '../annotation-item/annotation-item.component';
import {AnnotationListComponent} from '../annotation-list/annotation-list.component';
import {DetailAllEventsComponent} from '../detail-all-events/detail-all-events.component';
import {DetailHeaderComponent} from '../detail-header/detail-header.component';
import {DetailTabTableComponent} from '../detail-tab-table/detail-tab-table.component';
import {DetailTabsComponent} from '../detail-tabs/detail-tabs.component';
import {ParticipantStatusComponent} from '../participant-status/participant-status.component';
import {ReviewStateService} from '../review-state.service';
import {SidebarContentComponent} from '../sidebar-content/sidebar-content.component';
import {DetailPage} from './detail-page';

describe('DetailPage', () => {
  let component: DetailPage;
  let fixture: ComponentFixture<DetailPage>;
  const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        AnnotationItemComponent,
        AnnotationListComponent,
        DetailAllEventsComponent,
        DetailHeaderComponent,
        DetailPage,
        DetailTabsComponent,
        DetailTabTableComponent,
        ParticipantStatusComponent,
        SidebarContentComponent,
      ],
      imports: [ClarityModule, NgxPopperModule, ReactiveFormsModule],
      providers: [
        {provide: ActivatedRoute, useValue: {data: Observable.of({
              participant: {},
              annotations: [],
            })
        }},
        {provide: CohortReviewService, useValue: {}},
        {provide: ReviewStateService, useValue: {
          review$: Observable.of({
            participantCohortStatuses: []
          }),
          annotationDefinitions$: Observable.of([
            <CohortAnnotationDefinition> {
              cohortAnnotationDefinitionId: 1,
              cohortId: 2,
              columnName: 'test',
              annotationType: AnnotationType.BOOLEAN
            }
          ]),
        }},
        {provide: Router, useValue: routerSpy},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailPage);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
