import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {FormControl, FormGroup, NgForm, ReactiveFormsModule} from '@angular/forms';

import {NgRedux} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions, CohortSearchState, isParameterActive} from '../redux';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnInit {
  @Input() node;
  fields: Array<string>;
  private isSelected: boolean;
  private subscription: Subscription;

  /*form = new FormGroup({
      attr1: new FormGroup({
          operator: new FormControl(),
          valueA: new FormControl(),
          valueB: new FormControl()
      })
  });*/

  constructor(
      private ngRedux: NgRedux<CohortSearchState>,
      private actions: CohortSearchActions) { }

  ngOnInit() {
    console.log(this.node);
    this.subscription = this.ngRedux
        .select(isParameterActive(this.paramId))
        .map(val => true && val)
        .subscribe(val => {
          console.log(val);
          this.isSelected = val;
        });
  }

  ngOnChanges (changes: SimpleChanges) {
    console.log(changes);
    if (changes.node) {
      console.log(changes.node.currentValue.toJS());
      const currentNode = changes.node.currentValue;
      if (currentNode.get('subtype') === 'BP') {
        this.fields = ['Systolic', 'Diastolic'];
      } else {
        this.fields = [''];
      }
    }
  }

  get paramId() {
      return `param${this.node.get('id')}`;
  }

  addAttrs(attrs: NgForm) {
    let code = '';
    let name = this.node.get('name', '') + ' (';
    this.fields.forEach((field, i) => {
      if (i > 0) {
        code += ';';
        name += ' / ';
      }
      if (attrs.value['operator' + i] === 'between') {
        code += 'between;' + attrs.value['valueA' + i] + ' and ' + attrs.value['valueB' + i];
        name += field + ' ' + attrs.value['valueA' + i] + '-' + attrs.value['valueB' + i];
      } else {
        code += code += attrs.value['operator' + i] + ';' + attrs.value['valueA' + i];
        name += field + ' ' + attrs.value['operator' + i] + attrs.value['valueA' + i];
      }
    });
    name += ')';
    const param = this.node
        .set('parameterId', this.paramId)
        .set('code', code)
        .set('name', name);
    this.actions.addParameter(param);
    this.actions.hideAttributesPage();
  }

  cancel() {
    this.actions.hideAttributesPage();
  }

}
