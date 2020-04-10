// @ts-nocheck

export enum workspaceAccessLevel {
   OWNER = 'OWNER',
   READER = 'READER',
   WRITER = 'WRITER',
}

export enum workspaceAction {
   DUPLICATE  = 'Duplicate',
   DELETE = 'Delete',
   EDIT = 'Edit',
   SHARE = 'Share',
}

const configs = require('../resources/workbench-config');

export enum pageUrl {
   HOME = configs.uiBaseUrl,
   WORKSPACES = configs.uiBaseUrl + configs.workspacesUrlPath,
   ADMIN = configs.uiBaseUrl + configs.adminUrlPath,
}

export enum sideNavLink {
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

export enum sideNavLinkIcon {
   HOME = 'home',
   ADMIN = 'user',
   CONTACT_US = 'envelope',
   USER_SUPPORT = 'help',
   YOUR_WORKSPACES = 'applications',
   FEATURED_WORKSPACES = 'star',
}
