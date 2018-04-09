import {select} from '@angular-redux/store';
import {Component} from '@angular/core';

import {activeModifierList, CohortSearchActions} from '../../redux';

@Component({
  selector: 'crit-modifier-selection',
  templateUrl: './modifier-selection.component.html',
  styleUrls: ['./modifier-selection.component.css']
})
export class ModifierSelectionComponent {
  @select(activeModifierList) modifiers$;
  constructor(private actions: CohortSearchActions) {}

  remove(modifier): void {
    this.actions.removeModifier(modifier);
  }

  name(modifier) {
    return {
      ageAtEvent: 'Age At Event',
      numOfOccurrences: 'Number of Occurrences',
      eventDate: 'Date of Event',
    }[modifier.get('name')];
  }

  value(modifier) {
    const op = {
      equal: 'Equal To',
      greater: 'Greater Than',
      lesser: 'Less Than',
    }[modifier.get('operator')];

    return `${op} ${modifier.getIn(['operands', 0])}`;
  }
}
