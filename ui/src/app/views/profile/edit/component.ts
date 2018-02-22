import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileEditComponent implements OnInit {
  profile: Profile;
  profileLoaded = false;
  constructor(
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

  submitChanges(): void {
    this.profileService.updateProfile(this.profile).subscribe(() => {
        this.router.navigate(['../'], {relativeTo : this.route});
      }
    );
  }
}
