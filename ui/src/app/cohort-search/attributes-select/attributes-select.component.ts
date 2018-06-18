import { Component, Input, OnInit, Output } from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'crit-attributes-select',
  templateUrl: './attributes-select.component.html',
  styleUrls: ['./attributes-select.component.css']
})
export class AttributesSelectComponent implements OnInit {
  @Input() field;
  @Output() value;

  form = new FormGroup({
      attribute: new FormGroup({
          operator: new FormControl(),
          valueA: new FormControl(),
          valueB: new FormControl()
      })
  });

  constructor() { }

  ngOnInit() {
  }

}
