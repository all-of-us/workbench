import * as React from 'react';
import {useParams} from 'react-router';
import {WorkspaceAuditLogQueryResponse} from '../../../generated';
import {AuditPageComponent} from '../../components/admin/audit-page-component';
import {workspaceAdminApi} from '../../services/swagger-fetch-clients';

const getAuditLog = (subject: string) => {
  const bqRowLimit = 1000; // Workspaces take many rows because of the Research Purpose fields
  return workspaceAdminApi().getAuditLogEntries(subject, bqRowLimit);
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

export const WorkspaceAuditPage = (props) => {
  const {workspaceNamespace = ''} = useParams();
  return <AuditPageComponent auditSubjectType='Workspace'
                             initialAuditSubject={workspaceNamespace}
                             debug={true}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}/>;
};
