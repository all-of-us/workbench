import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {fromJS, List} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';

import {
  CohortSearchActions,
  INIT_SEARCH_GROUP,
  initGroup
} from '../redux';
import {SearchGroupItemComponent} from '../search-group-item/search-group-item.component';
import {SearchGroupComponent} from '../search-group/search-group.component';
import {SearchGroupListComponent} from './search-group-list.component';

import {CohortBuilderService} from 'generated';

class MockActions {
  @dispatch() initGroup = initGroup;

  generateId(prefix?: string): string {
    return 'TestId';
  }
}

describe('SearchGroupListComponent', () => {
  let fixture: ComponentFixture<SearchGroupListComponent>;
  let component: SearchGroupListComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          SearchGroupListComponent,
          SearchGroupComponent,
          SearchGroupItemComponent,
        ],
        imports: [
          ClarityModule,
          NgxPopperModule,
        ],
        providers: [
          {provide: NgRedux, useValue: mockReduxInst},
          {provide: CohortBuilderService, useValue: {}},
          {provide: CohortSearchActions, useValue: new MockActions()},
        ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    MockNgRedux.reset();

    fixture = TestBed.createComponent(SearchGroupListComponent);
    component = fixture.componentInstance;

    // Default Inputs for tests
    component.role = 'includes';
    component.groups$ = Observable.of(List());

    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(component).toBeTruthy();
  });

  it('Should dispatch INIT_SEARCH_GROUP on Add Group button click', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    component.initGroup();
    expect(spy).toHaveBeenCalledWith({
      type: INIT_SEARCH_GROUP,
      role: 'includes',
      groupId: 'TestId',
    });
  });

  it('Should Display the correct title', () => {
    component.role = 'includes';
    fixture.detectChanges();
    expect(component.title).toEqual('Included Participants');

    component.role = 'excludes';
    fixture.detectChanges();
    expect(component.title).toEqual('Excluded Participants');
  });
});
