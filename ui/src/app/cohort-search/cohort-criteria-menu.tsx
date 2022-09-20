import * as React from 'react';

const { useEffect, useRef, useState } = React;

import { AlertDanger } from 'app/components/alert';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  validateInputForMySQL,
  withCurrentWorkspace,
} from 'app/utils';
import { serverConfigStore } from 'app/utils/stores';

const styles = reactStyles({
  cardBlock: {
    borderBottom: `1px solid ${colors.light}`,
    padding: '0.5rem 0.5rem 0.5rem 0.75rem',
    position: 'relative',
  },
  count: {
    alignItems: 'center',
    background: colors.accent,
    borderRadius: '10px',
    color: colors.white,
    display: 'inline-flex',
    fontSize: '10px',
    height: '0.625rem',
    lineHeight: 'normal',
    minWidth: '0.675rem',
    padding: '0 4px',
    float: 'right',
    marginTop: '0.55rem',
  },
  error: {
    width: '99%',
    marginTop: '2.75rem',
    padding: '0.25rem',
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    borderRadius: '5px',
  },
  menuButton: {
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    borderRadius: '0.125rem',
    color: colorWithWhiteness(colors.black, 0.45),
    cursor: 'pointer',
    fontSize: '12px',
    fontWeight: 100,
    height: '1.5rem',
    letterSpacing: '1px',
    lineHeight: '1.5rem',
    padding: '0 0.5rem',
    textTransform: 'uppercase',
    verticalAlign: 'middle',
  },
  searchContainer: {
    background: colors.white,
    padding: '6px 12px 12px',
    width: '100%',
    zIndex: 10,
  },
  searchBar: {
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
    borderRadius: '5px',
    height: '1.67rem',
    marginTop: '0.25rem',
    padding: '0.3rem',
  },
  searchInput: {
    background: 'transparent',
    border: 0,
    height: '1rem',
    marginLeft: '0.25rem',
    outline: 'none',
    padding: '0',
    width: '84%',
  },
  dropdownMenu: {
    position: 'absolute',
    top: '100%',
    display: 'flex',
    flexDirection: 'column',
    background: colors.white,
    padding: '.25rem 0',
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    boxShadow: '0 1px 0.125rem hsla(0,0%,45%,.25)',
    marginTop: '-0.5rem',
    minHeight: '1.25rem',
    width: '15rem',
    borderRadius: '.125rem',
    zIndex: 103,
  },
  dropdownHeader: {
    height: '1.75rem',
    lineHeight: '1.75rem',
    backgroundColor: colorWithWhiteness(colors.light, 0),
    borderTop: '1px solid #cccccc',
    margin: 0,
    padding: '0 0.5rem',
    width: '100%',
  },
  dropdownHeaderText: {
    color: colors.primary,
    fontSize: '12px',
    fontWeight: 600,
    verticalAlign: 'middle',
  },
  dropdownItem: {
    height: '1.75rem',
    lineHeight: '1.75rem',
    background: 'transparent',
    borderTop: '1px solid #cccccc',
    cursor: 'pointer',
    position: 'relative',
  },
  dropdownLink: {
    display: 'inline-block',
    color: colors.black,
    fontSize: '12px',
    textDecoration: 'none',
    padding: '0 0.5rem 0 1.25rem',
    width: '90%',
  },
  subMenu: {
    position: 'absolute',
    top: 'calc(100% - 3.75rem)',
    left: '100%',
    display: 'flex',
    flexDirection: 'column',
    background: colors.white,
    padding: '.25rem 0',
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    boxShadow: '0 1px 0.125rem hsla(0,0%,45%,.25)',
    minHeight: '1.25rem',
    width: '15rem',
    borderRadius: '.125rem',
    zIndex: 103,
  },
  subMenuIcon: {
    color: colors.secondary,
    float: 'right',
    marginTop: '0.5rem',
    transform: 'rotate(-90deg)',
  },
  subMenuItem: {
    color: colors.black,
    display: 'block',
    fontSize: '12px',
    height: '1.25rem',
    lineHeight: '1.25rem',
    cursor: 'pointer',
    margin: 0,
    padding: '0 1.25rem',
    textDecoration: 'none',
    width: '100%',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.25rem',
  },
});
const searchTrigger = 2;
const searchTooltip = (
  <div style={{ marginLeft: '0.5rem' }}>
    The following special operators can be used to augment search terms:
    <ul style={{ listStylePosition: 'outside' }}>
      <li>
        (*) is the wildcard operator. This operator can be used with a prefix or
        suffix. For example: ceph* (starts with) or *statin (ends with - NOTE:
        when searching for ends with it will only match with end of concept
        name)
      </li>
      <li>
        (-) indicates that this word must <b>not</b> be present. For example:
        lung -cancer
      </li>
      <li>
        (") a phrase that is enclosed within double quote (") characters matches
        only rows that contain the phrase literally, as it was typed. For
        example: "lung cancer"
      </li>
      <li>
        These operators can be combined to produce more complex search
        operations. For example: brain tum* -neoplasm
      </li>
    </ul>
  </div>
);

export const CohortCriteriaMenu = withCurrentWorkspace()(
  ({ launchSearch, menuOptions, workspace, temporalGroup, isTemporal }) => {
    const [domainCounts, setDomainCounts] = useState(null);
    const [domainCountsError, setDomainCountsError] = useState(false);
    const [domainCountsLoading, setDomainCountsLoading] = useState(false);
    const [hoverId, setHoverId] = useState(null);
    const [inputError, setInputError] = useState(null);
    const [menuOpen, setMenuOpen] = useState(false);
    const [searchTerms, setSearchTerms] = useState('');
    const [subHoverId, setSubHoverId] = useState(null);
    const [subMenuOpen, setSubMenuOpen] = useState(false);
    const menuRef = useRef(null);

    const getDomainCounts = () => {
      const { id, namespace } = workspace;
      cohortBuilderApi()
        .findUniversalDomainCounts(namespace, id, searchTerms)
        .then((response) => {
          setDomainCounts(response.items);
          setDomainCountsLoading(false);
        })
        .catch((error) => {
          console.error(error);
          setDomainCountsError(true);
        });
    };

    const onEnterPress = () => {
      const inputErrors = validateInputForMySQL(searchTerms, searchTrigger);
      if (inputErrors.length > 0) {
        setInputError(inputErrors.join('\r\n'));
      } else if (!searchTerms || searchTerms.length < searchTrigger) {
        setInputError('Minimum search length is two characters.');
      } else {
        setDomainCountsError(false);
        setDomainCountsLoading(true);
        setInputError(null);
        getDomainCounts();
      }
    };

    const categoryHasResults = (index: number) => {
      return domainCounts?.some((domainCount) =>
        menuOptions[index].some(
          (menuOption) => domainCount.domain === menuOption.domain
        )
      );
    };
    const showMenuItem = (index: number) => {
      return domainCounts === null || categoryHasResults(index);
    };

    const closeAndClearMenu = () => {
      setMenuOpen(false);
      setSubMenuOpen(false);
      setHoverId(null);
      setSubHoverId(null);
      setDomainCounts(null);
      setDomainCountsError(false);
      setInputError(null);
      setSearchTerms('');
    };

    const onMenuItemClick = (menuItem) => {
      if (typeof temporalGroup !== 'undefined') {
        launchSearch(menuItem, temporalGroup, searchTerms);
      } else {
        launchSearch(menuItem, searchTerms);
      }
      closeAndClearMenu();
    };

    const onClickOutside = (event) => {
      if (
        menuRef?.current &&
        !menuRef.current.contains(event.target) &&
        menuOpen
      ) {
        closeAndClearMenu();
      }
    };

    useEffect(() => {
      // Close menu on outside click
      document.addEventListener('click', onClickOutside);
      return () => {
        document.removeEventListener('click', onClickOutside);
      };
    });
    return (
      <div style={styles.cardBlock}>
        <button
          data-test-id='criteria-menu-button'
          style={styles.menuButton}
          onClick={() => {
            if (!menuOpen) {
              setMenuOpen(true);
            }
          }}
        >
          Add Criteria <ClrIcon shape='caret down' size={12} />
        </button>
        {menuOpen && (
          <div
            className='p-tieredmenu p-menu-overlay-visible'
            data-test-id='criteria-menu-dropdown'
            ref={menuRef}
            style={styles.dropdownMenu}
          >
            <div style={styles.searchContainer}>
              <span style={styles.dropdownHeaderText}>
                Search or browse all domains
                {(
                  <TooltipTrigger side='top' content={searchTooltip}>
                    <ClrIcon
                      style={styles.infoIcon}
                      className='is-solid'
                      shape='info-standard'
                    />
                  </TooltipTrigger>
                )}
              </span>
              <div style={styles.searchBar}>
                {domainCountsLoading ? (
                  <Spinner style={{ verticalAlign: 'middle' }} size={16} />
                ) : (
                  <ClrIcon shape='search' size='18' />
                )}
                <TextInput
                  data-test-id='criteria-menu-input'
                  style={styles.searchInput}
                  value={searchTerms}
                  onChange={(val) => setSearchTerms(val)}
                  onKeyDown={(e) => e.key === 'Enter' && onEnterPress()}
                />
                <ClrIcon
                  shape='times'
                  size='24'
                  style={{ color: colors.secondary, cursor: 'pointer' }}
                  onClick={() => {
                    setDomainCounts(null);
                    setSearchTerms('');
                    setInputError(null);
                  }}
                />
              </div>
            </div>
            {!domainCountsLoading && (
              <React.Fragment>
                {domainCountsError && (
                  <div style={styles.error}>
                    <ClrIcon
                      style={{ margin: '0 0.5rem 0 0.25rem' }}
                      className='is-solid'
                      shape='exclamation-triangle'
                      size='22'
                    />
                    Sorry, the request cannot be completed. Please try again or
                    contact Support in the left hand navigation.
                  </div>
                )}
                {!!inputError && (
                  <AlertDanger
                    data-test-id='criteria-menu-input-alert'
                    style={{ margin: '0 0.5rem 0.25rem', padding: '0.25rem' }}
                  >
                    <span>{inputError}</span>
                  </AlertDanger>
                )}
                {domainCounts?.length === 0 ? (
                  <div style={{ padding: '0.25rem 0.5rem' }}>
                    No results found
                  </div>
                ) : menuOptions.length === 0 ? (
                  <div style={{ textAlign: 'center' }}>
                    <Spinner size={36} />
                  </div>
                ) : (
                  menuOptions
                    .filter((optionList) =>
                      isTemporal
                        ? optionList[0].category !== 'Program Data'
                        : optionList
                    )
                    .map((category, index) => (
                      <ul key={index}>
                        {showMenuItem(index) && (
                          <li
                            style={styles.dropdownHeader}
                            className='menuitem-header'
                          >
                            <span style={styles.dropdownHeaderText}>
                              {category[0].category}
                            </span>
                          </li>
                        )}
                        {category
                          .filter(
                            (menuItem) =>
                              domainCounts === null ||
                              domainCounts.find(
                                (dc) => dc.domain === menuItem.domain
                              )
                          )
                          .map((menuItem, m) => (
                            <li
                              key={m}
                              style={{
                                ...styles.dropdownItem,
                                ...(hoverId === `${index}-${m}`
                                  ? {
                                      background: colorWithWhiteness(
                                        colors.light,
                                        0.5
                                      ),
                                    }
                                  : {}),
                              }}
                              onMouseEnter={() => {
                                setHoverId(`${index}-${m}`);
                                if (menuItem.group) {
                                  setSubMenuOpen(true);
                                }
                              }}
                              onMouseLeave={() => {
                                setHoverId(null);
                                if (menuItem.group) {
                                  setSubMenuOpen(false);
                                }
                              }}
                            >
                              <a
                                role='menuitem'
                                aria-haspopup={menuItem.group}
                                key={m}
                                style={styles.dropdownLink}
                                onClick={() => {
                                  if (
                                    !menuItem.group ||
                                    categoryHasResults(index)
                                  ) {
                                    onMenuItemClick(menuItem);
                                  }
                                }}
                              >
                                <span style={{ verticalAlign: 'middle' }}>
                                  {menuItem.name}
                                </span>
                                {domainCounts !== null && (
                                  <span style={styles.count}>
                                    {domainCounts
                                      .find(
                                        (dc) => dc.domain === menuItem.domain
                                      )
                                      .count.toLocaleString()}
                                  </span>
                                )}
                              </a>
                              {menuItem.group && !categoryHasResults(index) && (
                                <React.Fragment>
                                  <i
                                    style={styles.subMenuIcon}
                                    className='pi pi-sort-down'
                                  />
                                  {hoverId === `${index}-${m}` && subMenuOpen && (
                                    <ul style={styles.subMenu}>
                                      {menuItem.children?.map(
                                        (subMenuItem, s) => (
                                          <li>
                                            <a
                                              role='menuitem'
                                              key={s}
                                              style={{
                                                ...styles.subMenuItem,
                                                ...(subHoverId ===
                                                `${index}-${m}-${s}`
                                                  ? {
                                                      background:
                                                        colorWithWhiteness(
                                                          colors.light,
                                                          0.5
                                                        ),
                                                    }
                                                  : {}),
                                              }}
                                              onMouseEnter={() =>
                                                setSubHoverId(
                                                  `${index}-${m}-${s}`
                                                )
                                              }
                                              onMouseLeave={() => {
                                                setSubHoverId(null);
                                              }}
                                              onClick={(e) => {
                                                e.stopPropagation();
                                                onMenuItemClick(subMenuItem);
                                              }}
                                            >
                                              {subMenuItem.name}
                                            </a>
                                          </li>
                                        )
                                      )}
                                    </ul>
                                  )}
                                </React.Fragment>
                              )}
                            </li>
                          ))}
                      </ul>
                    ))
                )}
              </React.Fragment>
            )}
          </div>
        )}
      </div>
    );
  }
);
