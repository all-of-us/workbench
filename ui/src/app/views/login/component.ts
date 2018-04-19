import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {
  ActivatedRoute,
  Router,
} from '@angular/router';

import {SignInService} from 'app/services/sign-in.service';

@Component({
  selector: 'app-signed-out',
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/headers.css'],
  templateUrl: './component.html'
})
export class LoginComponent implements OnInit {
  currentUrl: string;
  backgroundImgSrc = '/assets/images/login-group.png';
  smallerBackgroundImgSrc = '/assets/images/login-standing.png';
  googleIcon = '/assets/icons/google-icon.png';

  constructor(
    /* Ours */
    private signInService: SignInService,
    /* Angular's */
    private activatedRoute: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUrl = this.router.url;
    document.body.style.backgroundColor = '#e2e3e5';

    this.signInService.isSignedIn$.subscribe((signedIn) => {
      if (signedIn === true) {
        if (this.activatedRoute.snapshot.params.from === undefined) {
          this.router.navigateByUrl('/');
        } else {
          this.router.navigateByUrl(this.activatedRoute.snapshot.params.from);
        }
      }
    });
  }

  signIn(): void {
    this.signInService.signIn();
  }
}
