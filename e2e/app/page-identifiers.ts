import {config} from 'resources/workbench-config';

export enum WorkspaceAccessLevel {
   Owner = 'OWNER',
   Reader = 'READER',
   Writer = 'WRITER',
}

export enum EllipsisMenuAction {
   Duplicate  = 'Duplicate',
   Delete = 'Delete',
   Edit = 'Edit',
   Share = 'Share',
   Review = 'Review',
   RenameDataset = 'Rename Dataset',
}

export enum PageUrl {
   Home = config.uiBaseUrl,
   Workspaces = config.uiBaseUrl + config.workspacesUrlPath,
   Admin = config.uiBaseUrl + config.adminUrlPath,
}
