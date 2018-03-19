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
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class PageTemplateSignedOutComponent implements OnChanges, OnInit {
  @Input() imageSrc = '';
  @Input() smallerImageSrc = '';
  headerImg = '/assets/images/logo-registration-non-signed-in.svg';
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
