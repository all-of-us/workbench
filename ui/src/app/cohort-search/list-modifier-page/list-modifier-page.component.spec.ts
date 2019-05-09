import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {activeModifierList, CohortSearchActions, previewStatus} from 'app/cohort-search/redux';
import {wizardStore} from 'app/cohort-search/search-state.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType, WorkspaceAccessLevel} from 'generated';
import {CohortBuilderApi} from 'generated/fetch';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {ListModifierPageComponent} from './list-modifier-page.component';

class MockActions {
  @dispatch() activeModifierList = activeModifierList;
  @dispatch() previewStatus = previewStatus;
}

describe('ListModifierPageComponent', () => {
  let component: ListModifierPageComponent;
  let fixture: ComponentFixture<ListModifierPageComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        ListModifierPageComponent,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    wizardStore.next({
      domain: DomainType.MEASUREMENT,
      item: {modifiers: []}
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListModifierPageComponent);
    component = fixture.componentInstance;
    component.disabled = () => {};
    component.wizard = {domain: DomainType.MEASUREMENT};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
