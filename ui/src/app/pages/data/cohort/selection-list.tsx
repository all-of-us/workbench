import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Attribute,
  Criteria,
  Domain,
  Modifier,
  VariantFilter,
  VariantFilterInfoResponse,
} from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow, FlexRowWrap } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { AttributesPage } from 'app/pages/data/cohort/attributes-page';
import {
  getItemFromSearchRequest,
  saveCriteria,
} from 'app/pages/data/cohort/cohort-search';
import { VARIANT_DISPLAY } from 'app/pages/data/cohort/constant';
import { ModifierPage } from 'app/pages/data/cohort/modifier-page';
import { nameDisplay, typeDisplay } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortCriteria,
  withCurrentCohortSearchContext,
} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  currentWorkspaceStore,
  sidebarActiveIconStore,
} from 'app/utils/navigation';
import arrowLeft from 'assets/icons/arrow-left-regular.svg';
import times from 'assets/icons/times-light.svg';
import { Subscription } from 'rxjs/Subscription';

const proIcons = {
  arrowLeft: arrowLeft,
  times: times,
};

const styles = reactStyles({
  buttonContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: '0.75rem',
    padding: '0.675rem 0rem',
  },
  button: {
    fontSize: '12px',
    height: '2.25rem',
    letterSpacing: '0.03rem',
    lineHeight: '1.125rem',
    margin: '0.375rem 0.75rem',
    padding: '0rem 1.125rem',
  },
  itemInfo: {
    width: '100%',
    minWidth: 0,
    flex: 1,
    display: 'flex',
    flexFlow: 'row nowrap',
    justifyContent: 'flex-start',
  },
  itemName: {
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  modifierButtonContainer: {
    bottom: '0.75rem',
    paddingLeft: '0.75rem',
    position: 'absolute',
    width: 'calc(100% - 2.25rem)',
  },
  removeSelection: {
    background: 'none',
    border: 0,
    color: colors.danger,
    cursor: 'pointer',
    marginRight: '0.375rem',
    padding: 0,
  },
  editSelectAll: {
    background: 'none',
    border: 0,
    cursor: 'pointer',
    marginRight: '0.375rem',
    padding: 0,
  },
  saveButton: {
    height: '3rem',
    borderRadius: '5px',
    fontWeight: 600,
    marginRight: '0.75rem',
  },
  selectionContainer: {
    background: colors.white,
    border: `2px solid ${colors.primary}`,
    borderRadius: '5px',
    height: '80%',
    lineHeight: '1.125rem',
    marginTop: '0.75rem',
    padding: '0.75rem',
    position: 'relative',
  },
  selectionItem: {
    display: 'flex',
    fontSize: '14px',
    padding: '0',
    width: '100%',
    color: 'rgb(38, 34, 98)',
  },
  selectionList: {
    height: '100%',
    overflow: 'hidden auto',
    paddingLeft: '0.75rem',
  },
  selectionPanel: {
    height: '52.5rem',
    paddingTop: '0.75rem',
  },
  selectionTitle: {
    color: colors.primary,
    margin: 0,
    padding: '0.75rem 0',
  },
  modifierButton: {
    height: '2.625rem',
    maxWidth: '100%',
    width: '100%',
    backgroundColor: colors.white,
    border: '0.15rem solid' + colors.accent,
    color: colors.accent,
  },
  selectionHeader: {
    borderBottom: `1px solid ${colors.disabled}`,
    color: colors.primary,
    display: 'inline-block',
    fontSize: '13px',
    fontWeight: 600,
    paddingRight: '0.375rem',
  },
  sectionTitle: {
    marginTop: '0.75rem',
    fontWeight: 600,
    color: colors.primary,
  },
  navIcons: {
    alignItems: 'center',
    position: 'absolute',
    right: '0',
    top: '1.125rem',
  },
  caret: {
    cursor: 'pointer',
    float: 'right',
    marginTop: '3px',
    transition: 'transform 0.1s ease-out',
  },
  filterContainer: {
    height: 'auto',
    overflow: 'hidden',
    transition: 'max-height 0.4s ease-out',
  },
  filterList: {
    lineHeight: '1.25rem',
    listStyle: 'none',
    margin: 0,
    paddingLeft: '1rem',
  },
});

function mapCriteria(crit: Selection) {
  return {
    attributes: crit.attributes,
    code: crit.code,
    domainId: crit.domainId,
    group: crit.group,
    hasAncestorData: crit.hasAncestorData,
    standard: crit.standard,
    name: crit.name,
    parameterId: crit.parameterId,
    type: crit.type,
    variantFilter: crit.variantFilter,
  };
}

export interface Selection extends Criteria {
  attributes?: Array<Attribute>;
  parameterId: string;
  variantFilter?: VariantFilter;
  variantId?: string;
}

interface SelectionInfoProps {
  index: number;
  selection: Selection;
  removeSelection: Function;
  cohortContext?: any;
}

interface SelectionInfoState {
  filtersExpanded: boolean;
  loadingVariantBuckets: boolean;
  truncated: boolean;
  variantFilterInfoResponse: VariantFilterInfoResponse;
}

export const SelectionInfo = withCurrentCohortSearchContext()(
  class extends React.Component<SelectionInfoProps, SelectionInfoState> {
    name: HTMLDivElement;
    constructor(props: SelectionInfoProps) {
      super(props);
      this.state = {
        filtersExpanded: false,
        loadingVariantBuckets: !!props.selection.variantFilter,
        truncated: false,
        variantFilterInfoResponse: null,
      };
    }

    componentDidMount(): void {
      if (this.props.selection.variantFilter) {
        this.getVariantFilterBuckets();
      }
      const { offsetWidth, scrollWidth } = this.name;
      this.setState({ truncated: scrollWidth > offsetWidth });
    }

    componentDidUpdate(prevProps: Readonly<SelectionInfoProps>) {
      if (
        this.props.selection.variantFilter?.exclusionList !==
        prevProps.selection.variantFilter?.exclusionList
      ) {
        this.setState({ loadingVariantBuckets: true });
        this.getVariantFilterBuckets();
      }
    }

    get showType() {
      return ![
        Domain.PHYSICAL_MEASUREMENT.toString(),
        Domain.DRUG.toString(),
        Domain.SURVEY.toString(),
      ].includes(this.props.selection.domainId);
    }

    async getVariantFilterBuckets() {
      const { namespace, id } = currentWorkspaceStore.getValue();
      try {
        const filterBucketResponse =
          await cohortBuilderApi().findVariantFilterInfo(
            namespace,
            id,
            this.props.selection.variantFilter
          );
        this.setState({ variantFilterInfoResponse: filterBucketResponse });
      } catch (error) {
        console.error(error);
      } finally {
        this.setState({ loadingVariantBuckets: false });
      }
    }

    renderVariantFilters() {
      return (
        <ul style={styles.filterList}>
          {Object.entries(this.props.selection.variantFilter)
            .filter(
              ([key, value]) =>
                !(
                  (Array.isArray(value) && value.length === 0) ||
                  ['', null].includes(value) ||
                  key === 'sortBy'
                )
            )
            .map(([key, value], index) => (
              <li key={index}>
                <b>{VARIANT_DISPLAY[key]}</b>:{' '}
                {Array.isArray(value) ? (
                  <>
                    <br />
                    {value.join(', ')}
                  </>
                ) : (
                  value.toLocaleString()
                )}
              </li>
            ))}
          <li>
            <b>Participant Count Overview:</b>
            {this.state.loadingVariantBuckets ? (
              <div>
                <Spinner size={24} style={{ margin: '1rem 5rem' }} />
              </div>
            ) : (
              <ul style={styles.filterList}>
                {!!this.state.variantFilterInfoResponse &&
                  Object.entries(this.state.variantFilterInfoResponse)
                    .filter(([, value]) => value !== 0)
                    .map(([key, value], index) => (
                      <li key={index}>
                        <b>{VARIANT_DISPLAY[key]}</b>: {value.toLocaleString()}
                      </li>
                    ))}
              </ul>
            )}
          </li>
        </ul>
      );
    }

    render() {
      const { cohortContext, index, selection, removeSelection } = this.props;
      const { filtersExpanded, truncated } = this.state;
      const itemName = (
        <React.Fragment>
          {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
          {nameDisplay(selection)}
        </React.Fragment>
      );
      return (
        <FlexColumn style={styles.selectionItem}>
          {index > 0 && (
            <div style={{ padding: '0.45rem 0rem 0.45rem 1.5rem' }}>
              OR&nbsp;
            </div>
          )}
          <FlexRow
            style={{
              alignItems: 'baseline',
              ...(cohortContext.editSelectAll?.parameterId ===
              selection.parameterId
                ? { cursor: 'not-allowed', opacity: 0.4 }
                : {}),
            }}
          >
            <button
              style={styles.removeSelection}
              onClick={() => removeSelection()}
              title='Remove Selection'
              disabled={
                cohortContext.editSelectAll?.parameterId ===
                selection.parameterId
              }
            >
              <ClrIcon shape='times-circle' />
            </button>
            {!!selection.variantFilter && (
              <button
                style={styles.editSelectAll}
                onClick={() =>
                  currentCohortSearchContextStore.next({
                    ...cohortContext,
                    editSelectAll: selection,
                  })
                }
                title='Edit Select All Group'
                disabled={
                  cohortContext.editSelectAll?.parameterId ===
                  selection.parameterId
                }
              >
                <ClrIcon shape={'pencil'} />
              </button>
            )}
            <FlexColumn style={{ width: 'calc(100% - 1.5rem)' }}>
              {selection.group && <div>Group</div>}
              <TooltipTrigger disabled={!truncated} content={itemName}>
                <div style={styles.itemName} ref={(e) => (this.name = e)}>
                  {itemName}
                </div>
              </TooltipTrigger>
            </FlexColumn>
            {!!selection.variantFilter && (
              <ClrIcon
                style={{
                  ...styles.caret,
                  ...(filtersExpanded ? { transform: 'rotate(90deg)' } : {}),
                }}
                shape={'caret right'}
                size={18}
                onClick={() =>
                  this.setState({ filtersExpanded: !filtersExpanded })
                }
              />
            )}
          </FlexRow>
          {!!selection.variantFilter && (
            <div
              style={{
                ...styles.filterContainer,
                maxHeight: filtersExpanded ? '22.5rem' : 0,
              }}
            >
              {this.renderVariantFilters()}
            </div>
          )}
        </FlexColumn>
      );
    }
  }
);

interface Props {
  back: Function;
  close: Function;
  disableFinish: boolean;
  domain: Domain;
  finish: Function;
  removeSelection: Function;
  selections: Array<Selection>;
  setView: Function;
  view: string;
  cohortContext?: any;
  criteria?: Array<Selection>;
}

interface State {
  attributesSelection: Criteria;
  disableSave: boolean;
  modifiers: Array<Modifier>;
  showModifiersSlide: boolean;
}

export const SelectionList = fp.flow(
  withCurrentCohortCriteria(),
  withCurrentCohortSearchContext()
)(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        attributesSelection: undefined,
        disableSave: false,
        modifiers: [],
        showModifiersSlide: false,
      };
    }

    componentDidMount(): void {
      const { cohortContext } = this.props;
      if (!!cohortContext) {
        // Check for disabling the Save Criteria button
        this.setState({ modifiers: cohortContext.item.modifiers }, () =>
          this.checkCriteriaChanges()
        );
      }
      this.subscription = attributesSelectionStore.subscribe(
        (attributesSelection) => {
          this.setState({ attributesSelection });
          if (!!attributesSelection) {
            sidebarActiveIconStore.next('criteria');
          }
        }
      );
      this.subscription.add(
        currentCohortCriteriaStore.subscribe(() => {
          if (!!this.props.cohortContext) {
            // Each time the criteria changes, we check for disabling the Save Criteria button again
            setTimeout(() => this.checkCriteriaChanges());
          }
        })
      );
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      const { criteria } = this.props;
      if (!criteria && !!prevProps.criteria) {
        this.setState({ showModifiersSlide: false });
      }
    }

    componentWillUnmount() {
      attributesSelectionStore.next(undefined);
    }

    checkCriteriaChanges() {
      const {
        cohortContext: { groupId, item, role },
        criteria,
      } = this.props;
      if (criteria.length === 0) {
        this.setState({ disableSave: true, modifiers: [] });
      } else {
        const requestItem = getItemFromSearchRequest(groupId, item.id, role);
        if (requestItem) {
          // We need to check for changes to criteria selections AND the modifiers array
          const mappedCriteriaString = JSON.stringify({
            criteria: criteria.map(mapCriteria),
            modifiers: this.state.modifiers,
          });
          const mappedParametersString = JSON.stringify({
            criteria: requestItem.searchParameters.map(mapCriteria),
            modifiers: requestItem.modifiers,
          });
          this.setState({
            disableSave: mappedCriteriaString === mappedParametersString,
          });
        } else {
          this.setState({ disableSave: false });
        }
      }
    }

    removeCriteria(criteriaToDel) {
      const updateList = fp.remove(
        (selection) => selection.parameterId === criteriaToDel.parameterId,
        this.props.criteria
      );
      currentCohortCriteriaStore.next(updateList);
    }

    closeModifiers(modifiers) {
      if (modifiers) {
        this.setState({ modifiers }, () => this.checkCriteriaChanges());
      }
      this.setState({ showModifiersSlide: false });
    }

    closeSidebar() {
      attributesSelectionStore.next(undefined);
      sidebarActiveIconStore.next(null);
    }

    get showModifierButton() {
      const { criteria } = this.props;
      return (
        criteria &&
        criteria.length > 0 &&
        criteria[0].domainId !== Domain.PHYSICAL_MEASUREMENT.toString() &&
        criteria[0].domainId !== Domain.PERSON.toString() &&
        criteria[0].domainId !== Domain.SNP_INDEL_VARIANT.toString()
      );
    }

    get showAttributesOrModifiers() {
      const { attributesSelection, showModifiersSlide } = this.state;
      return attributesSelection || showModifiersSlide;
    }

    renderSelections() {
      const { criteria } = this.props;
      if (
        [Domain.CONDITION.toString(), Domain.PROCEDURE.toString()].includes(
          criteria[0].domainId
        )
      ) {
        // Separate selections by standard and source concepts for Condition and Procedures
        const standardConcepts = criteria.filter((con) => con.standard);
        const sourceConcepts = criteria.filter((con) => !con.standard);
        return (
          <React.Fragment>
            {standardConcepts.length > 0 && (
              <div style={{ marginBottom: '0.75rem' }}>
                <div style={styles.selectionHeader}>Standard Concepts</div>
                {standardConcepts.map((selection, index) => (
                  <SelectionInfo
                    key={index}
                    index={index}
                    selection={selection}
                    removeSelection={() => this.removeCriteria(selection)}
                  />
                ))}
              </div>
            )}
            {sourceConcepts.length > 0 && (
              <div>
                <div style={styles.selectionHeader}>Source Concepts</div>
                {sourceConcepts.map((selection, index) => (
                  <SelectionInfo
                    key={index}
                    index={index}
                    selection={selection}
                    removeSelection={() => this.removeCriteria(selection)}
                  />
                ))}
              </div>
            )}
          </React.Fragment>
        );
      } else {
        return criteria.map((selection, index) => (
          <SelectionInfo
            key={index}
            index={index}
            selection={selection}
            removeSelection={() => this.removeCriteria(selection)}
          />
        ));
      }
    }

    render() {
      const { back, cohortContext, criteria } = this.props;
      const { attributesSelection, disableSave, showModifiersSlide } =
        this.state;
      return (
        <div id='selection-list' style={{ height: '100%' }}>
          <FlexRow style={styles.navIcons}>
            {showModifiersSlide && (
              <Clickable
                style={{ marginRight: '1.5rem' }}
                onClick={() => this.setState({ showModifiersSlide: false })}
              >
                <img
                  src={proIcons.arrowLeft}
                  style={{ height: '21px', width: '18px' }}
                  alt='Go back'
                />
              </Clickable>
            )}
            <Clickable
              style={{ marginRight: '1.5rem' }}
              onClick={() => this.closeSidebar()}
            >
              <img
                src={proIcons.times}
                style={{ height: '27px', width: '17px' }}
                alt='Close'
              />
            </Clickable>
          </FlexRow>
          {!this.showAttributesOrModifiers && (
            <React.Fragment>
              <h3 style={{ ...styles.sectionTitle, marginTop: 0 }}>
                Add selected criteria to cohort
              </h3>
              <div style={styles.selectionContainer}>
                {!!criteria && (
                  <div
                    style={
                      this.showModifierButton
                        ? {
                            ...styles.selectionList,
                            height: 'calc(100% - 3rem)',
                          }
                        : styles.selectionList
                    }
                  >
                    {!!criteria &&
                      criteria.length > 0 &&
                      this.renderSelections()}
                  </div>
                )}
                {this.showModifierButton && (
                  <div style={styles.modifierButtonContainer}>
                    <Button
                      type='secondaryLight'
                      style={styles.modifierButton}
                      onClick={() =>
                        this.setState({ showModifiersSlide: true })
                      }
                    >
                      {cohortContext.item.modifiers.length > 0
                        ? '(' +
                          cohortContext.item.modifiers.length +
                          ')  MODIFIERS APPLIED'
                        : 'APPLY MODIFIERS'}
                    </Button>
                  </div>
                )}
              </div>
              <FlexRowWrap
                style={{ flexDirection: 'row-reverse', marginTop: '1.5rem' }}
              >
                <Button
                  type='primary'
                  style={styles.saveButton}
                  disabled={disableSave || !!cohortContext?.editSelectAll}
                  onClick={() => saveCriteria()}
                >
                  Save Criteria
                </Button>
                <Button
                  type='link'
                  style={{ color: colors.primary, marginRight: '0.75rem' }}
                  onClick={() => back()}
                >
                  Back
                </Button>
              </FlexRowWrap>
            </React.Fragment>
          )}
          {showModifiersSlide && !attributesSelection && (
            <ModifierPage
              selections={criteria}
              closeModifiers={(modifiers) => this.closeModifiers(modifiers)}
            />
          )}
          {!!attributesSelection && (
            <AttributesPage
              back={() => this.closeSidebar()}
              close={() => attributesSelectionStore.next(undefined)}
              node={attributesSelection}
            />
          )}
        </div>
      );
    }
  }
);
