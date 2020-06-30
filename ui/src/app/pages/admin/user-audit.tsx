import { ProfileApiFp } from 'generated/fetch';
import * as React from 'react';

const {useEffect, useState} = React;

export const UserAudit = () => {
  const [actions, setActions] = useState();
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    const getLogEntries = async() => {
      const auditResponse = await ProfileApiFp().getAuditLogEntries('psantos');
      console.log(auditResponse);
      setActions(auditResponse);
      setBusy(false);
    };

    getLogEntries();
  }, []);

  return <div>logEntries</div>;
};
