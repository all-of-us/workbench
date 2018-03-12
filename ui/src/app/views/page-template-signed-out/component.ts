import {Component, Input, OnChanges, OnInit} from '@angular/core';

@Component({
  selector: 'app-page-template-signed-out',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
  @Input() imageSrc = '';
  @Input() smallerImageSrc = '';
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';
  templateElement: any;

  constructor() {}

  ngOnInit() {
    this.templateElement = document.getElementsByClassName('template')[0];
    this.updateImages();
  }

  ngOnChanges(changes) {
    this.updateImages();
  }

  updateImages(): void {
    if (this.templateElement !== undefined) {
      this.templateElement.style.setProperty('--smaller-image', `url("${this.smallerImageSrc}")`);
      this.templateElement.style.setProperty('--larger-image', `url("${this.imageSrc}")`);
    }
  }

}
