import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {DbHomeComponent} from './views/db-home/db-home.component';
import {QuickSearchComponent} from './views/quick-search/quick-search.component';
import {SurveyViewComponent} from './views/survey-view/survey-view.component';
import {SurveysComponent} from './views/surveys/surveys.component';

declare let gtag: Function;
declare let ga_tracking_id: string;

const routes: Routes = [
  {
    path: '',
    component: DbHomeComponent,
    data: {title: 'Data Browser Home'}
  },
  {
    path: 'quick-search',
    component: QuickSearchComponent,
    data: {title: 'Quick Search'}
  },
  {
    path: 'surveys',
    component: SurveysComponent,
    data: {title: 'Browse Survey Instruments'}
  },
  {
    path: 'survey/:id',
    component: SurveyViewComponent,
    data: {title: 'View Survey Questions and Answers'}
  },
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
