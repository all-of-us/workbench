import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';

import {
  CohortSearchActions,
  WIZARD_CANCEL,
  WIZARD_FINISH,
} from '../../redux';

import {AlertsComponent} from '../alerts/alerts.component';
import {AttributesModule} from '../attributes/attributes.module';
import {ExplorerComponent} from '../explorer/explorer.component';
import {LeafComponent} from '../leaf/leaf.component';
import {
  QuickSearchResultsComponent
} from '../quicksearch-results/quicksearch-results.component';
import {QuickSearchComponent} from '../quicksearch/quicksearch.component';
import {RootSpinnerComponent} from '../root-spinner/root-spinner.component';
import {SelectionComponent} from '../selection/selection.component';
import {TreeComponent} from '../tree/tree.component';
import {WizardComponent} from './wizard.component';

import {CohortBuilderService} from 'generated';


describe('WizardComponent', () => {
  let fixture: ComponentFixture<WizardComponent>;
  let comp: WizardComponent;

  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const old = mockReduxInst.getState;
    const wrapped = () => fromJS(old());
    mockReduxInst.getState = wrapped;

    TestBed
      .configureTestingModule({
        declarations: [
          AlertsComponent,
          ExplorerComponent,
          LeafComponent,
          QuickSearchComponent,
          QuickSearchResultsComponent,
          RootSpinnerComponent,
          SelectionComponent,
          TreeComponent,
          WizardComponent,
        ],
        imports: [
          AttributesModule,
          ClarityModule,
          NoopAnimationsModule,
          ReactiveFormsModule,
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

    fixture = TestBed.createComponent(WizardComponent);
    comp = fixture.componentInstance;
    comp.open = true;
    comp.criteriaType = 'icd9';
    fixture.detectChanges();
  });

  it('Should render', () => {
    expect(comp).toBeTruthy();
  });

  it('Should dispatch WIZARD_CANCEL on cancel', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.cancel();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_CANCEL});
  });

  it('Should dispatch WIZARD_FINISH on finish', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    comp.finish();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_FINISH});
  });
});
