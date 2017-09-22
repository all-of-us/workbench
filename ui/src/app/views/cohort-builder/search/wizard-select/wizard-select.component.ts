// TODO: Remove the lint-disable comment once we can selectively ignore import lines.
// https://github.com/palantir/tslint/pull/3099
// tslint:disable:max-line-length

import {
  Component, ComponentFactoryResolver,
  ViewChild, ViewContainerRef
} from '@angular/core';

import {BroadcastService} from '../broadcast.service';
import {CRITERIA_TYPES} from '../constants';
import {WizardModalComponent} from '../wizard-modal/wizard-modal.component';

// tslint:enable:max-line-length

@Component({
  selector: 'app-wizard-select',
  templateUrl: './wizard-select.component.html',
  styleUrls: ['./wizard-select.component.css']
})
export class WizardSelectComponent {

  readonly criteriaTypes = CRITERIA_TYPES;

  @ViewChild('parent', {read: ViewContainerRef})
  parent: ViewContainerRef;

  constructor(private broadcastService: BroadcastService,
              private componentFactoryResolver: ComponentFactoryResolver) { }

  openWizard(criteriaType: string) {
    const wizardModalComponent = this.componentFactoryResolver.resolveComponentFactory(
        WizardModalComponent);
    const wizardModalRef = this.parent.createComponent(wizardModalComponent);
    this.broadcastService.selectCriteriaType(criteriaType);
    wizardModalRef.instance.wizardModalRef = wizardModalRef;
    wizardModalRef.instance.wizard.open();
  }
}
