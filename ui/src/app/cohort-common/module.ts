import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';

import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {ComboChartComponent} from './combo-chart/combo-chart.component';
import {ValidatorErrorsComponent} from './validator-errors/validator-errors.component';

@NgModule({
  imports: [CommonModule],
  declarations: [
    ComboChartComponent,
    ConfirmDeleteModalComponent,
    ValidatorErrorsComponent
  ],
  exports: [
    // TODO: This could be moved back to CohortSearchModule once no longer
    // needed in CohortReviewModule.
    CommonModule,
    ComboChartComponent,
    ConfirmDeleteModalComponent,
    ValidatorErrorsComponent
  ],
})
export class CohortCommonModule {}
