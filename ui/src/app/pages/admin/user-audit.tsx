import {profileApi} from 'app/services/swagger-fetch-clients';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {UserAuditLogQueryResponse} from '../../../generated';
import {AuditActionCard} from '../audit/audit-action-card';
import {actionToString} from 'app/utils/audit-utils';

const {useEffect, useState} = React;

// const firstTen = fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));
const firstTen = (res: UserAuditLogQueryResponse) => {
  return JSON.stringify(res.actions);
};

// fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));
export const UserAudit = () => {
  const [userActions, setUserActions] = useState();

  useEffect(() => {
    const getLogEntries = async() => {
      const usernameWithoutGsuiteDomain = 'jaycarlton';
      const limit = 50;
      const {actions} = await profileApi().getAuditLogEntries(usernameWithoutGsuiteDomain, limit);
      const renderedString = actions.map(action => actionToString(action)).join('<br/>');
      console.log(renderedString);
      console.log(actions);
      // setUserActions(actions);
      setUserActions(renderedString);
    };

    getLogEntries();
  }, []);

  // return userActions ? <div>{firstTen(userActions)}</div> : <div>Loading Log Entries...</div>;
  return userActions ? <div>{userActions}</div> : <div>Loading Log Entries...</div>;

  // return <AuditActionCard action={actions[0]}/>;
};
