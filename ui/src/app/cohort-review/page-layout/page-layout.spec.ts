import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {CohortBuilderService, CohortsService} from 'generated';
import {CohortReviewApi, CriteriaListResponse} from 'generated/fetch';
import {CohortReviewServiceStub, cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspace-storage-service-stub';
import {PageLayout} from './page-layout';
import {Observable} from 'rxjs/Observable';
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

    TestBed.configureTestingModule({
      declarations: [
        CreateReviewPage,
        PageLayout,
      ],
      imports: [ClarityModule, ReactiveFormsModule, RouterTestingModule],
      providers: [
        {provide: CohortsService, useValue: new CohortsServiceStub()},
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
