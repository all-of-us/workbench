import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';

import {AttributesModule} from './attributes/attributes.module';
import {AlertsComponent} from './alerts/alerts.component';
import {RootSpinnerComponent} from './root-spinner/root-spinner.component';
import {SelectionComponent} from './selection/selection.component';
import {TreeComponent} from './tree/tree.component';
import {WizardComponent} from './wizard/wizard.component';


@NgModule({
  imports: [
    AttributesModule,
    CommonModule,
    ClarityModule,
  ],
  exports: [WizardComponent],
  declarations: [
    AlertsComponent,
    RootSpinnerComponent,
    SelectionComponent,
    TreeComponent,
    WizardComponent,
  ],
})
export class CriteriaWizardModule { }
