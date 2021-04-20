import * as React from "react";

import {Workspace} from 'generated/fetch';
import {WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {
    altCdrVersion,
    cdrVersionTiersResponse,
    controlledCdrVersion,
    defaultCdrVersion
} from 'testing/stubs/cdr-versions-api-stub';
import {getCdrVersion, hasDefaultCdrVersion, getDefaultCdrVersionForTier} from 'app/utils/cdr-versions';
import {AccessTierShortNames} from 'app/utils/access-tiers';

const stubWorkspace: Workspace = {
    name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
    id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
    namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS
};


const testWorkspace: Workspace = {
    ...stubWorkspace,
    cdrVersionId: defaultCdrVersion.cdrVersionId,
    accessTierShortName: AccessTierShortNames.Registered
};
const testWorkspaceMissingVersion: Workspace = {
    ...stubWorkspace,
    accessTierShortName: AccessTierShortNames.Registered
};
const testWorkspaceMissingTier: Workspace = {
    ...stubWorkspace,
    cdrVersionId: defaultCdrVersion.cdrVersionId
};
const ctWorkspace: Workspace = {
    ...stubWorkspace,
    cdrVersionId: controlledCdrVersion.cdrVersionId,
    accessTierShortName: AccessTierShortNames.Controlled
};

describe('cdr-versions', () => {
    it('should correctly get the CDR Version for a workspace', async () => {
        expect(getCdrVersion(testWorkspace, cdrVersionTiersResponse)).toBe(defaultCdrVersion);
    });

    it('should correctly get the CDR Version for a CT workspace', async () => {
        expect(getCdrVersion(ctWorkspace, cdrVersionTiersResponse)).toBe(controlledCdrVersion);
    });

    it('should return undefined for a workspace with an invalid CDR Version ID', async () => {
        expect(getCdrVersion(testWorkspaceMissingVersion, cdrVersionTiersResponse)).toBeUndefined();
    });

    it('should return undefined for a workspace with an invalid access tier', async () => {
        expect(getCdrVersion(testWorkspaceMissingTier, cdrVersionTiersResponse)).toBeUndefined();
    });

    it('should correctly get the default CDR Version for the registered tier', async () => {
        expect(getDefaultCdrVersionForTier(testWorkspace, cdrVersionTiersResponse)).toBe(defaultCdrVersion);
    });

    it('should correctly get the default CDR Version for the controlled tier', async () => {
        expect(getDefaultCdrVersionForTier(ctWorkspace, cdrVersionTiersResponse)).toBe(controlledCdrVersion);
    });

    it('should return undefined when the tier is invalid', async () => {
        expect(getDefaultCdrVersionForTier(testWorkspaceMissingTier, cdrVersionTiersResponse)).toBeUndefined();
    });

    it('should detect when a Workspace has the default CDR Version for its tier', async () => {
        expect(hasDefaultCdrVersion(testWorkspace, cdrVersionTiersResponse)).toBeTruthy();
    });

    it('should detect when a CT Workspace has the default CDR Version for its tier', async () => {
        expect(hasDefaultCdrVersion(ctWorkspace, cdrVersionTiersResponse)).toBeTruthy();
    });

    it('should detect when a Workspace does not have the default CDR Version', async () => {
        const altWorkspace = {...testWorkspace, cdrVersionId: altCdrVersion.cdrVersionId}
        expect(hasDefaultCdrVersion(altWorkspace, cdrVersionTiersResponse)).toBeFalsy();
    });

    it('should return false when a Workspace does not have a valid CDR Version', async () => {
        expect(hasDefaultCdrVersion(testWorkspaceMissingVersion, cdrVersionTiersResponse)).toBeFalsy();
    });

    it('should return false when a Workspace does not have a valid tier', async () => {
        expect(hasDefaultCdrVersion(testWorkspaceMissingTier, cdrVersionTiersResponse)).toBeFalsy();
    });
});
