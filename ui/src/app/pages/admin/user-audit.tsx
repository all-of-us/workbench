import {profileApi, workspaceAdminApi} from 'app/services/swagger-fetch-clients';
import * as React from 'react';
import {useParams} from 'react-router-dom';
import {AuditPageComponent} from '../../components/admin/audit-page-component';

const getAuditLog = (subject: string) => {
  const bqRowLimit = 1000; // Workspaces take many rows because of the Research Purpose fields
  return profileApi().getAuditLogEntries(subject, bqRowLimit);
};

const queryAuditLog = (subject: string) => {
  return getAuditLog(subject).then((queryResult) => {
    return {
      actions: queryResult.actions,
      sourceId: queryResult.userDatabaseId,
      query: queryResult.query,
      logEntries: queryResult.logEntries
    };
  });
};

const getNextAuditPath = (subject: string) => {
  return `/admin/user-audit/${subject}`;
};

export const UserAuditPage = () => {
  const {username = ''} = useParams();
  return <AuditPageComponent auditSubjectType='User'
                             initialAuditSubject={username}
                             debug={true}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}/>;
};
