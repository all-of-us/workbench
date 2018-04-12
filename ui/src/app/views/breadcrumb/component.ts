import {Component} from '@angular/core';
import {ActivatedRoute, NavigationEnd, PRIMARY_OUTLET, Router} from '@angular/router';

export interface Breadcrumb {
  label: string;
  url: string;
}

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    './component.css'],
  templateUrl: './component.html',
  selector: 'breadcrumb'
})

export class BreadcrumbComponent {

  breadcrumbs: Breadcrumb[];

  constructor(
      private activatedRoute: ActivatedRoute,
      private router: Router) {
    this.router.events.filter(event => event instanceof NavigationEnd).subscribe(event => {
      this.breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);
    });
  }

  /**
   * Returns array of Breadcrumb objects that represent the breadcrumb trail.
   * Derived from current route in conjunction with the overall route structure.
   *
   * @param {ActivatedRoute} route
   * @param {string} url
   * @param {Breadcrumb[]} breadcrumbs
   * @returns {Array<Breadcrumb>}
   */
  private buildBreadcrumbs(route: ActivatedRoute,
                           url: string = '',
                           breadcrumbs: Breadcrumb[] = []): Array<Breadcrumb> {

    const ROUTE_DATA_BREADCRUMB: string = "breadcrumb";

    //get the child routes
    let children: ActivatedRoute[] = route.children;

    //return if there are no more children
    if (children.length === 0) {
      return breadcrumbs;
    }

    //iterate over children
    for (let child of children) {

      //verify primary route
      if (child.outlet !== PRIMARY_OUTLET) {
        continue;
      }

      //verify the custom data property "breadcrumb" is specified on the route
      if (!child.snapshot.data.hasOwnProperty(ROUTE_DATA_BREADCRUMB)) {
        return this.buildBreadcrumbs(child, url, breadcrumbs);
      }

      //get the route's URL segment
      let routeURL: string = child.snapshot.url.map(segment => segment.path).join("/");

      //append route URL to URL
      if (routeURL.length > 0) {
        url += `/${routeURL}`;
      }

      const label = child.snapshot.data[ROUTE_DATA_BREADCRUMB];

      //make a new breadcrumb if needed
      if (!breadcrumbs.some(b => b.url === url)) {
        let breadcrumb = BreadcrumbComponent.makeBreadcrumb(label, url, child);
        breadcrumbs.push(breadcrumb);
      }

      //recursive
      return this.buildBreadcrumbs(child, url, breadcrumbs);
    }

  }

  /**
   * Generate a breadcrumb using the default label and url.
   * Uses the route's paramMap to do any necessary variable replacement.
   *
   * @param {String} label
   * @param {String} url
   * @param {ActivatedRoute} route
   * @returns {Breadcrumb}
   */
  private static makeBreadcrumb(
      label: string,
      url: string,
      route: ActivatedRoute): Breadcrumb {
    try {
      const paramValues = route.paramMap['source']['_value'];
      let newLabel = label;
      for (let k in paramValues) {
        newLabel = newLabel.replace(":" + k, paramValues[k]);
      }
      return {
        label: newLabel,
        url: url
      };
    } catch (e) {
      console.log(e);
      return {
        label: label,
        url: url
      };
    }
  }

}
