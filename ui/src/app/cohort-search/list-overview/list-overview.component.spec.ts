import {NO_ERRORS_SCHEMA} from '@angular/core';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';

import {ListOverviewComponent} from './list-overview.component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {CohortBuilderApi, CohortsApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';

describe('ListOverviewComponent', () => {
  let fixture: ComponentFixture<ListOverviewComponent>;
  let component: ListOverviewComponent;

  beforeEach(async(() => {

    TestBed
      .configureTestingModule({
        declarations: [ListOverviewComponent],
        imports: [ClarityModule],
        providers: [],
        schemas: [NO_ERRORS_SCHEMA],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ListOverviewComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.total = 0;
    component.isRequesting = false;

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
