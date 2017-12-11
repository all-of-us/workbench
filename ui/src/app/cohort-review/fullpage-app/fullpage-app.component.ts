import {Component, ElementRef, HostListener, OnInit, ViewChild} from '@angular/core';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Component({
  selector: 'app-fullpage-app',
  templateUrl: './fullpage-app.component.html',
  styleUrls: ['./fullpage-app.component.css']
})
export class FullpageAppComponent implements OnInit {
  @ViewChild('wrapper') wrapper: ElementRef;

  ngOnInit() {
    this._updateWrapperDimensions();
  }

  @HostListener('window:resize')
  onResize() {
    this._updateWrapperDimensions();
  }

  _updateWrapperDimensions() {
    const element = this.wrapper.nativeElement;
    const {top} = element.getBoundingClientRect();
    element.style.minHeight = pixel(window.innerHeight - top - ONE_REM);
  }
}
