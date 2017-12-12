import {Directive, ElementRef, HostListener, OnInit, Renderer2} from '@angular/core';

const pixel = (n: number) => `${n}px`;
const ONE_REM = 24;  // value in pixels

@Directive({selector: '[appFullPage]'})
export class FullPageDirective implements OnInit {

  constructor(
    private element: ElementRef,
    private renderer: Renderer2,
  ) {}

  ngOnInit() {
    const el = this.element.nativeElement;
    this.renderer.setStyle(el, 'background-color', 'rgb(250, 250, 250)');
    this.renderer.setStyle(el, 'padding', '1rem');
    this.renderer.setStyle(el, 'margin-bottom', '1rem');
    this.renderer.setStyle(el, 'position', 'relative');
    this._updateWrapperDimensions();
  }

  @HostListener('window:resize')
  onResize() {
    this._updateWrapperDimensions();
  }

  _updateWrapperDimensions() {
    const nativeEl = this.element.nativeElement;
    const {top} = nativeEl.getBoundingClientRect();
    const minHeight = pixel(window.innerHeight - top - ONE_REM);
    this.renderer.setStyle(nativeEl, 'min-height', minHeight);
  }
}
