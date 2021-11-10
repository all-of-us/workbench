import {DataTable} from 'primereact/datatable';
import {Column} from 'primereact/column';
import {reactStyles} from 'app/utils';
import {StyledRouterLink} from 'app/components/buttons';

import {useState, useEffect} from 'react';
import { egressEventsAdminApi, workspaceAdminApi } from 'app/services/swagger-fetch-clients';


const styles = reactStyles({
  colStyle: {
    fontSize: 12,
    height: '60px',
    lineHeight: '0.5rem',
    overflow: 'hidden',
    padding: '.5em',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  }
});

interface Props {
  sourceUserEmail?: string;
  sourceWorkspaceNamespace?: string;
}

export const EgressEventsTable = (props: Props) => {
  const {sourceUserEmail, sourceWorkspaceNamespace} = props;

  const [events, setEvents] = useState(null);

  useEffect(() => {
    egressEventsAdminApi().listEgressEvents({
      sourceUserEmail,
      sourceWorkspaceNamespace
    }).then(({events}) =>  setEvents(events));
  }, []);

  // TODO: pagination, sorting options, filtering

  return <DataTable value={events} paginator={true}>
    <Column field='egressEventId'
            bodyStyle={{...styles.colStyle}}
            filterField={'egressEventId'}
            filterMatchMode={'contains'}
            frozen={true}
            header='Event ID'
            headerStyle={{...styles.colStyle, width: '100px'}}
            sortable={true}
            sortField={'egressEventId'}
    />
    <Column field='creationTime'
            bodyStyle={{...styles.colStyle}}
            excludeGlobalFilter={true}
            header='Time'
            headerStyle={{...styles.colStyle, width: '150px'}}
    />
    <Column field='sourceUserEmail'
            body={({sourceUserEmail}) => {
              const [username,] = sourceUserEmail.split('@');
              return <StyledRouterLink path={`/admin/users/${username}`}>
                {sourceUserEmail}
              </StyledRouterLink>;
            }}
            bodyStyle={{...styles.colStyle}}
            header='Source User'
            headerStyle={{...styles.colStyle, width: '300px'}}
            sortable={true}
    />
    <Column field='sourceWorkspaceNamespace'
            body={({sourceWorkspaceNamespace}) => {
              return <StyledRouterLink path={`/admin/workspaces/${sourceWorkspaceNamespace}`}>
                {sourceWorkspaceNamespace}
              </StyledRouterLink>;
            }}
            bodyStyle={{...styles.colStyle}}
            header='Source Workspace Namespace'
            headerStyle={{...styles.colStyle, width: '250px'}}
            sortable={true}
    />
    <Column field='status'
            bodyStyle={{...styles.colStyle}}
            header='Status'
            headerStyle={{...styles.colStyle, width: '200px'}}
            sortable={true}
    />
  </DataTable>;
};
