import {
  AuditEgressEventResponse,
  EgressEvent,
  EgressEventsAdminApi,
  EgressEventStatus,
  ListEgressEventsRequest,
  ListEgressEventsResponse,
  UpdateEgressEventRequest,
} from 'generated/fetch';

import { stubNotImplementedError } from 'testing/stubs/stub-utils';

export class EgressEventsAdminApiStub extends EgressEventsAdminApi {
  private nextEventId = 1;

  constructor(public events: EgressEvent[] = []) {
    super(undefined);
  }

  public listEgressEvents(
    request?: ListEgressEventsRequest
  ): Promise<ListEgressEventsResponse> {
    return new Promise((accept) => {
      const events = this.events
        .filter(
          ({ sourceUserEmail }) =>
            !request.sourceUserEmail ||
            sourceUserEmail === request.sourceUserEmail
        )
        .filter(
          ({ sourceWorkspaceNamespace }) =>
            !request.sourceWorkspaceNamespace ||
            sourceWorkspaceNamespace === request.sourceWorkspaceNamespace
        );

      let offset = 0;
      if (+request.pageToken) {
        offset = +request.pageToken;
      }
      const end = offset + (request.pageSize || 20);
      accept({
        events: events.slice(offset, end),
        totalSize: events.length,
        nextPageToken: end > events.length ? null : String(end),
      });
    });
  }

  public updateEgressEvent(
    id: string,
    request?: UpdateEgressEventRequest
  ): Promise<EgressEvent> {
    return new Promise((accept) => {
      const event = this.events.find(
        ({ egressEventId }) => egressEventId === id
      );
      if (!event) {
        throw new Error(`egress event ${id} not found`);
      }

      event.status = request.egressEvent.status;
      accept(event);
    });
  }

  public auditEgressEvent(id: string): Promise<AuditEgressEventResponse> {
    return new Promise((accept) => {
      const event = this.events.find(
        ({ egressEventId }) => egressEventId === id
      );
      if (!event) {
        throw new Error(`egress event ${id} not found`);
      }

      accept({
        egressEvent: event,
        sumologicEvent: {
          vmPrefix: 'all-of-us-1',
          egressWindowStart: new Date('2000-01-01 05:00:00').getTime() / 1000,
          egressMib: 188.99,
        },
        runtimeLogGroups: [
          {
            name: 'Notebook interactions',
            pattern: '%.ipynb%',
            totalEntries: 100,
            entries: [
              {
                timestamp: new Date('2000-01-01 05:00:00').toISOString(),
                message: 'user opened asdf.ipynb',
              },
              {
                timestamp: new Date('2000-01-01 03:00:00').toISOString(),
                message: 'user opened foo.txt',
              },
            ],
          },
          {
            name: 'Downloads',
            pattern: '%download%',
            totalEntries: 100,
            entries: [
              {
                timestamp: new Date('2000-01-01 06:00:00').toISOString(),
                message: 'user downloaded asdf.ipynb',
              },
              {
                timestamp: new Date('2000-01-01 04:00:00').toISOString(),
                message: 'user downloaded foo.txt',
              },
            ],
          },
        ],
      });
    });
  }

  public simulateNewEvent(event?: EgressEvent): EgressEvent {
    const e = event || this.newEvent();
    e.egressEventId = String(this.nextEventId++);
    this.events.push(e);
    return e;
  }

  private newEvent(): EgressEvent {
    return {
      sourceUserEmail: 'asdf@fake-research-aou.org',
      sourceWorkspaceNamespace: 'ns',
      sourceGoogleProject: 'proj',
      egressMegabytes: 300.0,
      egressWindowSeconds: 3600,
      status: EgressEventStatus.REMEDIATED,
      creationTime: new Date('2000-01-01 03:00:00').toISOString(),
    };
  }
}
