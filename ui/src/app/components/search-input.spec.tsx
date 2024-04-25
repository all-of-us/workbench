import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { SearchInput } from './search-input';

describe(SearchInput.name, () => {
  const getSearchInput = () => screen.getByRole('textbox', { name: 'Search' });

  test('component should render', () => {
    render(<SearchInput />);
    expect(getSearchInput()).toBeInTheDocument();
  });

  test('no dropdown is displayed on user input by default', async () => {
    render(<SearchInput />);
    fireEvent.change(getSearchInput(), { target: { value: 'foo' } });
    await waitFor(() => {
      expect(
        screen.queryByTestId('search-input-drop-down')
      ).not.toBeInTheDocument();
    });
  });

  test('dropdown is displayed when results are available', async () => {
    function onSearch() {
      return new Promise<Array<string>>((accept) => {
        accept(['bar']);
      });
    }
    const { getByTestId } = render(<SearchInput onSearch={onSearch} />);
    fireEvent.change(getSearchInput(), { target: { value: 'foo' } });
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
    const { getByTestId, queryByTestId } = render(
      <SearchInput onSearch={onSearch} />
    );
    fireEvent.change(getSearchInput(), { target: { value: 'foo' } });
    await waitFor(() => getByTestId('search-input-drop-down-element-0'));
    fireEvent.mouseDown(getByTestId('search-input-drop-down-element-0'));
    fireEvent.blur(getSearchInput());
    expect(queryByTestId('search-input-drop-down')).not.toBeInTheDocument();
  });

  test('onChange handler is called when the contents changes', async () => {
    let externalValue = false;
    render(
      <SearchInput
        onChange={() => {
          externalValue = true;
        }}
      />
    );
    fireEvent.change(getSearchInput(), { target: { value: 'foo' } });
    await waitFor(() => {
      expect(externalValue).toEqual(true);
    });
  });
});
