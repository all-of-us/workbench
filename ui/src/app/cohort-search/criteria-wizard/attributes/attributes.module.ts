import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';

import {AttributesComponent} from './attributes.component';
import {AttributesDirective} from './attributes.directive';

/*
 * B/c the implementation does not currently have a concrete implementation of
 * an attribute form, we're using this DummyComponent as a "pass" statement.
 * It is syntactically useful but does nothing real.
 */
import {DummyComponent} from './dummy.component';

/*
 * The basic component is AttributesComponent.  When a criterion enters
 * "focus", AttributesComponent will accept that criteria node and dynamically
 * render the correct form for that node.  On form submission, the dynamically
 * generated form is destroyed, a completed parameter is put into state, and
 * the "focus" is unset (upon which the criteria explorer component switches
 * mode back to Tree or Search).
 *
 * Main docs for how to dynamically generate components in Angular:
 * https://angular.io/guide/dynamic-component-loader
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
    /* NOTE: Dynamically generated forms need to be declared here as well */
    DummyComponent,
  ],
})
export class AttributesModule {}
