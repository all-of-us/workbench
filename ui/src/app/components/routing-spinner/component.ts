import {
  Component,
  ElementRef,
  Input,
  NgZone,
  OnInit,
  Renderer2,
  ViewChild,
} from '@angular/core';

import {
  Event as RouterEvent,
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
} from '@angular/router';

@Component({
  selector: 'app-routing-spinner',
  templateUrl: './component.html',
  styleUrls: ['./component.css'],
})
export class RoutingSpinnerComponent implements OnInit {
  @ViewChild('pageSpinner') pageSpinner: ElementRef;
  @Input('alwaysShow') alwaysShow = false;

  constructor(
    private renderer: Renderer2,
    private router: Router,
    private zone: NgZone,
  ) {}

  ngOnInit() {
    if (this.alwaysShow) {
      this.showSpinner();
      return;
    }
    /* TODO: The spinner should only show if the route is taking a certain
     * amount of time to load.  Also I'm not convinced that this is actually
     * activating for all the router start / end events we want it to.  But the
     * component exists and can be iterated on */
    this.router.events.subscribe((event: RouterEvent) => {
      if (this.isStart(event)) {
        this.showSpinner();
      } else if (this.isEnd(event)) {
        this.hideSpinner();
      }
    });
  }

  private isEnd(event: RouterEvent) {
    return event instanceof NavigationEnd
      || event instanceof NavigationError
      || event instanceof NavigationCancel;
  }

  private isStart(event: RouterEvent) {
    return event instanceof NavigationStart;
  }

  get spinner() {
    return this.pageSpinner.nativeElement;
  }

  get spinnerContainer() {
    return this.renderer.parentNode(this.spinner);
  }

  private showSpinner() {
    this.zone.runOutsideAngular(() => {
      this.renderer.setStyle(this.spinnerContainer, 'display', 'block');
    });
  }

  private hideSpinner() {
    this.zone.runOutsideAngular(() => {
      this.renderer.setStyle(this.spinnerContainer, 'display', 'none');
    });
  }
}
