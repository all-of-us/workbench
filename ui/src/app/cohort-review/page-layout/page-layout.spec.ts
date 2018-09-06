import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {CohortAnnotationDefinitionService, CohortReview} from 'generated';
import {ReviewStateServiceStub} from 'testing/stubs/review-state-service-stub';

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
        {provide: ReviewStateService, useValue: new ReviewStateServiceStub()},
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
