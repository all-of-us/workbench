import { environment } from 'environments/environment';

export const workspacePath = (namespace: string, id: string): string =>
  `/workspaces/${namespace}/${id}`;

// internal name of the analysis tab, also used to construct URLs
export const analysisTabName = environment.showNewAnalysisTab
  ? 'analysis'
  : 'notebooks';

export const analysisTabPath = (namespace: string, id: string): string =>
  `${workspacePath(namespace, id)}/${analysisTabName}`;

export const dataTabPath = (namespace: string, id: string): string =>
  `${workspacePath(namespace, id)}/data`;

export const appTabPath = (
  namespace: string,
  id: string,
  appType: string
): string => `${analysisTabPath(namespace, id)}/userApp/${appType}`;
