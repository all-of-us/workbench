import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { useEffect } from 'react';
import { EgressEventsTable } from './egress-events-table';

export const AdminEgressEvents = (props: WithSpinnerOverlayProps) => {
  useEffect(() => {
    props.hideSpinner();
  }, []);

  return (
    <>
      <EgressEventsTable />
    </>
  );
};
