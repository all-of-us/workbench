import {select} from '@angular-redux/store';
import {Component} from '@angular/core';

import {activeModifierList, CohortSearchActions} from '../../redux';
import {ModifierType, Operator} from 'generated';

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
      [ModifierType.AGEATEVENT]: 'Age At Event',
      [ModifierType.NUMOFOCCURRENCES]: 'Number of Occurrences',
      [ModifierType.EVENTDATE]: 'Date of Event',
    }[modifier.get('name')];
  }

  value(modifier) {
    const op = {
      [Operator.EQUAL]: 'Equal To',
      [Operator.GREATERTHAN]: 'Greater Than',
      [Operator.GREATERTHANOREQUALTO]: 'Greater Than Or Equal To',
      [Operator.LESSTHAN]: 'Less Than',
      [Operator.LESSTHANOREQUALTO]: 'Less Than Or Equal To',
      [Operator.BETWEEN]: 'Between',
    }[modifier.get('operator')];

    return `${op} ${modifier.getIn(['operands', 0])}`;
  }
}
