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

export enum NavLink {
   HOME = 'Home',
   ADMIN = 'Admin',
   USER_ADMIN = 'User Admin',
   PROFILE = 'Profile',
   SIGN_OUT = 'Sign Out',
   CONTACT_US = 'Contact Us',
   USER_SUPPORT = 'User Support',
   YOUR_WORKSPACES = 'Your Workspaces',
   FEATURED_WORKSPACES = 'Featured Workspaces',
}

export enum NavLinkIcon {
   HOME = 'home',
   ADMIN = 'user',
   CONTACT_US = 'envelope',
   USER_SUPPORT = 'help',
   YOUR_WORKSPACES = 'applications',
   FEATURED_WORKSPACES = 'star',
}
