import {EventEmitter} from '@angular/core';
import {Attribute} from 'generated';


/**
 * An Attribute completes certain types of criteria; if a criteria requires an
 * attribute in order to make sense, then selecting that criteria will
 * instantiate a class that implements the AttributesComponent interface.
 */
export interface AttributeFormComponent {
  /**
   * When the form is submitted, this emits an Attribute.  If the form action
   * is cancelled instead, this emits null.
   */
  attribute: EventEmitter<Attribute | null>;
}
