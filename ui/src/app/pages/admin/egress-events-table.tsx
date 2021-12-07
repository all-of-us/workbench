import {DataTable} from 'primereact/datatable';
import {Column} from 'primereact/column';
import {StyledRouterLink} from 'app/components/buttons';
import {Dropdown} from 'primereact/dropdown';
import * as fp from 'lodash/fp';

import { useState, useEffect, useCallback} from 'react';
import { egressEventsAdminApi } from 'app/services/swagger-fetch-clients';
import { EgressEvent } from 'generated/fetch';
import { mutableEgressEventStatuses } from 'app/utils/egress-events';


interface Props {
  sourceUserEmail?: string;
  sourceWorkspaceNamespace?: string;
  displayPageSize?: number;
}

export const EgressEventsTable = ({sourceUserEmail, sourceWorkspaceNamespace, displayPageSize = 20}: Props) => {
  const [loading, setLoading] = useState(false);
  const [first, setFirst] = useState(0);
  const [events, setEvents] = useState<EgressEvent[]>(null);
  const [nextPageToken, setNextPageToken] = useState<string>(null);
  const [totalRecords, setTotalRecords] = useState<number>(null);
  const [pendingUpdateEvent, setPendingUpdateEvent] = useState<EgressEvent>(null);

  // This callback fetches more events and appends to the events array, if needed.
  // This works by continuing to walk the paginated stream until the given event count
  // is satisfied. Note that the backend pages are independent of the displayed pages
  // within the DataTable.
  const maybeFetchMoreEvents = useCallback(async(atLeastCount: number): Promise<void> => {
    if (loading || events?.length >= atLeastCount) {
      return;
    }
    if (events && !nextPageToken) {
      // We've loaded at least one page, and there's no more data to load.
      return;
    }
    setLoading(true);

    let pageToken = nextPageToken;
    const allLoadedEvents = [...(events || [])];
    do {
      const resp = await egressEventsAdminApi().listEgressEvents({
        sourceUserEmail,
        sourceWorkspaceNamespace,
        pageToken
      });
      setTotalRecords(resp.totalSize);

      allLoadedEvents.push(...resp.events);
      pageToken = resp.nextPageToken;
    } while (pageToken && allLoadedEvents.length < atLeastCount);
    setEvents(allLoadedEvents);
    setNextPageToken(pageToken);
    setLoading(false);
  }, [loading, events, nextPageToken]);

  const onRowEditSave = useCallback(async() => {
    if (!pendingUpdateEvent) {
      return;
    }

    const i = events.findIndex(({egressEventId}) => egressEventId === pendingUpdateEvent.egressEventId);
    if (fp.isEqual(events[i], pendingUpdateEvent)) {
      return;
    }

    setLoading(true);
    const updatedEvent = await egressEventsAdminApi().updateEgressEvent(
      pendingUpdateEvent.egressEventId, {egressEvent: pendingUpdateEvent});
    const updatedEvents = events.slice();
    updatedEvents.splice(i, 1, updatedEvent)
    setEvents(updatedEvents);
    setLoading(false);
  }, [events, pendingUpdateEvent]);

  const statusEditor = ({rowData}) => {
    return (
      <Dropdown value={pendingUpdateEvent?.status ?? rowData.status}
                options={mutableEgressEventStatuses}
                onChange={(e)=> {
                  setPendingUpdateEvent({
                    ...rowData,
                    status: e.value
                  });
                }}
                placeholder="Select a Status"
      />
    );
  };

  useEffect(() => {
    maybeFetchMoreEvents(2 * displayPageSize);
  }, []);

  const displayEvents = events?.slice(first, first + displayPageSize);
  return <DataTable
      lazy paginator
      loading={loading}
      editMode="row"
      onRowEditSave={onRowEditSave}
      onRowEditCancel={() => setPendingUpdateEvent(null)}
      value={displayEvents}
      onPage={async(e) => {
        await maybeFetchMoreEvents(e.first + e.rows - 1);
        setFirst(e.first);
      }}
      first={first}
      rows={displayPageSize}
      totalRecords={totalRecords}>
    <Column field='egressEventId'
            body={({egressEventId}) => (
              <StyledRouterLink
                  data-test-id="egress-event-id"
                  path={`/admin/egress-events/${egressEventId}`}>
                {egressEventId}
              </StyledRouterLink>
            )}
            header='Event ID'
            headerStyle={{width: '100px'}}
    />
    <Column field='creationTime'
            header='Time'
            headerStyle={{width: '150px'}}
    />
    <Column field='sourceUserEmail'
            body={({sourceUserEmail}) => {
              const [username,] = sourceUserEmail.split('@');
              return <StyledRouterLink path={`/admin/users/${username}`}>
                {sourceUserEmail}
              </StyledRouterLink>;
            }}
            header='Source User'
            headerStyle={{width: '300px'}}
    />
    <Column field='sourceWorkspaceNamespace'
            body={({sourceWorkspaceNamespace}) => {
              return <StyledRouterLink path={`/admin/workspaces/${sourceWorkspaceNamespace}`}>
                {sourceWorkspaceNamespace}
              </StyledRouterLink>;
            }}
            header='Source Workspace Namespace'
            headerStyle={{width: '250px'}}
    />
    <Column field='status'
            editor={(options) => statusEditor(options)}
            header='Status'
            headerStyle={{width: '200px'}}
    />
    <Column rowEditor
            headerStyle={{ width: '10%', minWidth: '8rem' }}
            bodyStyle={{ textAlign: 'center' }}>
    </Column>
  </DataTable>;
};

