import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {ListAttributesPageComponent} from 'app/cohort-search/list-attributes-page/list-attributes-page.component';
import {ListDemographicsComponent} from 'app/cohort-search/list-demographics/list-demographics.component';
import {ListModifierPageComponent} from 'app/cohort-search/list-modifier-page/list-modifier-page.component';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {ListNodeComponent} from 'app/cohort-search/list-node/list-node.component';
import {ListOptionInfoComponent} from 'app/cohort-search/list-option-info/list-option-info.component';
import {ListSearchBarComponent} from 'app/cohort-search/list-search-bar/list-search-bar.component';
import {ListSearchComponent} from 'app/cohort-search/list-search/list-search.component';
import {ListSelectionInfoComponent} from 'app/cohort-search/list-selection-info/list-selection-info.component';
import {ListTreeComponent} from 'app/cohort-search/list-tree/list-tree.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ListModalComponent} from './list-modal.component';

describe('ListModalComponent', () => {
  let component: ListModalComponent;
  let fixture: ComponentFixture<ListModalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ListAttributesPageComponent,
        ListDemographicsComponent,
        ListModalComponent,
        ListModifierPageComponent,
        ListNodeInfoComponent,
        ListNodeComponent,
        ListOptionInfoComponent,
        ListSearchBarComponent,
        ListSearchComponent,
        ListSelectionInfoComponent,
        ListTreeComponent,
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
    fixture = TestBed.createComponent(ListModalComponent);
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
