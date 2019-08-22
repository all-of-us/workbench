import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';

import {SearchGroupItemComponent} from './search-group-item.component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi, CriteriaType, DomainType} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

const zeroCrit = {
  id: 0,
  name: 'Test param',
  domain: DomainType.CONDITION,
  type: CriteriaType.ICD9CM,
  code: 'CodeA',
};

const oneCrit = {
  id: 1,
  name: 'Test param',
  domain: DomainType.CONDITION,
  type: CriteriaType.ICD9CM,
  code: 'CodeB',
};

const baseItem = {
  id: 'item001',
  name: 'Test item',
  domain: DomainType.CONDITION,
  type: CriteriaType.ICD9CM,
  searchParameters: [],
  modifiers: [],
  count: null,
  isRequesting: false,
  status: 'active',
};

describe('SearchGroupItemComponent', () => {
  let fixture: ComponentFixture<SearchGroupItemComponent>;
  let comp: SearchGroupItemComponent;

  beforeEach(async(() => {

    TestBed
      .configureTestingModule({
        declarations: [SearchGroupItemComponent],
        imports: [
          ClarityModule,
          NgxPopperModule,
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
    fixture = TestBed.createComponent(SearchGroupItemComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.role = 'includes';
    comp.groupId = 'include0';
    comp.updateGroup = () => {};
    comp.item = baseItem;

    fixture.detectChanges();
  });

  it('Should display code type', () => {
    comp.item.searchParameters = [zeroCrit, oneCrit];
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('small.trigger'))).toBeTruthy();

    const display = fixture.debugElement.query(By.css('small.trigger')).nativeElement;
    expect(display.childElementCount).toBe(2);

    const trimmedText = display.textContent.replace(/\s+/g, ' ').trim();
    expect(trimmedText).toEqual('Contains ICD9CM Codes');
  });

  it('Should properly pluralize \'Code\'', () => {
    comp.item.searchParameters = [zeroCrit];
    fixture.detectChanges();
    expect(comp.pluralizedCode).toBe('Code');

    comp.item.searchParameters.push(oneCrit);
    fixture.detectChanges();
    expect(comp.pluralizedCode).toBe('Codes');
  });

});
