import {Component, Input} from '@angular/core';
import {groupSelectionsStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {attributeDisplay, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {DomainType} from 'generated/fetch';

@Component({
  selector: 'crit-list-selection-info',
  templateUrl: './selection-info.component.html',
  styleUrls: ['./selection-info.component.css']
})
export class SelectionInfoComponent {
  @Input() parameter;
  @Input() indexes;

  remove(): void {
    const {parameterId} = this.parameter;
    let selections = selectionsStore.getValue();
    const wizard = wizardStore.getValue();
    wizard.item.searchParameters = wizard.item.searchParameters
      .filter(p => p.parameterId !== parameterId);
    selections = selections.filter(s => s !== parameterId);
    if (this.parameter.group) {
      const groups = groupSelectionsStore.getValue().filter(id => id !== this.parameter.id);
      groupSelectionsStore.next(groups);
    }
    selectionsStore.next(selections);
    wizardStore.next(wizard);
  }

  get _type()     { return typeDisplay(this.parameter); }
  get name()      { return nameDisplay(this.parameter); }
  get attribute() { return attributeDisplay(this.parameter); }
  get showType() {
    return this.parameter.domainId !== DomainType.PHYSICALMEASUREMENT
      && this.parameter.domainId !== DomainType.DRUG
      && this.parameter.domainId !== DomainType.SURVEY;
  }
  get showOr() {
    return (this.indexes && (this.indexes[0] > 0)) && this.parameter.domainId !== DomainType.PERSON;
  }
}

