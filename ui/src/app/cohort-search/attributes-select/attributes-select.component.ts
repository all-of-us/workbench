import { Component, Input, OnInit, Output } from '@angular/core';

@Component({
  selector: 'crit-attributes-select',
  templateUrl: './attributes-select.component.html',
  styleUrls: ['./attributes-select.component.css']
})
export class AttributesSelectComponent implements OnInit {
  @Input() field;
  @Output() value;

  constructor() { }

  ngOnInit() {
  }

}
