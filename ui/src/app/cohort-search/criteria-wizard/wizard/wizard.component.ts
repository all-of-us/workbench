import {Component, Input, ViewEncapsulation} from '@angular/core';

import {CohortSearchActions} from '../../redux';
import {typeToTitle} from '../../utils';

@Component({
  selector: 'app-criteria-wizard',
  templateUrl: './wizard.component.html',
  styleUrls: [
    './wizard.component.css',
    '../../../styles/buttons.css',
  ],
  encapsulation: ViewEncapsulation.None,
})
export class WizardComponent {
  @Input() open: boolean;
  @Input() criteriaType: string;

  constructor(private actions: CohortSearchActions) {}

  get critPageTitle() {
    return `Choose ${typeToTitle(this.criteriaType)} Codes`;
  }

  cancel() {
    this.actions.cancelWizard();
  }

  finish() {
    this.actions.finishWizard();
  }
}
