import {AuditActionCardListView} from 'app/components/admin/audit-card-list-view';
import {Navigate} from 'app/components/app-router';
import {Button, StyledAnchorTag} from 'app/components/buttons';
import {NumberInput, TextInputWithLabel} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import { useDebounce, useToggle } from 'app/utils';
import {downloadTextFile} from 'app/utils/audit-utils';
import {AuditAction, AuditLogEntry} from 'generated';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';

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
   * ID in the MySQL database and BigQuery Audit Database for the this query. Currently either
   * a userId or workspaceId as appropriate. This may be needed for situations (such as workspace audit)
   * where it's not obvious in the responses which workspace is the one you audited.
   */
  sourceId: number;
}

const EMPTY_AUDIT_RESULT: GenericAuditQueryResult = {actions: [], logEntries: [], sourceId: 0, query: ''};

// Common properties for User & Workspace (and similar future pages).
// Assumptions: the path parameter is called initially with the audit API subject.
//   (this will need revisiting for multi-subject queries)
export interface AuditPageProps {
  initialAuditSubject?: string;
  auditSubjectType: string;
  queryAuditLog: (subject: string) => Promise<GenericAuditQueryResult>;
  getNextAuditPath: (subject: string) => string;
  buttonLabel?: string;
  getAdminPageUrl: (subject: string) => string[];
}

const UserInput = ({initialAuditSubject, auditSubjectType, getNextAuditPath, buttonLabel, queryText, getAdminPageUrl}) => {
  const [auditSubject, setAuditSubject] = useState(initialAuditSubject);
  const [downloadSqlFile, setDownloadSqlFile] = useState(false);
  const [subjectRequested, setSubjectRequested] = useToggle();

  useEffect(() => {
    if (downloadSqlFile && !fp.isEmpty(queryText)) {
      const sqlFileName = `audit_query_${auditSubject}_${moment().toISOString()}.sql`;
      downloadTextFile(sqlFileName, queryText);
    }
  }, [queryText, downloadSqlFile]);

  const buttonStyle = {height: '1.5rem', margin: '0.25rem 0.5rem'};

  const onAuditClick = () => {
    setAuditSubject(auditSubject.toLowerCase().trim());
    setSubjectRequested(true);
  };

  const getBigQueryConsoleUrl = () => {
    // TODO(jaycarlton): use actual config endpoint to create correct link RW-5283. This only
    // works for the test env right now.
    const props = {
      bigQueryConsoleUrl: 'https://console.cloud.google.com/bigquery',
      gcpProject: 'all-of-us-workbench-test',
      auditDataset: 'workbench_action_audit_test',
      auditTable: 'workbench_action_audit_test'
    };
    return `${props.bigQueryConsoleUrl}`
        + `?project=${props.gcpProject}`
        + `&p=${props.gcpProject}`
        + `&d=${props.auditDataset}`
        + `&t=${props.auditTable}`
        + '&page=table';
  };

  return <React.Fragment>
    {subjectRequested && <Navigate to={getNextAuditPath(auditSubject)}/>}
    <TextInputWithLabel
      containerStyle={{display: 'inline-block'}}
      style={{width: '15rem', margin: '1rem'}}
      labelText = {buttonLabel || auditSubjectType}
      value = {auditSubject}
      onChange = {setAuditSubject}
    />
    <TooltipTrigger content={`Retrieve the audit trail for selected ${auditSubjectType}.`}>
      <Button style={buttonStyle}
              disabled={fp.isEmpty(auditSubject)}
              onClick={onAuditClick}>
      Audit
      </Button>
    </TooltipTrigger>
    <div style={{
      margin: '0',
      display: 'flex',
      flexDirection: 'row',
      textAlign: 'center',
      fontWeight: 600
    }}>
    <TooltipTrigger content={'BigQuery Console page (use pmi-ops.org account)'}>
      <StyledAnchorTag href={getBigQueryConsoleUrl()}
                       target='_blank'>
        BigQuery Console
      </StyledAnchorTag>
    </TooltipTrigger>
    &nbsp;|&nbsp;
    <TooltipTrigger content={`Admin Page for ${auditSubjectType} ${auditSubject || 'n/a'}`}>
      <StyledAnchorTag href={auditSubject ? getAdminPageUrl(auditSubject) : undefined}
                       style={auditSubject ? {} : {cursor: 'not-allowed', color: colors.disabled}}>
        {auditSubjectType} Admin
      </StyledAnchorTag>
    </TooltipTrigger>
    </div>
    <TooltipTrigger content={'Download actual SQL query for BigQuery Action Audit table. Useful' +
      ' for developers or analysts interested in basing other ad hoc queries off' +
      ' this audit query in the BigQuery console or bq tool.'}>
      <Button style={buttonStyle}
              disabled={fp.isEmpty(queryText)}
              onClick={() => setDownloadSqlFile(true)}>
        Download SQL
      </Button>
    </TooltipTrigger>
  </React.Fragment>;
};

const NumActions = ({onChange, totalActions}) => {
  const [displayNum, setDisplayNum] = useState(20);

  useDebounce(() => onChange(displayNum), [displayNum]);

  return <div>
    <label style={{
      fontSize: 14,
      display: 'block',
      color: colors.primary,
      lineHeight: '22px',
      fontWeight: 600,
      marginRight: '0.25rem'}}>{`Number of Actions to Display (${totalActions} available)`}</label>
    <NumberInput value={Math.min(displayNum, totalActions)} min={1} max={totalActions} style={{width: '4rem'}} onChange={setDisplayNum}/>
  </div>;
};

export const AuditPageComponent = (props: AuditPageProps) => {
  const {initialAuditSubject, queryAuditLog, getNextAuditPath, auditSubjectType, buttonLabel, getAdminPageUrl} = props;

  const [loading, setLoading] = useState(true);
  const [queryResult, setQueryResult] = useState<GenericAuditQueryResult>(EMPTY_AUDIT_RESULT);
  const [displayNum, setDisplayNum] = useState(20);
  const {actions, sourceId, query} = queryResult;

  useEffect(() => {
    const getLogEntries = async() => {
      setLoading(true);
      setQueryResult(EMPTY_AUDIT_RESULT);
      try {
        await queryAuditLog(initialAuditSubject)
          .then(setQueryResult);
      } catch (e) {
        console.warn(`Error retrieving audit query results for ${initialAuditSubject}`);
      }
      setLoading(false);
    };

    getLogEntries();
  }, [initialAuditSubject]);

  const getTitle = () => {
    const timesMillis = fp.map(fp.get('actionTime'))(actions);
    const minTime = new Date(Math.min(...timesMillis)).toDateString();
    const maxTime = new Date(Math.max(...timesMillis)).toDateString();
    return initialAuditSubject
      ? `${auditSubjectType} ${initialAuditSubject} ID ${sourceId} Audits from ${minTime} to ${maxTime}`
      : `No ${auditSubjectType} selected`;
  };

  return !loading
    ? <React.Fragment>
        <div style={{marginLeft: '1rem'}}>
          <UserInput initialAuditSubject={initialAuditSubject}
                     auditSubjectType={auditSubjectType}
                     getNextAuditPath={getNextAuditPath}
                     buttonLabel={buttonLabel}
                     queryText={query}
                     getAdminPageUrl={getAdminPageUrl}/>
          <NumActions onChange={setDisplayNum} totalActions={actions.length}/>
          <div>{getTitle()}</div>
        </div>
        <AuditActionCardListView actions={fp.slice(0, displayNum, actions)}/>
      </React.Fragment>
    : <div>Loading Audit for {auditSubjectType} {initialAuditSubject}...</div>;
};

