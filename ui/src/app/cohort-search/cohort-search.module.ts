// tslint:disable:max-line-length
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {NgReduxModule} from '@angular-redux/store';

/* Components */
import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';
import {SearchGroupComponent} from './search-group/search-group.component';
import {SearchGroupItemComponent} from './search-group-item/search-group-item.component';
import {WizardCriteriaGroupComponent} from './wizard-criteria-group/wizard-criteria-group.component';
import {WizardModalComponent} from './wizard-modal/wizard-modal.component';
import {CriteriaTreeComponent} from './criteria-tree/criteria-tree.component';
import {
  ChartsComponent,
  GenderChartComponent,
  RaceChartComponent,
  GoogleChartComponent,
} from './charts';

/* Other Objects */
import {CohortSearchRouter} from './router.module';
import {
  CohortSearchActions,
  CohortSearchEpics,
  ConfigureStore,
} from './redux';
import {CohortBuilderService} from 'generated';

// tslint:enable:max-line-length

@NgModule({
  imports: [
    ClarityModule,
    CohortSearchRouter,
    CommonModule,
    NgReduxModule,
  ],
  declarations: [
    CohortBuilderComponent,
    CriteriaTreeComponent,

    SearchGroupComponent,
    SearchGroupItemComponent,
    WizardModalComponent,
    WizardCriteriaGroupComponent,

    ChartsComponent,
    GenderChartComponent,
    RaceChartComponent,
    GoogleChartComponent,
  ],
  entryComponents: [WizardModalComponent],
  providers: [
    CohortBuilderService,
    CohortSearchActions,
    CohortSearchEpics,
    ConfigureStore,
  ]
})
export class CohortSearchModule {
  constructor(store: ConfigureStore) {}
}
