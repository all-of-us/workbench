import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnInit,
  ViewChild
} from '@angular/core';

@Component({
  selector: 'app-page-template-signed-out',
  templateUrl: './page-template-signed-out.component.html',
  styleUrls: ['./page-template-signed-out.component.css']
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
  @Input() imageSrc = '';
  @Input() smallerImageSrc = '';
  headerImg = '/assets/db-images/All_Of_Us_Logo.svg';
  dbHeaderImg = '/assets/db-images/Data_Browser_Logo.svg';
  @ViewChild('template') template: ElementRef;
  constructor() {}

  ngOnInit() {
    this.updateImages();
  }

  ngOnChanges() {
    this.updateImages();
  }

  updateImages(): void {
    if (this.template !== undefined) {
      this.template.nativeElement.style
          .setProperty('--smaller-image', `url("${this.smallerImageSrc}")`);
      this.template.nativeElement.style.setProperty('--larger-image', `url("${this.imageSrc}")`);
    }
  }
}
