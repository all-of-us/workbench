import {async as _async, ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';
import {ClarityModule} from 'clarity-angular';
import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {fromJS} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
} from '../redux';
import {SearchGroupItemComponent} from './search-group-item.component';
import {CohortBuilderService} from 'generated';

describe('SearchGroupItemComponent', () => {
  let fixture: ComponentFixture<SearchGroupItemComponent>;
  let comp: SearchGroupItemComponent;

  beforeEach(_async(() => {
    const mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed
      .configureTestingModule({
        declarations: [SearchGroupItemComponent],
        imports: [
          ClarityModule,
        ],
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

    fixture = TestBed.createComponent(SearchGroupItemComponent);
    comp = fixture.componentInstance;

    // Default Inputs for tests
    comp.role = 'includes';
    comp.groupId = 'include0';
    comp.itemId = 'item001';
    comp.itemIndex = 0;

    fixture.detectChanges();
  });

  it('should render', () => {
    const displayText = fixture.debugElement.query(By.css('small.trigger'));
    // let content = displayText.nativeElement.textContent;
    expect(displayText).toBeTruthy();
    // expect(content).toBe('Nonsense');
  });
});
