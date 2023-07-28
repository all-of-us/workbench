import * as React from 'react';

import { AlertDanger } from 'app/components/alert';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  withCurrentWorkspace,
} from 'app/utils';
import {Clickable} from 'app/components/buttons';

const { useEffect, useState } = React;

const styles = reactStyles({
  searchContainer: {
    float: 'left',
    width: '80%',
    padding: '0.6rem 0',
    zIndex: 10,
  },
  searchBar: {
    height: '3.15rem',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
  },
  searchInput: {
    width: '94%',
    height: '2.25rem',
    marginLeft: '0.375rem',
    padding: '0',
    background: 'transparent',
    border: 0,
    outline: 'none',
  },
});

export const VariantSearch = withCurrentWorkspace()(({workspace}) => {
  const [loading, setLoading] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [searchTerms, setSearchTerms] = useState('');

  const searchVariants = async () => {
    const { id, namespace } = workspace;
    await cohortBuilderApi()
    .findVariants(namespace, id, searchTerms)
    .then((response) =>
      setSearchResults(response.items)
    );
  };

  useEffect(() => {
    searchVariants();
  }, []);

  return (
    <>
      <div style={{ display: 'flex' }}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            <ClrIcon shape='search' size='18' />
            <TextInput
              data-test-id='list-search-input'
              style={styles.searchInput}
              value={searchTerms}
              placeholder={this.textInputPlaceholder}
              onChange={(e) => this.setState({ searchTerms: e })}
              onKeyPress={this.handleInput}
            />
            {searching && (
              <Clickable
                style={styles.clearSearchIcon}
                onClick={() => this.clearSearch()}
              >
                <ClrIcon size={24} shape='times-circle' />
              </Clickable>
            )}
          </div>
          {inputErrors.map((error, e) => (
            <AlertDanger key={e} style={styles.inputAlert}>
              <span data-test-id='input-error-alert'>{error}</span>
            </AlertDanger>
          ))}
        </div>
        <div style={{ float: 'right', width: '20%' }}>
          <TooltipTrigger side='top' content={searchTooltip}>
            <ClrIcon
              style={styles.infoIcon}
              className='is-solid'
              shape='info-standard'
            />
          </TooltipTrigger>
        </div>
      </div>
    </>
  );
});