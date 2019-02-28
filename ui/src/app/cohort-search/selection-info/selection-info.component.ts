
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CohortSearchActions} from 'app/cohort-search/redux';
import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {TreeType} from 'generated';

@Component({
  selector: 'crit-selection-info',
  templateUrl: './selection-info.component.html',
  styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent {
  @Input() parameter;
  @Input() indexes;

  constructor(private actions: CohortSearchActions) {}


  remove(): void {
    const paramId = this.parameter.get('parameterId');
    const path = this.parameter.get('path');
    const id = this.parameter.get('id');
    this.actions.removeParameter(paramId, path, id);
  }

  get _type()     { return typeDisplay(this.parameter); }
  get name()      { return nameDisplay(this.parameter); }
  get attribute() { return attributeDisplay(this.parameter); }
  get showType() {
    return this.parameter.get('type') !== TreeType.PM
          && this.parameter.get('type') !== TreeType.DRUG
          && this.parameter.get('type') !== TreeType.PPI;
  }
  get showOr() {
    return (this.indexes && (this.indexes[0] > 0 || this.indexes[1] > 0))
          && this.parameter.get('type') !== TreeType.DEMO;
  }
}

