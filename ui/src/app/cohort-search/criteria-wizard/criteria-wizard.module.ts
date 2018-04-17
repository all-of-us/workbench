import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {NouisliderModule} from 'ng2-nouislider';
import {NgxPopperModule} from 'ngx-popper';

/* tslint:disable */
import {AlertsComponent} from './alerts/alerts.component';
import {AttributesModule} from './attributes/attributes.module';
import {CriteriaSelectionComponent} from './criteria-selection/criteria-selection.component';
import {DemoFormComponent} from './demo-form/demo-form.component';
import {DemoSelectComponent} from './demo-select/demo-select.component';
import {ExplorerComponent} from './explorer/explorer.component';
import {LeafComponent} from './leaf/leaf.component';
import {ModifiersComponent} from './modifiers/modifiers.component';
import {ModifierSelectionComponent} from './modifier-selection/modifier-selection.component';
import {QuickSearchResultsComponent} from './quicksearch-results/quicksearch-results.component';
import {QuickSearchComponent} from './quicksearch/quicksearch.component';
import {RootSpinnerComponent} from './root-spinner/root-spinner.component';
import {TreeComponent} from './tree/tree.component';
import {WizardComponent} from './wizard/wizard.component';
/* tslint:enable */


@NgModule({
  imports: [
    AttributesModule,
    CommonModule,
    ClarityModule,
    ReactiveFormsModule,
    NgxPopperModule,
    NouisliderModule,
  ],
  exports: [WizardComponent],
  declarations: [
    AlertsComponent,
    CriteriaSelectionComponent,
    DemoFormComponent,
    DemoSelectComponent,
    ExplorerComponent,
    LeafComponent,
    ModifiersComponent,
    ModifierSelectionComponent,
    QuickSearchComponent,
    QuickSearchResultsComponent,
    RootSpinnerComponent,
    TreeComponent,
    WizardComponent,
  ],
})
export class CriteriaWizardModule { }
