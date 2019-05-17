import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {CohortBuilderService} from 'generated';
import {CohortReviewApi, CohortsApi, CriteriaListResponse} from 'generated/fetch';
import {Observable} from 'rxjs/Observable';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
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
    registerApiClient(CohortReviewApi, new CohortReviewServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());

    TestBed.configureTestingModule({
      declarations: [
        CreateReviewPage,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {provide: CohortBuilderService, useValue: new BuilderApiStub()}
      ],
    })
      .compileComponents();
    NavStore.navigate = jasmine.createSpy('navigate');
    cohortReviewStore.next(cohortReviewStub);
    currentWorkspaceStore.next(workspaceDataStub);
    urlParamsStore.next({
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    });
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
