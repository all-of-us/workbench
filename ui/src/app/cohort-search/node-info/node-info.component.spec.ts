import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {NgxPopperModule} from 'ngx-popper';
import {NodeInfoComponent} from '../node-info/node-info.component';
import {CohortSearchActions} from '../redux';
import {SafeHtmlPipe} from '../safe-html.pipe';

describe('NodeInfoComponent', () => {
  let component: NodeInfoComponent;
  let fixture: ComponentFixture<NodeInfoComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        NodeInfoComponent,
        SafeHtmlPipe,
      ],
      imports: [
        ClarityModule,
        NgxPopperModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        CohortSearchActions,
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NodeInfoComponent);
    component = fixture.componentInstance;
    component.node = fromJS({
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
    });
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
