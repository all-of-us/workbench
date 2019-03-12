import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {currentWorkspaceStore, NavStore} from 'app/utils/navigation';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspace-storage-service-stub';
import {PageLayout} from './page-layout';

import {CohortReviewService, CohortsService} from 'generated';

describe('PageLayout', () => {
  let component: PageLayout;
  let fixture: ComponentFixture<PageLayout>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [
        CreateReviewPage,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {provide: CohortReviewService, useValue: new CohortReviewServiceStub()},
        {provide: CohortsService, useValue: new CohortsServiceStub()},
      ],
    })
      .compileComponents();
    NavStore.navigate = jasmine.createSpy('navigate');
    cohortReviewStore.next(cohortReviewStub);
    currentWorkspaceStore.next(workspaceDataStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PageLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
