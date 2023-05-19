import '@testing-library/jest-dom';

import { DataTable } from 'primereact/datatable';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';
import { Spinner } from 'app/components/spinners';

test('loads and displays greeting', async () => {
  render(<DisksTable />);
  await waitFor(() => {
    expect(Spinner).not.toBeInTheDocument();
  });
  expect(DataTable).toBeInTheDocument();
  screen.getByText(/disks/i);
});
