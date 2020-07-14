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
    console.log(queryResult.query);
    return {
      actions: queryResult.actions,
      sourceId: queryResult.userDatabaseId,
      query: queryResult.query,
      logEntries: queryResult.logEntries
    };
  }).then(genericQueryResult => {
    // TODO(jaycarlton): This is a workaround for LOGIN event issues on the backend. Can be removed when that patch is in.
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

export const UserAuditPage = () => {
  const {username = ''} = useParams();
  return <AuditPageComponent auditSubjectType='User'
                             buttonLabel='Username without domain'
                             initialAuditSubject={username}
                             logVerbose={false}
                             getNextAuditPath={getNextAuditPath}
                             queryAuditLog={queryAuditLog}/>;
};
