import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationError, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {hasRegisteredAccess} from 'app/utils';

import {
  DataAccessLevel,
  IdVerificationStatus,
  ProfileService,
} from 'generated';

@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class UnregisteredComponent implements OnInit, OnDestroy {
  IdVerificationStatus = IdVerificationStatus;
  idvStatus: IdVerificationStatus;
  private profileSub: Subscription;

  constructor(
    private serverConfigService: ServerConfigService,
    private profileService: ProfileService,
    private profileStorageService: ProfileStorageService,
    private activatedRoute: ActivatedRoute,
    private router: Router) {}

  ngOnInit() {
    this.serverConfigService.getConfig().subscribe((config) => {
      if (!config.enforceRegistered) {
        this.navigateAway();
        return;
      }
      this.profileSub = this.profileStorageService.profile$
        .first()
        .flatMap((profile) => {
          if (hasRegisteredAccess(profile.dataAccessLevel)) {
            this.navigateAway();
            return Observable.from([profile]);
          }
          this.idvStatus = profile.idVerificationStatus;

          let obs = Observable.from([profile]);
          // For now, we automatically submit ID verification as there's nothing
          // else to do in the application and only manual verification is supported.
          if (!profile.requestedIdVerification) {
            obs = obs.flatMap((p) => {
              return this.profileService.submitIdVerification().map((_) => p);
            });
          }

          // For now, we automatically submit all placeholder user setup steps.
          // Per RW-695, ID verification is sufficient for data access for
          // initial internal data access.
          if (!profile.termsOfServiceCompletionTime) {
            obs = obs.flatMap((p) => {
              return this.profileService.submitTermsOfService().map((_) => p);
            });
          }
          if (!profile.ethicsTrainingCompletionTime) {
            obs = obs.flatMap((p) => {
              return this.profileService.completeEthicsTraining().map((_) => p);
            });
          }
          if (!profile.demographicSurveyCompletionTime) {
            obs = obs.flatMap((p) => {
              return this.profileService.submitDemographicsSurvey().map((_) => p);
            });
          }
          return obs;
        })
        .subscribe();
    });
  }

  ngOnDestroy() {
    if (this.profileSub) {
      this.profileSub.unsubscribe();
    }
  }

  private navigateAway() {
    this.router.navigateByUrl(this.activatedRoute.snapshot.params.from || '/')
      .catch(() => {
        this.router.navigateByUrl('/');
      });
  }
}
