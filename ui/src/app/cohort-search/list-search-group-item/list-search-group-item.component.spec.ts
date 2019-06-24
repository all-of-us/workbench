import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {ClarityModule} from '@clr/angular';
import {NgxPopperModule} from 'ngx-popper';

import {ListSearchGroupItemComponent} from './list-search-group-item.component';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {TreeType} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

const zeroCrit = {
  id: 0,
  type: TreeType[TreeType.ICD9],
  code: 'CodeA',
};

const oneCrit = {
  id: 1,
  type: TreeType[TreeType.ICD9],
  code: 'CodeB',
};

const baseItem = {
  id: 'item001',
  type: TreeType[TreeType.ICD9],
  searchParameters: [],
  modifiers: [],
  count: null,
  isRequesting: false,
  status: 'active',
};

describe('ListSearchGroupItemComponent', () => {
  let fixture: ComponentFixture<ListSearchGroupItemComponent>;
  let comp: ListSearchGroupItemComponent;

  beforeEach(async(() => {

    TestBed
      .configureTestingModule({
        declarations: [ListSearchGroupItemComponent],
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
    fixture = TestBed.createComponent(ListSearchGroupItemComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.role = 'includes';
    comp.groupId = 'include0';

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
    expect(trimmedText).toEqual('Contains ICD9 Codes');
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
