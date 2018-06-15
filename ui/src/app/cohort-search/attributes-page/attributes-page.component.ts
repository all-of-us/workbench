import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnInit {
  @Input() node;
  fields: Array<string>;

  constructor() { }

  ngOnInit() {
    console.log(this.node);
  }

  ngOnChanges (changes: SimpleChanges) {
    console.log(changes);
    if (changes.node) {
      console.log(changes.node.currentValue.toJS());
      const currentNode = changes.node.currentValue;
      if (currentNode.get('subtype') === 'PM') {
        this.fields = ['Systolic', 'Diastolic'];
      } else {
        this.fields = ['Value'];
      }
    }
  }

}
