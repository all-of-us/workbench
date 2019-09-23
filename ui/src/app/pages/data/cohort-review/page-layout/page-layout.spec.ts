import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CreateReviewModalComponent} from 'app/pages/data/cohort-review/create-review-modal';
import {cohortReviewStore} from 'app/services/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {CohortBuilderService} from 'generated';
import {CohortBuilderApi, CohortReviewApi, CohortsApi, CriteriaListResponse} from 'generated/fetch';
import {Observable} from 'rxjs/Observable';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortReviewServiceStub, cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {PageLayout} from './page-layout';
class BuilderApiStub {
  getCriteriaBy(): Observable<CriteriaListResponse> {
    return Observable.of({items: []});
  }
}

describe('PageLayout', () => {
  let component: PageLayout;
  let fixture: ComponentFixture<PageLayout>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        CreateReviewModalComponent,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {provide: CohortBuilderService, useValue: new BuilderApiStub()}
      ],
    })
      .compileComponents();
    NavStore.navigate = jasmine.createSpy('navigate');
    cohortReviewStore.next(cohortReviewStubs[0]);
    currentWorkspaceStore.next(workspaceDataStub);
    urlParamsStore.next({
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(PageLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
