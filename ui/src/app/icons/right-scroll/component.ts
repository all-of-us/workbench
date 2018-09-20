import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-right-scroll-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class RightScrollComponent {
  @Input() rightScrollHover: boolean;
  constructor() {}
}
