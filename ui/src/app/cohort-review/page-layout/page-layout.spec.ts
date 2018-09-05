import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {AnnotationType, Cohort, CohortAnnotationDefinition, CohortAnnotationDefinitionService, CohortReview} from 'generated';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {ReviewStateService} from '../review-state.service';
import {SetAnnotationCreateComponent} from '../set-annotation-create/set-annotation-create.component';
import {SetAnnotationItemComponent} from '../set-annotation-item/set-annotation-item.component';
import {SetAnnotationListComponent} from '../set-annotation-list/set-annotation-list.component';
import {SetAnnotationModalComponent} from '../set-annotation-modal/set-annotation-modal.component';
import {PageLayout} from './page-layout';

describe('PageLayout', () => {
  let component: PageLayout;
  let fixture: ComponentFixture<PageLayout>;
  const routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);
  let route;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        PageLayout,
        SetAnnotationCreateComponent,
        SetAnnotationItemComponent,
        SetAnnotationListComponent,
        SetAnnotationModalComponent
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {
          provide: ReviewStateService, useValue: {
            annotationDefinitions: new ReplaySubject<CohortAnnotationDefinition[]>(1),
            annotationManagerOpen: new BehaviorSubject<boolean>(false),
            editAnnotationManagerOpen: new BehaviorSubject<boolean>(false),
            cohort: new ReplaySubject<Cohort>(1),
            review: new ReplaySubject<CohortReview>(1),
            annotationDefinitions$: Observable.of([
              <CohortAnnotationDefinition> {
                cohortAnnotationDefinitionId: 1,
                cohortId: 2,
                columnName: 'test',
                annotationType: AnnotationType.BOOLEAN
              }
            ]),
          }
        },
        {provide: ActivatedRoute, useValue: {snapshot: {data: {review: <CohortReview> {}}}}},
        {provide: CohortAnnotationDefinitionService, useValue: {}},
        {provide: Router, useValue: routerSpy},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PageLayout);
    component = fixture.componentInstance;
    route = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
