import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Input,
  ViewChild
} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List, Map} from 'immutable';

import {CohortSearchActions} from '../../redux';
import {AttributesDirective} from './attributes.directive';
import {AgeFormComponent} from './age-form.component';

@Component({
  selector: 'crit-attributes',
  templateUrl: './attributes.component.html',
  styleUrls: ['./attributes.component.css']
})
export class AttributesComponent implements AfterViewInit {
  @Input() node: Map<any, any>;

  @ViewChild(AttributesDirective) attrFormHost: AttributesDirective;
  private attrs: List<Map<any, any>> = List();

  constructor(
    private actions: CohortSearchActions,
    private resolver: ComponentFactoryResolver,
  ) {}

  ngAfterViewInit() {
    // This setTimeout works around an Angular dev mode bug; we have to wait a
    // tick or the second change detection pass will complain
    setTimeout(_ => this.createForm());
  }

  createForm() {
    // TODO(jms) selection logic for instantiating the form
    const component = AgeFormComponent;
    const factory = this.resolver.resolveComponentFactory(component);
    const container = this.attrFormHost.container;

    container.clear();
    const instance = container.createComponent(factory);
  }

  cancel() {
    this.actions.clearWizardFocus();
  }

  finish() {
    // TODO(jms) transform formData into attributes
    const param = this.node.set('attributes', this.attrs);
    this.actions.addParameter(param);
    this.actions.clearWizardFocus();
  }
}
