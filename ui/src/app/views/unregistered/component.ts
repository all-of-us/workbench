import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationError, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {timer} from 'rxjs/observable/timer';

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

          // Start a new retryable sequence of observables here.
          return Observable.from([profile])
            // For now, we automatically submit ID verification as there's nothing
            // else to do in the application and only manual verification is supported.
            .flatMap((p) => {
              if (p.requestedIdVerification) {
                return Observable.from([p]);
              }
              return this.profileService.submitIdVerification();
            })
            // Per RW-695, ID verification is sufficient for initial internal
            // data access.
            .flatMap((p) => {
              if (p.termsOfServiceCompletionTime) {
                return Observable.from([p]);
              }
              return this.profileService.submitTermsOfService();
            })
            .flatMap((p) => {
              if (p.ethicsTrainingCompletionTime) {
                return Observable.from([p]);
              }
              return this.profileService.completeEthicsTraining();
            })
            .flatMap((p) => {
              if (p.demographicSurveyCompletionTime) {
                return Observable.from([p]);
              }
              return this.profileService.submitDemographicsSurvey();
            })
            .do((p) => {
              if (hasRegisteredAccess(p.dataAccessLevel)) {
                this.navigateAway();
              }
            })
            .retryWhen(ProfileStorageService.conflictRetryPolicy());
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
