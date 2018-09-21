import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-left-scroll-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class LeftScrollComponent {
  @Input() leftScrollHover: boolean;
  constructor() {}
}
