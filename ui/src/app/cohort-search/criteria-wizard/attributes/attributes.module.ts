import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ClarityModule} from 'clarity-angular';
import {ReactiveFormsModule} from '@angular/forms';

import {AttributesDirective} from './attributes.directive';
import {AttributesComponent} from './attributes.component';
import {AgeFormComponent} from './age-form.component';

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
    AttributesDirective,
    AttributesComponent,
    AgeFormComponent,
  ],
  entryComponents: [
    AgeFormComponent,
  ],
})
export class AttributesModule {}
