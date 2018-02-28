import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-page-template-signed-out',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class PageTemplateSignedOutComponent {
  @Input() imageSrc = '';
  headerImg = '/assets/images/logo-registration_nonSignedIn.png';

  constructor() {}
}

