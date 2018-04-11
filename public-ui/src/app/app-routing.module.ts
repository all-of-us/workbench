import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {HomeComponent} from './data-browser/home/home.component';
import {SearchComponent} from './data-browser/search/search.component';

declare let gtag: Function;
declare let ga_tracking_id: string;

const routes: Routes = [
  {
    path: 'data-browser/home',
    component: HomeComponent,
    data: {title: 'Data Browser'}
  },
  {
    path: 'data-browser/browse',
    component: SearchComponent,
    data: {title: 'Browse'}
  },
  {
    path: '',
    component: SearchComponent,
    data: {title: 'Browse'}
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {onSameUrlNavigation: 'reload'})],
  exports: [RouterModule],
  providers: [
  ]
})
export class AppRoutingModule {

 constructor(public router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', ga_tracking_id, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
