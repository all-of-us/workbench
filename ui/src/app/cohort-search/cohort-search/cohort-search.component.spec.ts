import {dispatch, NgRedux} from '@angular-redux/store';
import {MockNgRedux} from '@angular-redux/store/testing';
import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {CohortBuilderService} from 'generated';
import {fromJS} from 'immutable';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';
import {Observable} from 'rxjs/Observable';
import {ComboChartComponent} from '../../cohort-common/combo-chart/combo-chart.component';
import {AttributesPageComponent} from '../attributes-page/attributes-page.component';
import {DemographicsComponent} from '../demographics/demographics.component';
import {GenderChartComponent} from '../gender-chart/gender-chart.component';
import {ModalComponent} from '../modal/modal.component';
import {ModifierPageComponent} from '../modifier-page/modifier-page.component';
import {MultiSelectComponent} from '../multi-select/multi-select.component';
import {NodeInfoComponent} from '../node-info/node-info.component';
import {NodeComponent} from '../node/node.component';
import {OverviewComponent} from '../overview/overview.component';
import {
  cancelWizard,
  CohortSearchActions,
  finishWizard,
  resetStore
} from '../redux';
import {SafeHtmlPipe} from '../safe-html.pipe';
import {SearchBarComponent} from '../search-bar/search-bar.component';
import {SearchGroupItemComponent} from '../search-group-item/search-group-item.component';
import {SearchGroupListComponent} from '../search-group-list/search-group-list.component';
import {SearchGroupSelectComponent} from '../search-group-select/search-group-select.component';
import {SearchGroupComponent} from '../search-group/search-group.component';
import {SelectionInfoComponent} from '../selection-info/selection-info.component';
import {TreeComponent} from '../tree/tree.component';
import {CohortSearchComponent} from './cohort-search.component';

class MockActions {
  @dispatch() cancelWizard = cancelWizard;
  @dispatch() finishWizard = finishWizard;
  @dispatch() resetStore = resetStore;

  loadFromJSON(json: string): void {}
  runAllRequests() {}
}

describe('CohortSearchComponent', () => {
  let activatedRoute: ActivatedRoute;
  let component: CohortSearchComponent;
  let fixture: ComponentFixture<CohortSearchComponent>;
  let mockReduxInst;

  beforeEach(async(() => {
    mockReduxInst = MockNgRedux.getInstance();
    const _old = mockReduxInst.getState;
    const _wrapped = () => fromJS(_old());
    mockReduxInst.getState = _wrapped;

    TestBed.configureTestingModule({
      declarations: [
        AttributesPageComponent,
        CohortSearchComponent,
        ComboChartComponent,
        DemographicsComponent,
        GenderChartComponent,
        ModalComponent,
        ModifierPageComponent,
        MultiSelectComponent,
        NodeComponent,
        NodeInfoComponent,
        OverviewComponent,
        SafeHtmlPipe,
        SearchBarComponent,
        SearchGroupComponent,
        SearchGroupItemComponent,
        SearchGroupListComponent,
        SearchGroupSelectComponent,
        SelectionInfoComponent,
        TreeComponent
      ],
      imports: [
        ClarityModule,
        FormsModule,
        NgxChartsModule,
        NgxPopperModule,
        NouisliderModule,
        ReactiveFormsModule
      ],
      providers: [
        {provide: NgRedux, useValue: mockReduxInst},
        {provide: CohortBuilderService, useValue: {}},
        {provide: CohortSearchActions, useValue: new MockActions()},
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: Observable.of({criteria: '{"excludes":[],"includes":[]}'}),
            data: Observable.of({workspace: {cdrVersionId: '1'}})
          }
        },
      ],
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CohortSearchComponent);
    component = fixture.componentInstance;
    activatedRoute = new ActivatedRoute();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
