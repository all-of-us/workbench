import * as React from "react";

import {Workspace, CdrVersion, CdrVersionListResponse} from 'generated/fetch';
import {WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';
import {getCdrVersion, getDefaultCdrVersion, hasDefaultCdrVersion} from "./cdr-versions";


const stubWorkspace: Workspace = {
    name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
    id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
    namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS
};

const defaultCdrVersion = cdrVersionListResponse.items[0];
const altCdrVersion = cdrVersionListResponse.items[1];

const testWorkspace: Workspace = {...stubWorkspace, cdrVersionId: defaultCdrVersion.cdrVersionId};
const testWorkspaceMissing: Workspace = {...stubWorkspace};

describe('cdr-versions', () => {
    it('should correctly get the CDR Version structure for a workspace', async () => {
        expect(getCdrVersion(testWorkspace, cdrVersionListResponse)).toBe(defaultCdrVersion);
    });

    it('should return undefined for a workspace with an invalid CDR Version ID', async () => {
        expect(getCdrVersion(testWorkspaceMissing, cdrVersionListResponse)).toBe(undefined);
    });

    it('should correctly get the default CDR Version structure', async () => {
        expect(getDefaultCdrVersion(cdrVersionListResponse)).toBe(defaultCdrVersion);
    });

    it('should correctly get an alternate default CDR Version structure', async () => {
        const altDefaultResponse = {...cdrVersionListResponse, defaultCdrVersionId: altCdrVersion.cdrVersionId}
        expect(getDefaultCdrVersion(altDefaultResponse)).toBe(altCdrVersion);
    });

    it('should detect when a Workspace has the default CDR Version structure', async () => {
        expect(hasDefaultCdrVersion(testWorkspace, cdrVersionListResponse)).toBeTruthy();
    });

    it('should detect when a Workspace does not have the default CDR Version structure', async () => {
        const altWorkspace = {...testWorkspace, cdrVersionId: altCdrVersion.cdrVersionId}
        expect(hasDefaultCdrVersion(altWorkspace, cdrVersionListResponse)).toBeFalsy();
    });
});
