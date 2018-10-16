import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

export interface Breadcrumb {
  label: string;
  url: string;
}
@Component({
  selector: 'app-breadcrumb',
  templateUrl: './component.html',
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    './component.css']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  subscription: Subscription;
  breadcrumbs: Breadcrumb[];
  constructor(
      private activatedRoute: ActivatedRoute,
      private router: Router) {}

  /**
   * Generate a breadcrumb using the default label and url. Uses the route's
   * paramMap to do any necessary variable replacement. For example, if we
   * have a label value of ':wsid' as defined in a route's breadcrumb, we can
   * do substitution with the 'wsid' value in the route's paramMap.
   */
  private static makeBreadcrumb(label: string,
                                url: string,
                                route: ActivatedRoute): Breadcrumb {
    let newLabel = label;
    // Perform variable substitution in label only if needed.
    if (newLabel.indexOf(':') >= 0) {
      const paramMap = route.snapshot.paramMap;
      for (const k of paramMap.keys) {
        newLabel = newLabel.replace(':' + k, paramMap.get(k));
      }
    }
    return {
      label: newLabel,
      url: url
    };
  }

  ngOnInit() {
    this.breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);

    this.subscription = this.router.events.filter(event => event instanceof NavigationEnd)
      .subscribe(event => {
        this.breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  /**
   * Returns array of Breadcrumb objects that represent the breadcrumb trail.
   * Derived from current route in conjunction with the overall route structure.
   */
  private buildBreadcrumbs(route: ActivatedRoute,
                           url: string = '',
                           breadcrumbs: Breadcrumb[] = []): Array<Breadcrumb> {
    const ROUTE_DATA_BREADCRUMB = 'breadcrumb';
    const children: ActivatedRoute[] = route.children;
    if (children.length === 0) {
      return breadcrumbs;
    }
    for (const child of children) {
      if (!child.snapshot.data.hasOwnProperty(ROUTE_DATA_BREADCRUMB)) {
        return this.buildBreadcrumbs(child, url, breadcrumbs);
      }
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');
      if (routeURL.length > 0) {
        url += `/${routeURL}`;
      }
      let label = child.snapshot.data[ROUTE_DATA_BREADCRUMB];
      if (label === 'Param: Workspace Name') {
        label = child.snapshot.data['workspace'].name;
      }
      if (label === 'Param: Cohort Name') {
        label = child.snapshot.data['cohort'].name;
      }
      if (label === 'Param: Concept Sets Name') {
        label = child.snapshot.data['workspace'].name;
        if (breadcrumbs.length > 2) {
          breadcrumbs = breadcrumbs.filter(b => !b.url.endsWith('/concepts'));
        }
      }
      if (label === 'Param: Concept Set Name') {
        label = child.snapshot.data['conceptSet'].name;
        // For the most part, we don't want to append the current label to the breadcrumbs
        //      since we want the label to stop at the parent.
        // In this case, we want the breadcrumb header to be the Concept Set Name
        const conceptSetBreadcrumb = BreadcrumbComponent.makeBreadcrumb(label, url, child);
        breadcrumbs.push(conceptSetBreadcrumb);
      }
      // Return if  child is a leaf, or if child's child has same breadcrumb as its parent
      if ((!child.firstChild) ||
          (child.firstChild.snapshot.data[ROUTE_DATA_BREADCRUMB] === label)) {
        console.log(breadcrumbs);
        return breadcrumbs;
      }
      // Prevent processing children with duplicate urls
      if (!breadcrumbs.some(b => b.url === url)) {
        const breadcrumb = BreadcrumbComponent.makeBreadcrumb(label, url, child);
        breadcrumbs.push(breadcrumb);
      }
      return this.buildBreadcrumbs(child, url, breadcrumbs);
    }

  }

}
