import {NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from '@clr/angular';
import {fromJS} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

import {
  CohortSearchActions,
  WIZARD_CANCEL,
  WIZARD_FINISH,
} from '../../redux';

/* tslint:disable */
import {AlertsComponent} from '../alerts/alerts.component';
import {AttributesModule} from '../attributes/attributes.module';
import {DemoFormComponent} from '../demo-form/demo-form.component';
import {DemoSelectComponent} from '../demo-select/demo-select.component';
import {ExplorerComponent} from '../explorer/explorer.component';
import {LeafComponent} from '../leaf/leaf.component';
import {ModifiersComponent} from '../modifiers/modifiers.component';
import {ModifierSelectionComponent} from '../modifier-selection/modifier-selection.component';
import {QuickSearchResultsComponent} from '../quicksearch-results/quicksearch-results.component';
import {QuickSearchComponent} from '../quicksearch/quicksearch.component';
import {RootSpinnerComponent} from '../root-spinner/root-spinner.component';
import {CriteriaSelectionComponent} from '../criteria-selection/criteria-selection.component';
import {TreeComponent} from '../tree/tree.component';
import {WizardComponent} from './wizard.component';
/* tslint:enable */

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
          DemoFormComponent,
          DemoSelectComponent,
          ExplorerComponent,
          LeafComponent,
          ModifiersComponent,
          ModifierSelectionComponent,
          QuickSearchComponent,
          QuickSearchResultsComponent,
          RootSpinnerComponent,
          CriteriaSelectionComponent,
          TreeComponent,
          WizardComponent,
        ],
        imports: [
          AttributesModule,
          ClarityModule,
          NoopAnimationsModule,
          NouisliderModule,
          NgxPopperModule,
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
    comp.onCancel();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_CANCEL});
  });

  it('Should dispatch WIZARD_FINISH on submission', () => {
    const spy = spyOn(mockReduxInst, 'dispatch');
    fixture.detectChanges();
    comp.onSubmit();
    expect(spy).toHaveBeenCalledWith({type: WIZARD_FINISH});
  });
});
