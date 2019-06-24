import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

import {wizardStore} from 'app/cohort-search/search-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi, CriteriaType, DomainType} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ListDemographicsComponent} from './list-demographics.component';


describe('ListDemographicsComponent', () => {
  let component: ListDemographicsComponent;
  let fixture: ComponentFixture<ListDemographicsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ListDemographicsComponent],
      imports: [
        ClarityModule,
        NouisliderModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [],
    })
      .compileComponents();
    currentWorkspaceStore.next(workspaceDataStub);
    wizardStore.next({
      domain: DomainType.PERSON,
      type: CriteriaType.GENDER,
      item: {modifiers: [], searchParameters: []}
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ListDemographicsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
