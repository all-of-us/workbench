import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ClarityModule} from 'clarity-angular';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';

import {AttributesModule} from './attributes/attributes.module';
import {AlertsComponent} from './alerts/alerts.component';
import {FuzzyFinderComponent} from './fuzzy-finder/fuzzy-finder.component';
import {RootSpinnerComponent} from './root-spinner/root-spinner.component';
import {SelectionComponent} from './selection/selection.component';
import {TreeComponent} from './tree/tree.component';
import {WizardComponent} from './wizard/wizard.component';
import { ExplorerComponent } from './explorer/explorer.component';


@NgModule({
  imports: [
    AttributesModule,
    BrowserAnimationsModule,
    CommonModule,
    ClarityModule,
    ReactiveFormsModule,
  ],
  exports: [WizardComponent],
  declarations: [
    AlertsComponent,
    FuzzyFinderComponent,
    RootSpinnerComponent,
    SelectionComponent,
    TreeComponent,
    WizardComponent,
    ExplorerComponent,
  ],
})
export class CriteriaWizardModule { }
