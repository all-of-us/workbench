import {config} from 'resources/workbench-config';

export enum WorkspaceAccessLevel {
   OWNER = 'OWNER',
   READER = 'READER',
   WRITER = 'WRITER',
}

export enum WorkspaceAction {
   DUPLICATE  = 'Duplicate',
   DELETE = 'Delete',
   EDIT = 'Edit',
   SHARE = 'Share',
}

export enum PageUrl {
   HOME = config.uiBaseUrl,
   WORKSPACES = config.uiBaseUrl + config.workspacesUrlPath,
   ADMIN = config.uiBaseUrl + config.adminUrlPath,
}

export enum PageTab {
   DATA = 'DATA',
   ANALYSIS = 'ANALYSIS',
   ABOUT = 'ABOUT'
}
