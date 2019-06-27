import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';

import {ListSearchGroupComponent} from './list-search-group.component';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {ListSearchGroupItemComponent} from 'app/cohort-search/list-search-group-item/list-search-group-item.component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

const group = {
  id: 'include0',
  count: null,
  isRequesting: false,
  items: [
    {
      id: 'itemA',
      type: DomainType.MEASUREMENT,
      searchParameters: [],
      modifiers: [],
      count: null,
      temporalGroup: 0,
      isRequesting: false,
      status: 'active'
    },
    {
      id: 'itemB',
      type: DomainType.MEASUREMENT,
      searchParameters: [],
      modifiers: [],
      count: null,
      temporalGroup: 0,
      isRequesting: false,
      status: 'active'
    }],
  status: 'active',
};

describe('ListSearchGroupComponent', () => {
  let fixture: ComponentFixture<ListSearchGroupComponent>;
  let comp: ListSearchGroupComponent;

  beforeEach(async(() => {
    TestBed
      .configureTestingModule({
        declarations: [
          ListSearchGroupComponent,
          ListSearchGroupItemComponent,
          ValidatorErrorsComponent
        ],
        imports: [
          ClarityModule,
          NgxPopperModule,
          ReactiveFormsModule
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
    fixture = TestBed.createComponent(ListSearchGroupComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.group = group;
    comp.role = 'includes';
    fixture.detectChanges();
  });

  it('Should render', () => {
    // sanity check
    expect(comp).toBeTruthy();
    const items = fixture.debugElement.queryAll(By.css('app-list-search-group-item'));
    expect(items.length).toBe(2);
  });

  it('Should render group count if group count', () => {
    comp.loading = false;
    comp.count = 25;
    fixture.detectChanges();

    const footer = fixture.debugElement.query(By.css('div.card-footer'));
    const spinner = fixture.debugElement.query(By.css('span.spinner.group'));
    const text = footer.nativeElement.textContent.replace(/\s+/g, ' ').trim();

    expect(text).toEqual('Temporal Group Count: 25');
    expect(spinner).toBeNull();
  });

  it('Should render a spinner if requesting', () => {
    comp.loading = true;
    comp.count = 1;
    fixture.detectChanges();
    const spinner = fixture.debugElement.query(By.css('span.spinner'));
    expect(spinner).not.toBeNull();
  });
});
