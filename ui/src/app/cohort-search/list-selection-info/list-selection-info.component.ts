import {Component, Input} from '@angular/core';
import {groupSelectionsStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {listAttributeDisplay, listNameDisplay, listTypeDisplay} from 'app/cohort-search/utils';
import {DomainType} from 'generated/fetch';

@Component({
  selector: 'crit-list-selection-info',
  templateUrl: './list-selection-info.component.html',
  styleUrls: ['./list-selection-info.component.css']
})
export class ListSelectionInfoComponent {
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

  get _type()     { return listTypeDisplay(this.parameter); }
  get name()      { return listNameDisplay(this.parameter); }
  get attribute() { return listAttributeDisplay(this.parameter); }
  get showType() {
    return this.parameter.domain !== DomainType.PHYSICALMEASUREMENT
      && this.parameter.domain !== DomainType.DRUG
      && this.parameter.domain !== DomainType.SURVEY;
  }
  get showOr() {
    return (this.indexes && (this.indexes[0] > 0)) && this.parameter.domain !== DomainType.PERSON;
  }
}

