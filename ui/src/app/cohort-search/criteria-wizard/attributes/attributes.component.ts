import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Input,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {List, Map, fromJS} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../../redux';
import {AttributesDirective} from './attributes.directive';
import {AgeFormComponent} from './age-form.component';


@Component({
  selector: 'crit-attributes',
  template: `
    <div [style.padding]="'1rem'">
      <ng-template critAttrFormHost></ng-template>
    </div>
  `
})
export class AttributesComponent implements AfterViewInit, OnDestroy {
  @Input() node: Map<any, any>;
  @ViewChild(AttributesDirective) attrFormHost: AttributesDirective;

  private _attr: any;
  private _subs: Subscription[];
  private _form: any;

  constructor(
    private actions: CohortSearchActions,
    private resolver: ComponentFactoryResolver,
  ) {}

  ngAfterViewInit() {
    // This setTimeout works around an Angular dev mode bug; we have to wait a
    // tick or the second change detection pass will complain
    setTimeout(_ => this.createForm());
  }

  ngOnDestroy() {
    this._form.destroy();
    this._subs.forEach(s => s.unsubscribe());
  }

  createForm() {
    // TODO(jms) selection logic for instantiating the form
    const component = AgeFormComponent;
    const factory = this.resolver.resolveComponentFactory(component);
    const container = this.attrFormHost.container;

    container.clear();
    this._form = container.createComponent(factory);

    this._subs = [
      this._form.instance.attribute.subscribe(
        value => this._attr = fromJS(value)
      ),
      this._form.instance.submitted.filter(v => v).subscribe(this.submit),
      this._form.instance.cancelled.filter(v => v).subscribe(this.cancel),
    ];
  }

  cancel = (): void => {
    this.actions.clearWizardFocus();
  }

  submit = (): void => {
    const parameterId = `param${this._attr.hashCode()}`;
    const param = this.node
      .set('attribute', this._attr)
      .set('parameterId', parameterId);
    this.actions.addParameter(param);
    this.actions.clearWizardFocus();
  }
}
