// UI Component framing the overall app (title and nav).
// Content is in other Components.

import {environment} from 'environments/environment';
import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {Router, NavigationEnd, ActivatedRoute} from '@angular/router';
import {SignInService, SignInDetails} from 'app/services/sign-in.service';
import {Title} from '@angular/platform-browser';
import {Observable} from 'rxjs/Observable';
import {CohortsService, ProfileService, Configuration, ConfigurationParameters} from 'generated';

declare const gapi: any;

@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  private baseTitle: string;
  user: Observable<SignInDetails>;

  constructor(
      private titleService: Title,
      private activatedRoute: ActivatedRoute,
      private router: Router,
      private signInService: SignInService,
      private cohortsService: CohortsService,
      private profileService: ProfileService
  ) {}

  ngOnInit(): void {
    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.

    this.baseTitle = this.titleService.getTitle();
    if (environment.displayTag) {
      this.baseTitle = `[${environment.displayTag}] ${this.baseTitle}`;
      this.titleService.setTitle(this.baseTitle);
    }

    // After navigation events, get the "title" value of the current Route and
    // include it in the web page title.
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        let currentRoute = this.activatedRoute;
        while (currentRoute.firstChild) {
          currentRoute = currentRoute.firstChild;
        }
        if (currentRoute.outlet === 'primary') {
          currentRoute.data.subscribe(value =>
              this.titleService.setTitle(`${value.title} | ${this.baseTitle}`));
        }
      }
    });
    this.user = this.signInService.user;
    alert("HELLO")
    this.profileService.getMe().subscribe(x => {
      alert(x.dataAccessLevel)
      alert(x.contactEmail)
      alert(x.fullName)
      this.profileService.register({ contactEmail: 'danrodney@hotmail.com', fullName: 'Dan Rodney',
         givenName: 'Dan', familyName: 'Rodney'}).subscribe(z => alert(z), b => alert(b))
    }, e => console.error(e))
  }

  signIn(e: Event): void {
    this.signInService.signIn();
  }

  signOut(e: Event): void {
    this.signInService.signOut();
  }
}
