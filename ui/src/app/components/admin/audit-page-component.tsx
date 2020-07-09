import {AuditActionCardListView} from 'app/components/admin/audit-card-list-view';
import {Navigate} from 'app/components/app-router';
import {Button} from 'app/components/buttons';
import {TextInputWithLabel} from 'app/components/inputs';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {actionToString} from 'app/utils/audit-utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {useParams} from 'react-router-dom';
import {AuditLogEntry} from '../../../generated';
import {AuditAction} from '../../../generated/model/auditAction';

const {useEffect, useState} = React;

// The APIs for user and workspace audit are slightly different, and typed differently,
// because of the need to return different database IDs and not get them confused (in future endpoints).
// This interface allows an easy mapping from both real query response types.
export interface GenericAuditQueryResult {
  /**
   * Re-constructed objects for each composite action in the query results.
   */
  actions?: Array<AuditAction>;
  logEntries: Array<AuditLogEntry>;
  /**
   * Copy of the query used in BigQuery to assist in further exploration or debugging.
   */
  query: string;
  /**
   * ID in the MySQL database and BigQuery Audit Database for the this qyery. Currently either
   * a userId or workspaceId as appropriate. This may be needed for situations (such as workspacd audit)
   * where it's not obvious in the responses which workspace is the one you audited.
   */
  sourceId: number;
}

// Common properties for User & Workspace (and similar future pages).
// Assmptions: the path parameter is called initially with the audit API subject.
//   (this will need revisiting for multi-subject queries)
export interface AuditPageProps {
  initialAuditSubject?: string;
  auditSubjectType: string;
  queryAuditLog: (subject: string) => Promise<GenericAuditQueryResult>;
  getNextAuditPath: (subject: string) => string;
  debug: boolean;
}

export const AuditPageComponent = (props: AuditPageProps) => {
  const {initialAuditSubject, queryAuditLog, getNextAuditPath, debug, auditSubjectType} = props;
  const [actions, setActions] = useState([]);
  const [nextAuditSubject, setNextAuditSubject] = useState('');
  const [navigateTo, setNavigateTo] = useState(false);
  const [loading, setLoading] = useState(true);
  const [dbId, setDbId] = useState();
  const [bqAuditQery, setBqAuditQuery] = useState('');

  useEffect(() => {
    setNextAuditSubject(initialAuditSubject);
  }, []);

  useEffect(() => {
    const getLogEntries = async() => {
      setLoading(true);
      const rowLimit = 500; // rows to fetch from BigQuery audit table. It's not possible to request a specific number of Actions.
      try {
        const queryResult: GenericAuditQueryResult = await queryAuditLog(nextAuditSubject);
        setActions(queryResult.actions);
        setDbId(queryResult.sourceId);
        setBqAuditQuery(queryResult.query);
        const actionDebugString = actions.map(action => actionToString(action)).join('\n');
        if (debug) {
          console.log(actionDebugString);
          console.log(actions);
          console.log(bqAuditQery);
        }
      } catch (e) {
        console.warn(`Error retrieving audit query results for ${nextAuditSubject}`);
        setActions([]);
      }
      setLoading(false);
    };

    nextAuditSubject.length && getLogEntries();
  }, [nextAuditSubject]);

  useEffect(() => {
    navigateTo && setNavigateTo(false);
  }, [navigateTo]);

  const getTitle = () => {
    const timesMillis = actions.map(action => new Date(action.actionTime).getTime());
    const minTime = new Date(Math.min(...timesMillis)).toDateString();
    const maxTime = new Date(Math.max(...timesMillis)).toDateString();
    return `${auditSubjectType} ${nextAuditSubject} ID ${dbId} Audits from ${minTime} to ${maxTime}`;
  };

  return !loading
      ? <React.Fragment>
        {navigateTo && <Navigate to={getNextAuditPath(nextAuditSubject)}/>}
        <TextInputWithLabel
            containerStyle={{display: 'inline-block'}}
            style={{
              width: '15rem',
              margin: '1rem'
            }}
            labelText = {auditSubjectType}
            value = {nextAuditSubject}
            onChange = {setNextAuditSubject}
        />
        <Button style={{height: '1.5rem', margin: '0.25rem 0.5rem'}}
                onClick={() => setNavigateTo(true)}>
          Audit
        </Button>
        <div>{getTitle()}</div>
        <AuditActionCardListView actions={fp.slice(0, 100, actions)}/>
      </React.Fragment>
      : <div>Loading Audit for {auditSubjectType} {nextAuditSubject}...</div>;
};
