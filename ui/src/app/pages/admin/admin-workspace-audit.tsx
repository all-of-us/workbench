import * as React from 'react';
import { useEffect } from 'react';
import { useParams } from 'react-router';

import { WorkspaceAuditLogQueryResponse } from 'generated/fetch';

import { AuditPageComponent } from 'app/components/admin/audit-page-component';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';
import { MatchParams } from 'app/utils/stores';

const getAuditLog = (subject: string) => {
  // Workspace actions take up many rows because of the Research Purpose fields
  const BQ_ROW_LIMIT = 1000;
  return workspaceAdminApi().getWorkspaceAuditLogEntries(subject, BQ_ROW_LIMIT);
};

const queryAuditLog = (subject: string) => {
  return getAuditLog(subject).then(
    (queryResult: WorkspaceAuditLogQueryResponse) => {
      return {
        actions: queryResult.actions,
        sourceId: queryResult.workspaceDatabaseId,
        query: queryResult.query,
        logEntries: queryResult.logEntries,
      };
    }
  );
};

const getNextAuditPath = (subject: string) => {
  return `/admin/workspace-audit/${subject}`;
};

const getAdminPageUrl = (subject: string) => {
  return `/admin/workspaces/${subject}`;
};

export const WorkspaceAudit = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  const { ns = '' } = useParams<MatchParams>();
  return (
    <AuditPageComponent
      auditSubjectType='Workspace'
      buttonLabel='Workspace namespace (begins with aou-rw-)'
      initialAuditSubject={ns}
      getNextAuditPath={getNextAuditPath}
      queryAuditLog={queryAuditLog}
      getAdminPageUrl={getAdminPageUrl}
    />
  );
};
