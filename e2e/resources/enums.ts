export const config = require('resources/workbench-config');

export enum WORKSPACE_ACCESS_LEVEL {
   OWNER = 'OWNER',
   READER = 'READER',
   WRITER = 'WRITER',
}

export enum WORKSPACE_ACTION {
   DUPLICATE  = 'Duplicate',
   DELETE = 'Delete',
   EDIT = 'Edit',
   SHARE = 'Share',
}

export enum PAGE_URL {
   HOME = config.uiBaseUrl,
   WORKSPACES = config.uiBaseUrl + config.workspacesUrlPath,
   ADMIN = config.uiBaseUrl + config.adminUrlPath,
}

export enum NAV_LINK {
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

export enum NAV_LINK_ICON {
   HOME = 'home',
   ADMIN = 'user',
   CONTACT_US = 'envelope',
   USER_SUPPORT = 'help',
   YOUR_WORKSPACES = 'applications',
   FEATURED_WORKSPACES = 'star',
}
