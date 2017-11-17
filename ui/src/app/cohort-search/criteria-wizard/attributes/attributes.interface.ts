/* tslint:disable:no-unused-variable */
import {EventEmitter} from '@angular/core';
import {Attribute} from 'generated';
/* tslint:enable:no-unused-variable */

export interface AttributeFormComponent {
  attribute: EventEmitter<Attribute | null>;
}
