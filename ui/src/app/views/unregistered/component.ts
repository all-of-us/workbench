import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {hasRegisteredAccess} from 'app/utils';

import {
  IdVerificationStatus,
  ProfileService,
} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class UnregisteredComponent implements OnInit, OnDestroy {
  IdVerificationStatus = IdVerificationStatus;
  idvStatus: IdVerificationStatus = null;
  private profileSub: Subscription;

  constructor(
    private profileService: ProfileService,
    private serverConfigService: ServerConfigService,
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
              if (p.trainingCompletionTime) {
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
            .retryWhen(ProfileStorageService.conflictRetryPolicy())
            .flatMap((p) => {
              if (!hasRegisteredAccess(p.dataAccessLevel)) {
                return Observable.from([p]);
              }
              // The profile just got registered access, so we need to reload
              // the cached profile before we can navigate away (else we'll hit
              // the registration guard and be sent back to this page again).
              // This reload process is clunky and involves an extra request,
              // RW-1057 tracks improvements to this.
              this.profileStorageService.reload();
              return this.profileStorageService.profile$
                .do(() => {
                  // Note: We cannot just do a .first() on profile$ because this
                  // subscription may trigger before or after the above reload()
                  // takes effect.
                  if (hasRegisteredAccess(p.dataAccessLevel)) {
                    this.navigateAway();
                  }
                });
            });
          })
        .subscribe();
    });
  }

  ngOnDestroy() {
    if (this.profileSub) {
      this.profileSub.unsubscribe();
    }
  }

  /** Exposed for testing. */
  setProfileService(svc: ProfileService) {
    this.profileService = svc;
  }

  private navigateAway() {
    this.router.navigateByUrl(this.activatedRoute.snapshot.params.from || '/')
      .catch(() => {
        this.router.navigateByUrl('/');
      });
  }
}
