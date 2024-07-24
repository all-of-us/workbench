export const workspacePath = (namespace: string, terraName: string): string =>
  `/workspaces/${namespace}/${terraName}`;

// internal name of the analysis tab, also used to construct URLs
export const analysisTabName = 'analysis';

export const analysisTabPath = (namespace: string, terraName: string): string =>
  `${workspacePath(namespace, terraName)}/${analysisTabName}`;

export const dataTabPath = (namespace: string, terraName: string): string =>
  `${workspacePath(namespace, terraName)}/data`;

export const appDisplayPath = (
  namespace: string,
  terraName: string,
  appType: string
): string => `${analysisTabPath(namespace, terraName)}/userApp/${appType}`;
