import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { MultiSelect } from 'primereact/multiselect';

import { AppType, Disk } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { Button } from 'app/components/buttons';
import { Spinner } from 'app/components/spinners';
import { disksAdminApi } from 'app/services/swagger-fetch-clients';
import { fetchWithErrorModal } from 'app/utils/errors';
import moment from 'moment';

interface Props {
  sourceWorkspaceNamespace?: string;
}

const validEnvironmentTypes = [
  ...Object.values(AppType),
  UIAppType.JUPYTER,
].map(fp.capitalize);

export const DisksTable = ({ sourceWorkspaceNamespace }: Props) => {
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [disks, setDisks] = useState([]);
  const [visibleEnvironmentTypes, setVisibleEnvironmentTypes] = useState([]);
  const [filteredDisks, setFilteredDisks] = useState([]);

  const filterByEnvironmentType = fp.curry((currentFilters, disk) => {
    return (
      currentFilters.length === 0 ||
      currentFilters.includes(fp.capitalize(disk.appType)) ||
      (currentFilters.includes(UIAppType.JUPYTER) && disk.isGceRuntime)
    );
  });

  const applyFilters = () => {
    const x = disks.filter(filterByEnvironmentType(visibleEnvironmentTypes));
    setFilteredDisks(x);
  };

  const refreshDisks = () => {
    setLoading(true);
    disksAdminApi()
      .listDisksInWorkspace(sourceWorkspaceNamespace)
      .then(setDisks)
      .finally(() => setLoading(false));
  };

  // Update filters whenever the underlying disks or any filters change
  useEffect(() => applyFilters(), [disks, visibleEnvironmentTypes]);

  useEffect(() => refreshDisks(), []);

  const onClickDelete = (disk) =>
    fetchWithErrorModal(() => {
      setDeleting(true);
      return disksAdminApi().deleteDisk(sourceWorkspaceNamespace, disk.name);
    }).finally(() => {
      setDeleting(false);
      refreshDisks();
    });

  const getEnvironmentType = (disk: Disk) =>
    fp.capitalize(
      (disk.isGceRuntime ? UIAppType.JUPYTER : disk.appType).toString()
    );

  const environmentTypeFilterTemplate = () => {
    return (
      <MultiSelect
        value={visibleEnvironmentTypes}
        options={validEnvironmentTypes}
        onChange={(e) => {
          setVisibleEnvironmentTypes(e.value ?? []);
        }}
        showClear={true}
        showSelectAll={false}
        panelHeaderTemplate={<></>}
      />
    );
  };

  const compareByEnvironmentType = fp.curry(
    (sortOrder, firstDisk, secondDisk) => {
      console.log(sortOrder);
      return (
        sortOrder *
        getEnvironmentType(firstDisk).localeCompare(
          getEnvironmentType(secondDisk)
        )
      );
    }
  );

  const onSortEnvironmentType = (sortEvent) => {
    // @ts-ignore
    return disks.sort(compareByEnvironmentType(sortEvent.order));
  };

  return loading ? (
    <Spinner title='disks loading spinner' />
  ) : (
    <DataTable value={filteredDisks} emptyMessage='No disks found'>
      <Column
        body={(disk) => (
          <Button disabled={deleting} onClick={() => onClickDelete(disk)}>
            Delete
          </Button>
        )}
      />
      <Column field='name' header='Name' />
      <Column field='creator' header='Creator' />
      <Column
        field='createdDate'
        header='Date Created'
        body={(disk) => moment(disk.createdDate).format('YYYY-MM-DD HH:mm')}
      />
      <Column field='status' header='Status' />
      <Column
        field='appType'
        header='Environment Type'
        body={getEnvironmentType}
        sortable={true}
        sortFunction={onSortEnvironmentType}
        filter
        showApplyButton={false}
        showClearButton={false}
        showFilterMatchModes={false}
        showFilterOperator={false}
        showFilterMenuOptions={false}
        filterElement={environmentTypeFilterTemplate}
        filterMatchMode='custom'
      />
      <Column field='size' header='Size (GB)' />
    </DataTable>
  );
};
