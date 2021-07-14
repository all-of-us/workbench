import {AuditPageComponent} from 'app/components/admin/audit-page-component';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {profileApi} from 'app/services/swagger-fetch-clients';
import * as React from 'react';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';

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

const getAdminPageUrl = (subject: string) => {
  return [`/admin/users/${subject}`];
};

export const UserAudit = (spinnerProps: WithSpinnerOverlayProps) => {
  useEffect(() => spinnerProps.hideSpinner(), []);

  const {username = ''} = useParams<{username: string}>();
  return <AuditPageComponent auditSubjectType='User'
                             buttonLabel='Username without domain'
                             initialAuditSubject={username}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}
                             getAdminPageUrl={getAdminPageUrl}/>;
};
