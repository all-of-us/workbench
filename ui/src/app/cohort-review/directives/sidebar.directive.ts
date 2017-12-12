import {
  Directive,
  ElementRef,
  HostListener,
  Input,
  OnInit,
  Renderer2,
} from '@angular/core';

interface Position {
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
}

@Directive({
  selector: '[appSidebar]',
  exportAs: 'appSidebar'
})
export class SidebarDirective implements OnInit {

  @Input() position: Position = {top: 0, left: 0};
  @Input() controller: HTMLElement;
  private _open = false;

  readonly styles: Object = {
    'height': '100%',
    'width': '0',
    'position': 'absolute',
    'z-index': '2',
    'background-color': 'rgb(250, 250, 250)',
    'overflow-x': 'hidden',
    'padding-top': '60px',
    'transition': '0.5s',
  };

  constructor(
    private element: ElementRef,
    private renderer: Renderer2,
  ) {}

  ngOnInit() {
    const nativeEl = this.element.nativeElement;
    const parentEl = this.renderer.parentNode(nativeEl);

    this.renderer.setStyle(parentEl, 'position', 'relative');

    Object.keys(this.styles).forEach(key => {
      const val = this.styles[key];
      this.renderer.setStyle(nativeEl, key, val);
    });

    Object.keys(this.position).forEach(key => {
      const val = this.position[key];
      this.renderer.setStyle(nativeEl, key, val);
    });
  }

  open() {
    const nativeEl = this.element.nativeElement;
    this.renderer.setStyle(nativeEl, 'width', '300px');

    const border = '1px solid rgb(86, 86, 86)';
    const borderRadius = '5px';

    this.renderer.setStyle(nativeEl, 'border', border);
    this.renderer.setStyle(nativeEl, 'border-radius', borderRadius);

    this._open = true;
  }

  close() {
    const nativeEl = this.element.nativeElement;
    this.renderer.setStyle(nativeEl, 'width', '0');
    this.renderer.removeStyle(nativeEl, 'border');
    this.renderer.removeStyle(nativeEl, 'border-radius');
    this._open = false;
  }

  @HostListener('document:click', ['$event'])
  onClick(event) {
    const inSidebar = this.element.nativeElement.contains(event.target);
    const inController = this.controller.contains(event.target);
    if (this._open && !(inSidebar || inController)) {
      this.close();
    }
  }

  get isOpen() {
    return this._open;
  }
}
