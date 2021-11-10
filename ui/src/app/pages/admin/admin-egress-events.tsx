import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {useState, useEffect} from 'react';
import { EgressEventsTable } from './egress-events-table';

interface Props extends WithSpinnerOverlayProps {}

export const AdminEgressEvents = (props: Props) => {
  useEffect(() => {
    props.hideSpinner();
  }, []);

  return <>
    <EgressEventsTable />
  </>;
}
