import {AuditPageComponent} from 'app/components/admin/audit-page-component';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {AuditAction, AuditEventBundle} from 'generated';
import * as fp from 'lodash/fp';
import * as React from 'react';
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
  }).then(genericQueryResult => {
    // TODO(jaycarlton): This is a workaround for spammy LOGIN events on the backend (RW-5249).
    const filteredActions = fp.filter(
      (action: AuditAction) => fp.negate(
        fp.any((eventBundle: AuditEventBundle) => 'LOGIN' === eventBundle.header.actionType))
      (action.eventBundles))
    (genericQueryResult.actions);
    return {
      actions: filteredActions,
      sourceId: genericQueryResult.sourceId,
      query: genericQueryResult.query,
      logEntries: genericQueryResult.logEntries
    };
  });
};

const getNextAuditPath = (subject: string) => {
  return `/admin/user-audit/${subject}`;
};

// Single-user admin page isn't available yet, so go to the main users list page.
const getAdminPageUrl = (subject: string) => {
  return `/admin/user/`;
};

export const UserAuditPage = () => {
  const {username = ''} = useParams();
  return <AuditPageComponent auditSubjectType='User'
                             buttonLabel='Username without domain'
                             initialAuditSubject={username}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}
                             getAdminPageUrl={getAdminPageUrl}/>;
};
