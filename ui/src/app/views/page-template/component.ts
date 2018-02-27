import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-page-template',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class PageTemplateComponent {
  @Input() imageSrc: string;
  headerImg: string;

  constructor() {
    this.imageSrc = '';
    this.headerImg = '/assets/images/AoU-logo-registration_nonSignedIn.png';
  }
}

