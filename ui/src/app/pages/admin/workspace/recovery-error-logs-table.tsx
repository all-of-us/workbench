import * as React from 'react';
import { useEffect, useRef, useState } from 'react';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  WorkspaceRecoveryErrorLog,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { Error as ErrorDiv } from 'app/components/inputs';
import { workspaceAdminApi } from 'app/services/swagger-fetch-clients';

interface Props {
  namespace: string;
  recoveryState?: WorkspaceRecoveryStatus;
}

const PAGE_SIZE = 10;

const formatTimestamp = (createdTime: string | number | undefined) => {
  if (createdTime === undefined || createdTime === null) {
    return 'N/A';
  }

  const parsedFromString = Date.parse(String(createdTime));
  const millis =
    typeof createdTime === 'number'
      ? createdTime
      : Number.isNaN(parsedFromString)
      ? Number(createdTime)
      : parsedFromString;

  if (Number.isNaN(millis)) {
    return String(createdTime);
  }

  return new Date(millis).toLocaleString();
};

export const RecoveryErrorLogsTable = ({ namespace, recoveryState }: Props) => {
  const [logs, setLogs] = useState<WorkspaceRecoveryErrorLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const activeRequestId = useRef(0);

  useEffect(() => {
    activeRequestId.current += 1;
    const requestId = activeRequestId.current;

    if (!namespace || recoveryState !== WorkspaceRecoveryStatus.FAILED) {
      setLogs([]);
      setError(undefined);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(undefined);

    workspaceAdminApi()
      .getWorkspaceRecoveryErrorLogs(namespace)
      .then((response) => {
        if (!cancelled && requestId === activeRequestId.current) {
          setLogs(response || []);
        }
      })
      .catch(() => {
        if (!cancelled && requestId === activeRequestId.current) {
          setError(
            'Failed to load recovery error logs. Please refresh and try again.'
          );
          setLogs([]);
        }
      })
      .finally(() => {
        if (!cancelled && requestId === activeRequestId.current) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [namespace, recoveryState]);

  return (
    <>
      <h3>Recovery Error Logs</h3>
      {error && <ErrorDiv>{error}</ErrorDiv>}
      <DataTable
        value={logs}
        loading={loading}
        paginator={logs.length > PAGE_SIZE}
        rows={PAGE_SIZE}
        emptyMessage='No recovery errors found for this workspace.'
        responsiveLayout='scroll'
      >
        <Column field='id' header='ID' />
        <Column field='workspaceId' header='Workspace ID' />
        <Column field='errorType' header='Error Type' />
        <Column
          field='errorMessage'
          header='Error Message'
          body={({ errorMessage }) => (
            <span style={{ whiteSpace: 'normal', wordBreak: 'break-word' }}>
              {errorMessage}
            </span>
          )}
        />
        <Column
          field='createdTime'
          header='Time'
          body={({ createdTime }) => formatTimestamp(createdTime)}
        />
      </DataTable>
    </>
  );
};
