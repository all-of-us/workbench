import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {AttributesPageComponent} from '../attributes-page/attributes-page.component';
import {DemographicsComponent} from '../demographics/demographics.component';
import {ModifierPageComponent} from '../modifier-page/modifier-page.component';
import {MultiSelectComponent} from '../multi-select/multi-select.component';
import {NodeInfoComponent} from '../node-info/node-info.component';
import {NodeComponent} from '../node/node.component';
import {
activeCriteriaTreeType,
activeCriteriaType,
activeParameterList,
CohortSearchActions,
nodeAttributes,
wizardOpen
} from '../redux';
import {SafeHtmlPipe} from '../safe-html.pipe';
import {SearchBarComponent} from '../search-bar/search-bar.component';
import {SelectionInfoComponent} from '../selection-info/selection-info.component';
import {TreeComponent} from '../tree/tree.component';
import {ModalComponent} from './modal.component';

class MockActions {
  @dispatch() addAttributeForPreview = activeParameterList;
  @dispatch() activeCriteriaType = activeCriteriaType;
  @dispatch() activeCriteriaTreeType = activeCriteriaTreeType;
  @dispatch() activeParameterList = activeParameterList;
  @dispatch() attributesPage = nodeAttributes;
  @dispatch() wizardOpen = wizardOpen;
}

describe('ModalComponent', () => {
  let component: ModalComponent;
  let fixture: ComponentFixture<ModalComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        AttributesPageComponent,
        DemographicsComponent,
        ModalComponent,
        ModifierPageComponent,
        MultiSelectComponent,
        NodeComponent,
        NodeInfoComponent,
        SafeHtmlPipe,
        SearchBarComponent,
        SelectionInfoComponent,
        TreeComponent,
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxPopperModule,
        NouisliderModule,
        ReactiveFormsModule,
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortSearchActions, useValue: new MockActions()},
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
