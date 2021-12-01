import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import { egressEventsAdminApi } from 'app/services/swagger-fetch-clients';
import { AuditEgressEventResponse } from 'generated/fetch';
import { useState, useEffect} from 'react';
import { MatchParams } from 'app/utils/stores';
import {useParams} from 'react-router';
import {Column} from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { TabPanel, TabView } from 'primereact/tabview';
import { FlexRow } from 'app/components/flex';
import { StyledRouterLink } from 'app/components/buttons';


const DetailRow = ({label, children}: {label: string, children: React.ReactNode|string}) => {
  return <FlexRow style={{display: 'table-row'}}>
    <label style={{
      display: 'table-cell',
      fontWeight: 600,
      paddingRight: '7px',
      verticalAlign: 'top'
    }}>{label}</label>
    <span style={{display: 'table-cell'}}>
      {children}
    </span>
  </FlexRow>;
};

const HighlightedLogMessage = ({logPattern, msg}: {logPattern: string, msg: string}) => {
  const target = logPattern.replace(/%/g, '');
  const parts = msg.split(target);
  return <>
    {parts.map((part, i) => <span key={i}>
      {part}
      {i < parts.length - 1 ? <span style={{fontWeight: 600}}>{target}</span> : null}
    </span>)}
  </>;
}

export const AdminEgressAudit = (props: WithSpinnerOverlayProps) => {
  const {eventId = ''} = useParams<MatchParams>();
  const [egressDetails, setEgressDetails] = useState<AuditEgressEventResponse>(null);
  const [activeLogGroup, setActiveLogGroup] = useState<number>(0);

  useEffect(() => {
    const aborter = new AbortController();
    (async() => {
      const resp = await egressEventsAdminApi().auditEgressEvent(eventId, {}, {
        signal: aborter.signal
      });
      setEgressDetails(resp);
      props.hideSpinner();
    })();
    return () => aborter.abort();
  }, []);

  if (!egressDetails) {
    return null;
  }

  const event = egressDetails.egressEvent;
  const [username,] = event.sourceUserEmail.split('@');
  return <>
    <h2>Egress event {event.egressEventId}</h2>
    <div style={{display: 'table'}}>
      <DetailRow label="Detection time">
        {(new Date(event.creationTime)).toString()}
      </DetailRow>
      <DetailRow label="Source user">
        <StyledRouterLink path={`/admin/users/${username}`}>
          {event.sourceUserEmail}
        </StyledRouterLink>
      </DetailRow>
      <DetailRow label="Source workspace">
        <StyledRouterLink path={`/admin/workspaces/${event.sourceWorkspaceNamespace}`}>
          {event.sourceWorkspaceNamespace}
        </StyledRouterLink>
      </DetailRow>
      <DetailRow label="Google project">
        {event.sourceGoogleProject}
      </DetailRow>
      <DetailRow label="Status">
        {event.status}
      </DetailRow>
      <DetailRow label="Sumologic event">
        <textarea readOnly style={{width: '400px'}} value={JSON.stringify(egressDetails.sumologicEvent, null, 2)} />
      </DetailRow>
    </div>
    <h2>Log entries</h2>
    <TabView activeIndex={activeLogGroup} onTabChange={(e) => setActiveLogGroup(e.index)}>
      {egressDetails.runtimeLogGroups.map((group) => <TabPanel key={group.name} header={group.name}>
        <DataTable
          value={group.entries} >
          <Column field='timestamp'
                  header='Timestamp'
                  headerStyle={{width: '200px'}} />
          <Column field='message'
                  header='Log Message'
                  body={({message}) => (
                    <HighlightedLogMessage
                      logPattern={group.pattern}
                      msg={message} />
                  )} />
        </DataTable>
      </TabPanel>)}
    </TabView>
  </>;
}
