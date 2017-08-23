import { Component, OnInit, ComponentFactoryResolver,
  ViewChild, ViewContainerRef } from '@angular/core';
import { CriteriaType } from '../model';
import { SearchService } from '../service';
import { WizardModalComponent } from '../wizard-modal/wizard-modal.component';

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
              private componentFactoryResolver: ComponentFactoryResolver) { }

  ngOnInit() {
    this.criteriaTypes = this.searchService.getCriteriaTypes();
  }

  openWizard(criteriaType: string) {
    const wizardModalComponent =
        this.componentFactoryResolver.resolveComponentFactory(WizardModalComponent);
    const wizardModalRef = this.parent.createComponent(wizardModalComponent);
    wizardModalRef.instance.criteriaType = criteriaType;
    wizardModalRef.instance.wizard.open();
  }

}
