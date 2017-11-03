import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';

import {WizardComponent} from './wizard/wizard.component';
import {TreeComponent} from './tree/tree.component';
import {SelectionComponent} from './selection/selection.component';
import {RootSpinnerComponent} from './root-spinner/root-spinner.component';
import {AlertsComponent} from './alerts/alerts.component';


@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
  ],
  exports: [WizardComponent],
  declarations: [
    SelectionComponent,
    TreeComponent,
    WizardComponent,
    RootSpinnerComponent,
    AlertsComponent,
  ],
})
export class CriteriaWizardModule { }
