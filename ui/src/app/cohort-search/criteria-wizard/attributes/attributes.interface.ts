import {EventEmitter} from '@angular/core';
import {Attribute} from 'generated';

export interface AttributeFormComponent {
  attribute: EventEmitter<Attribute>;
  submitted: EventEmitter<boolean>;
  cancelled: EventEmitter<boolean>;
}
