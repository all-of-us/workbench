import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import { HomeComponent } from './home/home.component';
import { SearchComponent } from './search/search.component';


@NgModule({
  imports: [RouterModule.forChild([
    /* Define routes here */
    {
      path: 'data-browser/home',
      component: HomeComponent,

    }, {
      path: 'data-browser/browse',
      component: SearchComponent,
      data: {title: 'Browse'}
    }
  ])],
  exports: [RouterModule]
})
export class DataBrowserRouter {}
