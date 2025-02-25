import { Workspace } from 'generated/fetch';

import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  getCdrVersion,
  getDefaultCdrVersionForTier,
  hasDefaultCdrVersion,
} from 'app/utils/cdr-versions';

import {
  altCdrVersion,
  cdrVersionTiersResponse,
  controlledCdrVersion,
  defaultCdrVersion,
} from 'testing/stubs/cdr-versions-api-stub';
import { WorkspaceStubVariables } from 'testing/stubs/workspaces';

const stubWorkspace: Workspace = {
  displayName: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
  terraName: WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
  namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
};

const testWorkspace: Workspace = {
  ...stubWorkspace,
  cdrVersionId: defaultCdrVersion.cdrVersionId,
  accessTierShortName: AccessTierShortNames.Registered,
};
const testWorkspaceMissingVersion: Workspace = {
  ...stubWorkspace,
  accessTierShortName: AccessTierShortNames.Registered,
};
const testWorkspaceMissingTier: Workspace = {
  ...stubWorkspace,
  cdrVersionId: defaultCdrVersion.cdrVersionId,
};
const ctWorkspace: Workspace = {
  ...stubWorkspace,
  cdrVersionId: controlledCdrVersion.cdrVersionId,
  accessTierShortName: AccessTierShortNames.Controlled,
};

describe('cdr-versions', () => {
  it('should correctly get the CDR Version for a workspace', async () => {
    expect(getCdrVersion(testWorkspace, cdrVersionTiersResponse)).toBe(
      defaultCdrVersion
    );
  });

  it('should correctly get the CDR Version for a CT workspace', async () => {
    expect(getCdrVersion(ctWorkspace, cdrVersionTiersResponse)).toBe(
      controlledCdrVersion
    );
  });

  it('should return undefined for a workspace with an invalid CDR Version ID', async () => {
    expect(
      getCdrVersion(testWorkspaceMissingVersion, cdrVersionTiersResponse)
    ).toBeUndefined();
  });

  it('should correctly get the default CDR Version for the registered tier', async () => {
    expect(
      getDefaultCdrVersionForTier(
        AccessTierShortNames.Registered,
        cdrVersionTiersResponse
      )
    ).toBe(defaultCdrVersion);
  });

  it('should correctly get the default CDR Version for the controlled tier', async () => {
    expect(
      getDefaultCdrVersionForTier(
        AccessTierShortNames.Controlled,
        cdrVersionTiersResponse
      )
    ).toBe(controlledCdrVersion);
  });

  it('should return undefined when the tier is invalid', async () => {
    expect(
      getDefaultCdrVersionForTier('invalid', cdrVersionTiersResponse)
    ).toBeUndefined();
  });

  it('should detect when a Workspace has the default CDR Version for its tier', async () => {
    expect(
      hasDefaultCdrVersion(testWorkspace, cdrVersionTiersResponse)
    ).toBeTruthy();
  });

  it('should detect when a CT Workspace has the default CDR Version for its tier', async () => {
    expect(
      hasDefaultCdrVersion(ctWorkspace, cdrVersionTiersResponse)
    ).toBeTruthy();
  });

  it('should detect when a Workspace does not have the default CDR Version', async () => {
    const altWorkspace = {
      ...testWorkspace,
      cdrVersionId: altCdrVersion.cdrVersionId,
    };
    expect(
      hasDefaultCdrVersion(altWorkspace, cdrVersionTiersResponse)
    ).toBeFalsy();
  });

  it('should return false when a Workspace does not have a valid CDR Version', async () => {
    expect(
      hasDefaultCdrVersion(testWorkspaceMissingVersion, cdrVersionTiersResponse)
    ).toBeFalsy();
  });

  it('should return false when a Workspace does not have a valid tier', async () => {
    expect(
      hasDefaultCdrVersion(testWorkspaceMissingTier, cdrVersionTiersResponse)
    ).toBeFalsy();
  });
});
