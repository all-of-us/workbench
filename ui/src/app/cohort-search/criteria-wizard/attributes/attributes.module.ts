import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {AgeFormComponent} from './age-form.component';
import {AttributesComponent} from './attributes.component';
import {AttributesDirective} from './attributes.directive';

@NgModule({
  imports: [
    CommonModule,
    ClarityModule,
    ReactiveFormsModule,
  ],
  exports: [
    AttributesComponent,
  ],
  declarations: [
    AgeFormComponent,
    AttributesComponent,
    AttributesDirective,
  ],
  entryComponents: [
    AgeFormComponent,
  ],
})
export class AttributesModule {}
