import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {FormControl, FormGroup, NgForm, ReactiveFormsModule} from '@angular/forms';

import {CohortSearchActions} from '../redux';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnInit {
  @Input() node;
  fields: Array<string>;

  /*form = new FormGroup({
      attr1: new FormGroup({
          operator: new FormControl(),
          valueA: new FormControl(),
          valueB: new FormControl()
      })
  });*/

  constructor(private actions: CohortSearchActions) { }

  ngOnInit() {
    console.log(this.node);
  }

  ngOnChanges (changes: SimpleChanges) {
    console.log(changes);
    if (changes.node) {
      console.log(changes.node.currentValue.toJS());
      const currentNode = changes.node.currentValue;
      if (currentNode.get('subtype') === 'BP') {
        this.fields = ['Systolic', 'Diastolic'];
      } else {
        this.fields = ['Value'];
      }
    }
  }

  addAttrs(attrs: NgForm) {
    console.log(attrs);

  }

  cancel() {
    this.actions.hideAttributesPage();
  }

}
