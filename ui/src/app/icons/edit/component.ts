import {Component, Input} from '@angular/core';



@Component({
  selector: 'app-edit-icon',
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class EditComponent {
  @Input() editHover: boolean;
  constructor() {}
}
