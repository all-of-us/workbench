import {
  Directive,
  ElementRef,
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

  private _open = false;

  @Input() set isOpen(val: boolean) {
    val ? this.open() : this.close();
  }

  get isOpen() {
    return this._open;
  }

  readonly styles: Object = {
    'height': '100%',
    'width': '0',
    'position': 'absolute',
    'z-index': '1',
    'background-color': 'rgb(250, 250, 250)',
    'overflow-x': 'hidden',
    'padding-top': '1rem',
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

  toggle() {
    this.isOpen ? this.close() : this.open();
  }
}
