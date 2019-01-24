import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {ValidatorErrorsComponent} from 'app/cohort-common/validator-errors/validator-errors.component';
import {activeModifierList, CohortSearchActions, previewStatus} from 'app/cohort-search/redux';
import {CohortBuilderService} from 'generated';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {ModifierPageComponent} from './modifier-page.component';

class MockActions {
  @dispatch() activeModifierList = activeModifierList;
  @dispatch() previewStatus = previewStatus;
}

describe('ModifierPageComponent', () => {
  let component: ModifierPageComponent;
  let fixture: ComponentFixture<ModifierPageComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        ModifierPageComponent,
        ValidatorErrorsComponent,
      ],
      imports: [
        ClarityModule,
        NgxPopperModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
        {
          provide: ActivatedRoute,
          useValue: {snapshot: {data: {workspace: {cdrid: 1}}}}
        },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModifierPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
