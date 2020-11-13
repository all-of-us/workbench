import {CdrVersion, CdrVersionListResponse, Workspace} from 'generated/fetch';

function getCdrVersion(workspace: Workspace, cdrList: CdrVersionListResponse): CdrVersion {
  return cdrList.items.find(v => v.cdrVersionId === workspace.cdrVersionId);
}

function getDefaultCdrVersion(cdrList: CdrVersionListResponse): CdrVersion {
  return cdrList.items.find(v => v.cdrVersionId === cdrList.defaultCdrVersionId);
}

function hasDefaultCdrVersion(workspace: Workspace, cdrList: CdrVersionListResponse): boolean {
  return workspace.cdrVersionId === cdrList.defaultCdrVersionId;
}

export {
    getCdrVersion,
    getDefaultCdrVersion,
    hasDefaultCdrVersion,
};
