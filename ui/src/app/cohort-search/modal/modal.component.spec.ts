import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {AttributesPageComponent} from 'app/cohort-search/attributes-page/attributes-page.component';
import {DemographicsComponent} from 'app/cohort-search/demographics/demographics.component';
import {ListSearchComponent} from 'app/cohort-search/list-search/list-search.component';
import {ModifierPageComponent} from 'app/cohort-search/modifier-page/modifier-page.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {SelectionInfoComponent} from 'app/cohort-search/selection-info/selection-info.component';
import {CriteriaTreeComponent} from 'app/cohort-search/tree/tree.component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ModalComponent} from './modal.component';

describe('ModalComponent', () => {
  let component: ModalComponent;
  let fixture: ComponentFixture<ModalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AttributesPageComponent,
        CriteriaTreeComponent,
        DemographicsComponent,
        ModalComponent,
        ModifierPageComponent,
        ListSearchComponent,
        SelectionInfoComponent,
        SafeHtmlPipe,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        NouisliderModule,
        ReactiveFormsModule,
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next(workspaceDataStub);
    wizardStore.next({
      domain: DomainType.MEASUREMENT,
      item: {modifiers: [], searchParameters: []}
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ModalComponent);
    component = fixture.componentInstance;
    component.attributesNode = {
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
      parentId: 0,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
