import * as React from 'react';
import * as fp from 'lodash/fp';

import { Attribute, Criteria, Domain, Modifier } from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { FlexColumn, FlexRow, FlexRowWrap } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { AttributesPage } from 'app/pages/data/cohort/attributes-page';
import {
  getItemFromSearchRequest,
  saveCriteria,
} from 'app/pages/data/cohort/cohort-search';
import { ModifierPage } from 'app/pages/data/cohort/modifier-page';
import { nameDisplay, typeDisplay } from 'app/pages/data/cohort/utils';
import colors from 'app/styles/colors';
import {
  reactStyles,
  withCurrentCohortCriteria,
  withCurrentCohortSearchContext,
} from 'app/utils';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  setSidebarActiveIconStore,
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
    position: 'absolute',
    right: '0',
    top: '1.125rem',
  },
});

function mapCriteria(crit: Selection) {
  return {
    attributes: crit.attributes,
    code: crit.code,
    domainId: crit.domainId,
    group: crit.group,
    hasAncestorData: crit.hasAncestorData,
    isStandard: crit.isStandard,
    name: crit.name,
    parameterId: crit.parameterId,
    type: crit.type,
  };
}

export interface Selection extends Criteria {
  attributes?: Array<Attribute>;
  parameterId: string;
}

interface SelectionInfoProps {
  index: number;
  selection: Selection;
  removeSelection: Function;
}

interface SelectionInfoState {
  truncated: boolean;
}

export class SelectionInfo extends React.Component<
  SelectionInfoProps,
  SelectionInfoState
> {
  name: HTMLDivElement;
  constructor(props: SelectionInfoProps) {
    super(props);
    this.state = { truncated: false };
  }

  componentDidMount(): void {
    const { offsetWidth, scrollWidth } = this.name;
    this.setState({ truncated: scrollWidth > offsetWidth });
  }

  get showType() {
    return ![
      Domain.PHYSICALMEASUREMENT.toString(),
      Domain.DRUG.toString(),
      Domain.SURVEY.toString(),
    ].includes(this.props.selection.domainId);
  }

  render() {
    const { index, selection, removeSelection } = this.props;
    const itemName = (
      <React.Fragment>
        {this.showType && <strong>{typeDisplay(selection)}&nbsp;</strong>}
        {nameDisplay(selection)}
      </React.Fragment>
    );
    return (
      <FlexColumn style={styles.selectionItem}>
        {index > 0 && (
          <div style={{ padding: '0.45rem 0rem 0.45rem 1.5rem' }}>OR&nbsp;</div>
        )}
        <FlexRow style={{ alignItems: 'baseline' }}>
          <button
            style={styles.removeSelection}
            onClick={() => removeSelection()}
          >
            <ClrIcon shape='times-circle' />
          </button>
          <FlexColumn style={{ width: 'calc(100% - 1.5rem)' }}>
            {selection.group && <div>Group</div>}
            <TooltipTrigger disabled={!this.state.truncated} content={itemName}>
              <div style={styles.itemName} ref={(e) => (this.name = e)}>
                {itemName}
              </div>
            </TooltipTrigger>
          </FlexColumn>
        </FlexRow>
      </FlexColumn>
    );
  }
}
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
            setSidebarActiveIconStore.next('criteria');
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
      setSidebarActiveIconStore.next(null);
    }

    get showModifierButton() {
      const { criteria } = this.props;
      return (
        criteria &&
        criteria.length > 0 &&
        criteria[0].domainId !== Domain.PHYSICALMEASUREMENT.toString() &&
        criteria[0].domainId !== Domain.PERSON.toString()
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
        const standardConcepts = criteria.filter((con) => con.isStandard);
        const sourceConcepts = criteria.filter((con) => !con.isStandard);
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
                  disabled={disableSave}
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
