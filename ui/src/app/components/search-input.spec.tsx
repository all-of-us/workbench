import * as React from 'react';

import { fireEvent, render, waitFor } from '@testing-library/react';

import { SearchInput } from './search-input';

test('component should render', () => {
  const { getByTestId } = render(<SearchInput />);
  expect(getByTestId('search-input')).toBeInTheDocument();
});

test('no dropdown is displayed on user input by default', async () => {
  const { getByTestId } = render(<SearchInput />);
  fireEvent.change(getByTestId('search-input'), { target: { value: 'foo' } });
  await waitFor(() => {
    expect(getByTestId('search-input-drop-down')).not.toBeInTheDocument();
  });
});

test('dropdown is displayed when results are available', async () => {
  function onSearch() {
    return new Promise<Array<string>>((accept) => {
      accept(['bar']);
    });
  }
  const { getByTestId } = render(<SearchInput onSearch={onSearch} />);
  fireEvent.change(getByTestId('search-input'), { target: { value: 'foo' } });
  await waitFor(() => {
    expect(getByTestId('search-input-drop-down')).toBeInTheDocument();
  });
});

test('selecting a result from the dropdown closes the dropdown', async () => {
  function onSearch() {
    return new Promise<Array<string>>((accept) => {
      accept(['bar']);
    });
  }
  const { getByTestId } = render(<SearchInput onSearch={onSearch} />);
  fireEvent.change(getByTestId('search-input'), { target: { value: 'foo' } });
  await waitFor(() => {
    fireEvent.mouseDown(getByTestId('search-input-drop-down-element-0'));
    fireEvent.blur(getByTestId('search-input'));
    expect(getByTestId('search-input-drop-down')).not.toBeInTheDocument();
  });
});

test('onChange handler is called when the contents changes', async () => {
  let changed = false;
  const { getByTestId } = render(
    <SearchInput
      onChange={() => {
        changed = true;
      }}
    />
  );
  fireEvent.change(getByTestId('search-input'), { target: { value: 'foo' } });
  await waitFor(() => {
    expect(changed).toBeTruthy();
  });
});
