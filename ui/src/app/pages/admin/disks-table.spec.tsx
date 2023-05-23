import '@testing-library/jest-dom';

import { screen } from '@testing-library/dom';
import { render, waitFor } from '@testing-library/react';

import { DisksTable } from './disks-table';

jest.mock('app/services/swagger-fetch-clients');

test('loads and displays greeting', async () => {
  render(<DisksTable sourceWorkspaceNamespace='123' />);
  await waitFor(() => {
    expect(screen.queryByTestId('disks spinner')).not.toBeInTheDocument();
  });
  // expect(DataTable).toBeInTheDocument();
  // screen.getByText(/disks/i);
});
