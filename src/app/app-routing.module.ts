// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.

import { NgModule }                  from '@angular/core';
import { RouterModule, Routes }      from '@angular/router'

import { LoginComponent }            from './login.component'
import { SelectRepositoryComponent } from './select-repository.component'
import { CohortBuilderComponent }    from './cohort-builder.component'

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'repository', component: SelectRepositoryComponent },
  { path: 'cohort/:id', component: CohortBuilderComponent }
];

@NgModule({
  imports: [ RouterModule.forRoot(routes) ],
  exports: [ RouterModule ]
})
export class AppRoutingModule {}
