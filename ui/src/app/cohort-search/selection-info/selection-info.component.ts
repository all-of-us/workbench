import {Component, Input} from '@angular/core';

import {CohortSearchActions} from '../redux';
import {attributeDisplay, nameDisplay, typeDisplay} from '../utils';

@Component({
  selector: 'crit-selection-info',
  templateUrl: './selection-info.component.html',
  styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent {
  @Input() parameter;
  @Input() index;

  constructor(private actions: CohortSearchActions) {}

  remove(): void {
    const paramId = this.parameter.get('parameterId');
    this.actions.removeParameter(paramId);
  }

  get _type()     { return typeDisplay(this.parameter); }
  get name()      { return nameDisplay(this.parameter); }
  get attribute() { return attributeDisplay(this.parameter); }
}
