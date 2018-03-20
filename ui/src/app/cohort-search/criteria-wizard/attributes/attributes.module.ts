import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {AttributesComponent} from './attributes.component';
import {AttributesDirective} from './attributes.directive';

import {DummyComponent} from './dummy.component';

/*
 * Main docs for dynamically generating components:
 * https://angular.io/guide/dynamic-component-loader
 *
 *
 */
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
    AttributesComponent,
    AttributesDirective,
    DummyComponent,
  ],
  entryComponents: [
    // Dynamically generated forms need to be declared here as well
    DummyComponent,
  ],
})
export class AttributesModule {}
