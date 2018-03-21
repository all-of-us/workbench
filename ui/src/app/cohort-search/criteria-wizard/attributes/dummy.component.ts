import {Component, EventEmitter} from '@angular/core';

import {AttributeFormComponent} from './attributes.interface';

import {Attribute} from 'generated';

/* This is a dummy stub for an actual attribute form */
@Component({template: '<div>Dummy</div>'})
export class DummyComponent implements AttributeFormComponent {
  attribute = new EventEmitter<Attribute | null>();
}
