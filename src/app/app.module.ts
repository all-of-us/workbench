import { BrowserModule }       from '@angular/platform-browser';
import { FormsModule }         from '@angular/forms';
import { NgModule }            from '@angular/core';

import { AppComponent }        from './app.component';
import { AppRoutingModule }    from './app-routing.module'
import { DashboardComponent }  from './dashboard.component'
import { HeroDetailComponent } from './hero-detail.component'
import { HeroService }         from './hero.service'
import { HeroesComponent }     from './heroes.component'

@NgModule({
  imports:      [
    AppRoutingModule,
    BrowserModule,
    FormsModule
  ],
  declarations: [
    AppComponent,
    DashboardComponent,
    HeroDetailComponent,
    HeroesComponent,
  ],
  providers: [ HeroService ],
  bootstrap: [ AppComponent ]
})
export class AppModule { }
