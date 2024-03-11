export const workspacePath = (namespace: string, id: string): string =>
  `/workspaces/${namespace}/${id}`;

// internal name of the analysis tab, also used to construct URLs
export const analysisTabName = 'analysis';

export const analysisTabPath = (namespace: string, id: string): string =>
  `${workspacePath(namespace, id)}/${analysisTabName}`;

export const dataTabPath = (namespace: string, id: string): string =>
  `${workspacePath(namespace, id)}/data`;

export const appDisplayPath = (
  namespace: string,
  id: string,
  appType: string
): string => `${analysisTabPath(namespace, id)}/userApp/${appType}`;
