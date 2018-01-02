import {Component, Input} from '@angular/core';



@Component({
  selector: 'app-share',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ShareComponent {
  @Input() shareHover: boolean;
  constructor() {}
}
