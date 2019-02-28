import {NgModule} from '@angular/core';
import {NgxChartsModule} from '@swimlane/ngx-charts';

import {ComboChartComponent} from './combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from './validator-errors/validator-errors.component';

@NgModule({
  imports: [
    NgxChartsModule,
  ],
  declarations: [
    ComboChartComponent,
    ValidatorErrorsComponent
  ],
  exports: [
    // TODO: This could be moved back to CohortSearchModule once no longer
    // needed in CohortReviewModule.
    ComboChartComponent,
    ValidatorErrorsComponent
  ],
  providers: []
})
export class CohortCommonModule {}
