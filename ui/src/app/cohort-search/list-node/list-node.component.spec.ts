import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {ListNodeInfoComponent} from 'app/cohort-search/list-node-info/list-node-info.component';
import {SafeHtmlPipe} from 'app/cohort-search/safe-html.pipe';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi} from 'generated/fetch';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ListNodeComponent} from './list-node.component';

describe('ListNodeComponent', () => {
  let component: ListNodeComponent;
  let fixture: ComponentFixture<ListNodeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        ListNodeInfoComponent,
        ListNodeComponent,
        SafeHtmlPipe,
      ],
      imports: [
        BrowserAnimationsModule,
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  }));

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    fixture = TestBed.createComponent(ListNodeComponent);
    component = fixture.componentInstance;
    component.node = {
      code: '',
      conceptId: 903133,
      count: 0,
      domainId: 'Measurement',
      group: false,
      hasAttributes: true,
      id: 316305,
      name: 'Height Detail',
      parentId: 0,
      predefinedAttributes: null,
      selectable: true,
      subtype: 'HEIGHT',
      type: 'PM'
    };
    component.wizard = {};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
