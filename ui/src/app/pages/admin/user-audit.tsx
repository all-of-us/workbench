import {profileApi} from 'app/services/swagger-fetch-clients';
import * as fp from 'lodash/fp';
import * as React from 'react';


const {useEffect, useState} = React;

const firstTen = fp.flow(fp.slice(0, 10), fp.map(JSON.stringify));

export const UserAudit = () => {
  const [userActions, setUserActions] = useState();

  useEffect(() => {
    const getLogEntries = async() => {
      const {actions} = await profileApi().getAuditLogEntries('psantos');
      setUserActions(actions);
    };

    getLogEntries();
  }, []);

  return userActions ? <div>{firstTen(userActions)}</div> : <div>Loading Log Entries...</div>;
};
