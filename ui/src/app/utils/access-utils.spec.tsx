import * as React from "react";

import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {Profile, RenewableAccessModuleStatus} from 'generated/fetch';
import {
    maybeDaysRemaining,
    MILLIS_PER_DAY,
    NOTIFICATION_THRESHOLD_DAYS
} from "app/utils/access-utils";
import ModuleNameEnum = RenewableAccessModuleStatus.ModuleNameEnum;

// 10 minutes, in millis
const SHORT_TIME_BUFFER = 10 * 60 * 1000;

// return a time (in epoch millis) which is today + `days` days, plus a short buffer
const todayPlusDays = (days: number): number => {
    return Date.now() + (MILLIS_PER_DAY * days) + SHORT_TIME_BUFFER ;
}

const noModules: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    renewableAccessModules: {}
}

const noExpModules: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    renewableAccessModules: {
        modules: [{
            moduleName: ModuleNameEnum.ComplianceTraining,
            hasExpired: false,
            expirationEpochMillis: undefined,
        }]
    }
}

const laterExpiration: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    renewableAccessModules: {
        modules: [{
            moduleName: ModuleNameEnum.ComplianceTraining,
            hasExpired: false,
            expirationEpochMillis: todayPlusDays(NOTIFICATION_THRESHOLD_DAYS + 1),
        }]
    }
}

const expirationsInWindow: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    renewableAccessModules: {
        modules: [{
            moduleName: ModuleNameEnum.ComplianceTraining,
            hasExpired: false,
            expirationEpochMillis: todayPlusDays(5),
        },
        {
            moduleName: ModuleNameEnum.DataUseAgreement,
            hasExpired: false,
            expirationEpochMillis: todayPlusDays(10),
        }]
    }
}

const thirtyDaysPlusExpiration: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    renewableAccessModules: {
        modules: [{
            moduleName: ModuleNameEnum.ComplianceTraining,
            hasExpired: false,
            expirationEpochMillis: todayPlusDays(30),
        },
        {
            moduleName: ModuleNameEnum.DataUseAgreement,
            hasExpired: false,
            expirationEpochMillis: todayPlusDays(31),
        }]
    }
}

describe('maybeDaysRemaining', () => {
    it('returns undefined when the profile has no renewableAccessModules', () => {
         expect(maybeDaysRemaining(noModules)).toBeUndefined();
    });

    it('returns undefined when the profile has no renewableAccessModules with expirations', () => {
         expect(maybeDaysRemaining(noExpModules)).toBeUndefined();
    });

    it('returns undefined when the renewableAccessModules have expirations past the window', () => {
        expect(maybeDaysRemaining(laterExpiration)).toBeUndefined();
    });

    it('returns the soonest of all expirations within the window', () => {
        expect(maybeDaysRemaining(expirationsInWindow)).toEqual(5);
    });

    // regression test for RW-7108
    it('returns 30 days when the max expiration is between 30 and 31 days', () => {
        expect(maybeDaysRemaining(thirtyDaysPlusExpiration)).toEqual(30);
    });
});
