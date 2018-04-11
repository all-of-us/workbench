import {Component, Input} from '@angular/core';



@Component({
  selector: 'app-share-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ShareComponent {
  @Input() shareHover: boolean;
  constructor() {}
}
