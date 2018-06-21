import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {NgForm} from '@angular/forms';

import {CohortSearchActions} from '../redux';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnChanges {
  @Input() node: any;
  fields: Array<string>;

  constructor(private actions: CohortSearchActions) { }

  ngOnChanges (changes: SimpleChanges) {
    if (changes.node) {
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
