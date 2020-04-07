import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {CohortBuilderApi, CriteriaType, DomainType} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {DemographicsComponent} from './demographics.component';

const wizardStub = {
  domain: DomainType.PERSON,
  type: CriteriaType.GENDER,
  item: {modifiers: [], searchParameters: []}
};
describe('DemographicsComponent', () => {
  let component: DemographicsComponent;
  let fixture: ComponentFixture<DemographicsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [DemographicsComponent],
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
    serverConfigStore.next({gsuiteDomain: 'fake-research-aou.org', enableCBAgeTypeOptions: false});
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(DemographicsComponent);
    component = fixture.componentInstance;
    component.wizard = wizardStub;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
