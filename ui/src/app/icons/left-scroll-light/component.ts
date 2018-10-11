import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-left-scroll-light-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class LeftScrollLightComponent {
  @Input() leftScrollHover: boolean;
  constructor() {}
}