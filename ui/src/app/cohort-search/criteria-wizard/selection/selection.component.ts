import {select} from '@angular-redux/store';
import {Component} from '@angular/core';

import {
  activeCriteriaType,
  activeParameterList,
  CohortSearchActions,
} from '../../redux';

import {
  attributeDisplay,
  nameDisplay,
  typeDisplay,
  typeToTitle,
} from '../../utils';

@Component({
  selector: 'crit-selection',
  templateUrl: './selection.component.html',
  styleUrls: ['./selection.component.css'],
})
export class SelectionComponent {

  @select(activeCriteriaType) criteriaType$;
  @select(activeParameterList) criteriaList$;

  /* Functions of SearchParameters */
  attributeDisplay = attributeDisplay;
  nameDisplay = nameDisplay;
  typeDisplay = typeDisplay;

  constructor(private actions: CohortSearchActions) {}

  get selectionTitle$() {
    return this.criteriaType$
      .filter(thing => thing) // filter out '', null, undefined, etc
      .map(_type => typeToTitle(_type))
      .map(title => title ? `Selected ${title} Codes` : 'No Selection');
  }

  remove(parameter): void {
    this.actions.removeParameter(parameter.get('parameterId'));
  }
}
