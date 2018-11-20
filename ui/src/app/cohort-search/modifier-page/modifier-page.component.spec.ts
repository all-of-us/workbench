import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {CohortBuilderService} from 'generated';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {activeModifierList, CohortSearchActions, previewStatus} from '../redux';
import {ModifierPageComponent} from './modifier-page.component';
import {Observable} from 'rxjs/Observable';

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
