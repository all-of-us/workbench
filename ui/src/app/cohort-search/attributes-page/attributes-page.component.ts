import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'crit-attributes-page',
  templateUrl: './attributes-page.component.html',
  styleUrls: ['./attributes-page.component.css']
})
export class AttributesPageComponent implements OnInit {
  @Input() node;

  constructor() { }

  ngOnInit() {
    console.log(this.node);
  }

}
