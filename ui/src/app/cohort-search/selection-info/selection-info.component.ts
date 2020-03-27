import {Component, Input} from '@angular/core';
import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {DomainType} from 'generated/fetch';

@Component({
  selector: 'crit-list-selection-info',
  templateUrl: './selection-info.component.html',
  styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent {
  @Input() index: number;
  @Input() parameter: any;
  @Input() remove: Function;

  get _type()     { return typeDisplay(this.parameter); }
  get name()      { return nameDisplay(this.parameter); }
  get attribute() { return attributeDisplay(this.parameter); }
  get showType() {
    return this.parameter.domainId !== DomainType.PHYSICALMEASUREMENT
      && this.parameter.domainId !== DomainType.DRUG
      && this.parameter.domainId !== DomainType.SURVEY;
  }
  get showOr() {
    return this.index > 0 && this.parameter.domainId !== DomainType.PERSON;
  }
}

