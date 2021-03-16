import {AuditPageComponent} from 'app/components/admin/audit-page-component';
import {workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceAuditLogQueryResponse} from 'generated';
import * as React from 'react';
import {useParams} from 'react-router';

const getAuditLog = (subject: string) => {
  // Workspace actions take up many rows because of the Research Purpose fields
  const BQ_ROW_LIMIT = 1000;
  return workspaceAdminApi().getAuditLogEntries(subject, BQ_ROW_LIMIT);
};

const queryAuditLog = (subject: string) => {
  return getAuditLog(subject).then((queryResult: WorkspaceAuditLogQueryResponse) => {
    return {
      actions: queryResult.actions,
      sourceId: queryResult.workspaceDatabaseId,
      query: queryResult.query,
      logEntries: queryResult.logEntries
    };
  });
};

const getNextAuditPath = (subject: string) => {
  return `/admin/workspace-audit/${subject}`;
};


const getAdminPageUrl = (subject: string) => {
  return [`/admin/workspaces/${subject}`];
};

export const WorkspaceAudit = () => {
  const {workspaceNamespace = ''} = useParams<{ workspaceNamespace: string}>();
  return <AuditPageComponent auditSubjectType='Workspace'
                             buttonLabel='Workspace namespace (begins with aou-rw-)'
                             initialAuditSubject={workspaceNamespace}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}
                             getAdminPageUrl={getAdminPageUrl}/>;
};
