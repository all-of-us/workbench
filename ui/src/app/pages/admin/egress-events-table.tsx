import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { Dropdown } from 'primereact/dropdown';

import { EgressEvent, VwbWorkspaceSearchParamType } from 'generated/fetch';

import { AdminUserLink } from 'app/components/admin/admin-user-link';
import { StyledRouterLink } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import {
  egressEventsAdminApi,
  vwbWorkspaceAdminApi,
} from 'app/services/swagger-fetch-clients';
import { mutableEgressEventStatuses } from 'app/utils/egress-events';

interface Props {
  sourceUserEmail?: string;
  sourceWorkspaceNamespace?: string;
  displayPageSize?: number;
}

// Helper function to check if a string is a UUID
const isUUID = (str: string): boolean => {
  const uuidRegex =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  return uuidRegex.test(str);
};

export const EgressEventsTable = ({
  sourceUserEmail,
  sourceWorkspaceNamespace,
  displayPageSize = 20,
}: Props) => {
  const [loading, setLoading] = useState(false);
  const [first, setFirst] = useState(0);
  const [events, setEvents] = useState<EgressEvent[]>(null);
  const [nextPageToken, setNextPageToken] = useState<string>(null);
  const [totalRecords, setTotalRecords] = useState<number>(null);
  const [pendingUpdateEvent, setPendingUpdateEvent] =
    useState<EgressEvent>(null);
  // Map of workspace UUID to user-facing ID for VWB workspaces
  const [vwbWorkspaceUserFacingIds, setVwbWorkspaceUserFacingIds] = useState<
    Map<string, string>
  >(new Map());
  // Track which UUIDs we're currently loading
  const [loadingVwbIds, setLoadingVwbIds] = useState<Set<string>>(new Set());
  // Track which UUIDs failed to load (e.g., deleted workspaces)
  const [failedVwbIds, setFailedVwbIds] = useState<Set<string>>(new Set());

  // This callback fetches more events and appends to the events array, if needed.
  // This works by continuing to walk the paginated stream until the given event count
  // is satisfied. Note that the backend pages are independent of the displayed pages
  // within the DataTable.
  const maybeFetchMoreEvents = useCallback(
    async (atLeastCount: number): Promise<void> => {
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
          pageToken,
        });
        setTotalRecords(resp.totalSize);

        allLoadedEvents.push(...resp.events);
        pageToken = resp.nextPageToken;
      } while (pageToken && allLoadedEvents.length < atLeastCount);
      setEvents(allLoadedEvents);
      setNextPageToken(pageToken);

      // Fetch VWB workspace user-facing IDs for any UUID namespaces
      const newVwbIds = new Map(vwbWorkspaceUserFacingIds);
      const newLoadingIds = new Set(loadingVwbIds);
      const newFailedIds = new Set(failedVwbIds);
      const uuidNamespaces = allLoadedEvents
        .map((e) => e.sourceWorkspaceNamespace)
        .filter(
          (ns) =>
            ns &&
            isUUID(ns) &&
            !newVwbIds.has(ns) &&
            !newLoadingIds.has(ns) &&
            !newFailedIds.has(ns)
        );

      const uniqueUuidNamespaces = Array.from(new Set(uuidNamespaces));

      // Mark these UUIDs as loading
      uniqueUuidNamespaces.forEach((id) => newLoadingIds.add(id));
      setLoadingVwbIds(newLoadingIds);

      setLoading(false);

      // Fetch in the background (don't block the UI)
      Promise.all(
        uniqueUuidNamespaces.map(async (workspaceId) => {
          try {
            const resp =
              await vwbWorkspaceAdminApi().getVwbWorkspacesBySearchParam(
                VwbWorkspaceSearchParamType.ID,
                workspaceId
              );
            if (resp.items?.length > 0) {
              setVwbWorkspaceUserFacingIds((prev) => {
                const updated = new Map(prev);
                updated.set(workspaceId, resp.items[0].userFacingId);
                return updated;
              });
            } else {
              // No workspace found (e.g., deleted workspace)
              setFailedVwbIds((prev) => {
                const updated = new Set(prev);
                updated.add(workspaceId);
                return updated;
              });
            }
          } catch (error) {
            console.error(
              `Failed to fetch VWB workspace for ${workspaceId}:`,
              error
            );
            // Mark as failed so we don't keep retrying
            setFailedVwbIds((prev) => {
              const updated = new Set(prev);
              updated.add(workspaceId);
              return updated;
            });
          } finally {
            setLoadingVwbIds((prev) => {
              const updated = new Set(prev);
              updated.delete(workspaceId);
              return updated;
            });
          }
        })
      );
    },
    [
      loading,
      events,
      nextPageToken,
      vwbWorkspaceUserFacingIds,
      loadingVwbIds,
      failedVwbIds,
    ]
  );

  const onRowEditSave = useCallback(async () => {
    if (!pendingUpdateEvent) {
      return;
    }

    const i = events.findIndex(
      ({ egressEventId }) => egressEventId === pendingUpdateEvent.egressEventId
    );
    if (fp.isEqual(events[i], pendingUpdateEvent)) {
      return;
    }

    setLoading(true);
    const updatedEvent = await egressEventsAdminApi().updateEgressEvent(
      pendingUpdateEvent.egressEventId,
      { egressEvent: pendingUpdateEvent }
    );
    const updatedEvents = events.slice();
    updatedEvents.splice(i, 1, updatedEvent);
    setEvents(updatedEvents);
    setPendingUpdateEvent(null);
    setLoading(false);
  }, [events, pendingUpdateEvent]);

  const statusEditor = ({ rowData }) => {
    return (
      <Dropdown
        value={pendingUpdateEvent?.status ?? rowData.status}
        options={mutableEgressEventStatuses}
        onChange={(e) => {
          setPendingUpdateEvent({
            ...rowData,
            status: e.value,
          });
        }}
        placeholder='Select a Status'
      />
    );
  };

  useEffect(() => {
    maybeFetchMoreEvents(2 * displayPageSize);
  }, []);

  const displayEvents = events?.slice(first, first + displayPageSize);
  return (
    <DataTable
      lazy
      paginator
      loading={loading}
      editMode='row'
      onRowEditSave={onRowEditSave}
      onRowEditCancel={() => setPendingUpdateEvent(null)}
      value={displayEvents}
      onPage={async (e) => {
        await maybeFetchMoreEvents(e.first + e.rows - 1);
        setFirst(e.first);
      }}
      breakpoint='0px'
      first={first}
      rows={displayPageSize}
      totalRecords={totalRecords}
    >
      <Column
        field='egressEventId'
        body={({ egressEventId }) => (
          <StyledRouterLink
            data-test-id='egress-event-id'
            path={`/admin/egress-events/${egressEventId}`}
          >
            {egressEventId}
          </StyledRouterLink>
        )}
        header='Event ID'
        headerStyle={{ width: '100px' }}
      />
      <Column
        field='creationTime'
        header='Time'
        headerStyle={{ width: '150px' }}
        body={({ creationTime }) => new Date(creationTime).toLocaleString()}
      />
      <Column
        field='sourceUserEmail'
        body={({ sourceUserEmail: email }) => {
          const [username] = email.split('@');
          return <AdminUserLink {...{ username }}>{email}</AdminUserLink>;
        }}
        header='Source User'
        headerStyle={{ width: '300px' }}
      />
      <Column
        field='sourceWorkspaceNamespace'
        body={({ sourceWorkspaceNamespace: namespace }) => {
          // Check if this is a VWB workspace (UUID format)
          if (isUUID(namespace)) {
            const userFacingId = vwbWorkspaceUserFacingIds.get(namespace);

            // Always show a clickable link - if we have the user-facing ID, use it
            // Otherwise, use the UUID itself and let the VWB workspace page handle the error
            const linkPath = userFacingId
              ? `/admin/vwb/workspaces/${userFacingId}`
              : `/admin/vwb/workspaces/${namespace}`;

            return (
              <StyledRouterLink path={linkPath}>{namespace}</StyledRouterLink>
            );
          }
          // Regular RWB workspace
          return (
            <StyledRouterLink path={`/admin/workspaces/${namespace}`}>
              {namespace}
            </StyledRouterLink>
          );
        }}
        header='Source Workspace Namespace'
        headerStyle={{ width: '250px' }}
      />
      <Column
        field='status'
        editor={(options) => statusEditor(options)}
        header='Status'
        headerStyle={{ width: '200px' }}
      />
      <Column
        rowEditor
        headerStyle={{ width: '10%', minWidth: '12rem' }}
        bodyStyle={{ textAlign: 'center' }}
      ></Column>
    </DataTable>
  );
};
