// TODO: Remove the lint-disable comment once we can selectively ignore import lines.
// https://github.com/palantir/tslint/pull/3099
// tslint:disable:max-line-length

import { Component, OnInit, ComponentFactoryResolver, ViewChild, ViewContainerRef } from '@angular/core';
import { CriteriaType } from '../model';
import { SearchService, BroadcastService } from '../service';
import { WizardModalComponent } from '../wizard-modal/wizard-modal.component';

// tslint:enable:max-line-length

@Component({
  selector: 'app-wizard-select',
  templateUrl: './wizard-select.component.html',
  styleUrls: ['./wizard-select.component.css']
})
export class WizardSelectComponent implements OnInit {

  criteriaTypes: CriteriaType[];
  @ViewChild('parent', {read: ViewContainerRef})
  parent: ViewContainerRef;

  constructor(private searchService: SearchService,
              private broadcastService: BroadcastService,
              private componentFactoryResolver: ComponentFactoryResolver) { }

  ngOnInit() {
    this.criteriaTypes = this.searchService.getCriteriaTypes();
  }

  openWizard(criteriaType: string) {
    const wizardModalComponent = this.componentFactoryResolver.resolveComponentFactory(
        WizardModalComponent);
    const wizardModalRef = this.parent.createComponent(wizardModalComponent);
    this.broadcastService.selectCriteriaType(criteriaType);
    wizardModalRef.instance.wizardModalRef = wizardModalRef;
    wizardModalRef.instance.wizard.open();
  }

}
