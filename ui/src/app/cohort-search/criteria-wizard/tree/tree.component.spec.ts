import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from 'clarity-angular';
import {fromJS, Map} from 'immutable';

import {
  BEGIN_CRITERIA_REQUEST,
  CohortSearchActions,
} from '../../redux';
import {LeafComponent} from '../leaf/leaf.component';
import {TreeComponent} from './tree.component';

import {CohortBuilderService} from 'generated';

describe('TreeComponent', () => {
  let fixture: ComponentFixture<TreeComponent>;
  let comp: TreeComponent;

  let dispatchSpy;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          LeafComponent,
          TreeComponent,
        ],
        imports: [ClarityModule],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          CohortSearchActions,
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();
    dispatchSpy = spyOn(mockReduxInst, 'dispatch');
    fixture = TestBed.createComponent(TreeComponent);
    comp = fixture.componentInstance;
    comp.node = Map({type: 'icd9', id: 0});
    fixture.detectChanges();
  });

  it('Should dispatch BEGIN_CRITERIA_REQUEST on init', () => {
    expect(comp).toBeTruthy();
    expect(dispatchSpy).toHaveBeenCalledWith({
      type: BEGIN_CRITERIA_REQUEST,
      kind: 'icd9',
      parentId: 0
    });
  });
});
