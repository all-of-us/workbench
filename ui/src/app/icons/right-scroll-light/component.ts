import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-right-scroll-light-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RightScrollLightComponent {
  @Input() rightScrollHover: boolean;
  constructor() {}
}
