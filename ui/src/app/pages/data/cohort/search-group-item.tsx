import * as React from 'react';
import { Menu } from 'primereact/menu';
import { OverlayPanel } from 'primereact/overlaypanel';

import {
  CohortDefinition,
  CriteriaSubType,
  CriteriaType,
  Domain,
  Modifier,
  ModifierType,
  ResourceType,
  SearchGroupItem as Item,
} from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { Modal, ModalFooter, ModalTitle } from 'app/components/modals';
import { RenameModal } from 'app/components/rename-modal';
import { MODIFIERS_MAP } from 'app/pages/data/cohort/constant';
import {
  encountersStore,
  searchRequestStore,
} from 'app/pages/data/cohort/search-state.service';
import {
  domainToTitle,
  getTypeAndStandard,
  mapGroupItem,
  typeToTitle,
} from 'app/pages/data/cohort/utils';
import { ConceptAddModal } from 'app/pages/data/concept/concept-add-modal';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import {
  currentCohortCriteriaStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  menu: {
    minWidth: '7.5rem',
    maxWidth: '22.5rem',
    width: 'auto',
  },
  lineItem: {
    minWidth: 0,
    flex: 4,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  suppressedItem: {
    color: colorWithWhiteness(colors.warning, -0.35),
    flex: 4,
  },
  codeText: {
    fontWeight: 300,
    color: colors.dark,
  },
  link: {
    height: 'auto',
    minWidth: '3rem',
    marginLeft: '0.375rem',
    padding: 0,
    fontSize: '12px',
    fontWeight: 600,
  },
  caret: {
    cursor: 'pointer',
    float: 'right',
    marginTop: '3px',
    transition: 'transform 0.1s ease-out',
  },
  parameterList: {
    height: 'auto',
    overflow: 'hidden',
    transition: 'max-height 0.4s ease-out',
  },
  parameter: {
    fontSize: '12px',
    color: colors.dark,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  viewMore: {
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '12px',
  },
  disabled: {
    color: colors.accent,
    pointerEvents: 'none',
    opacity: 0.6,
    fontSize: '12px',
  },
});

const itemStyles = `
  body .p-menu .p-menu-list {
    padding: 0.75rem 0;
  }
  body .p-menu .p-menuitem-link {
   padding: 0 1.5rem;
   line-height: 1.875rem;
  }
  .item-title:hover {
    cursor: pointer;
    color: ${colors.accent}!important
  }
`;

class SearchGroupItemParameter extends React.Component<
  { parameter: any },
  { tooltip: boolean }
> {
  element: HTMLDivElement;
  overlay: any;
  constructor(props: any) {
    super(props);
    this.state = { tooltip: false };
  }

  componentDidMount(): void {
    const { offsetWidth, scrollWidth } = this.element;
    this.setState({ tooltip: scrollWidth > offsetWidth });
  }

  render() {
    const {
      parameter,
      parameter: { domainId },
    } = this.props;
    const { tooltip } = this.state;
    const showCode = [
      Domain.CONDITION,
      Domain.DRUG,
      Domain.MEASUREMENT,
      Domain.PROCEDURE,
    ].includes(domainId);
    return (
      <div ref={(el) => (this.element = el)} style={styles.parameter}>
        <span
          style={
            domainId === Domain.PERSON ? { textTransform: 'capitalize' } : {}
          }
          onMouseEnter={(e) => tooltip && this.overlay.show(e)}
          onMouseLeave={() => tooltip && this.overlay.hide()}
        >
          {showCode && <b>{parameter.code}</b>} {parameter.name}
        </span>
        {tooltip && (
          <OverlayPanel
            style={{ maxWidth: '30%' }}
            ref={(el) => (this.overlay = el)}
            appendTo={document.body}
          >
            {parameter.name}
          </OverlayPanel>
        )}
      </div>
    );
  }
}

const SKIP_DOMAIN_SEARCH = [
  Domain.FITBIT.toString(),
  Domain.WHOLEGENOMEVARIANT.toString(),
  Domain.LRWHOLEGENOMEVARIANT.toString(),
  Domain.ARRAYDATA.toString(),
  Domain.STRUCTURALVARIANTDATA.toString(),
];

interface ItemProp extends Item {
  count: number;
  status: string;
}

interface Props {
  groupId: string;
  item: ItemProp;
  index: number;
  setSearchContext: (context: any) => void;
  role: keyof CohortDefinition;
  updateGroup: Function;
  workspace: WorkspaceData;
}

interface State {
  encounters: any;
  error: boolean;
  loading: boolean;
  paramListOpen: boolean;
  renaming: boolean;
  showConceptSetModal: boolean;
  showConceptSuccessModal: boolean;
  timeout: NodeJS.Timeout;
}

export const SearchGroupItem = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    actionsMenu: any;
    constructor(props: Props) {
      super(props);
      this.state = {
        encounters: encountersStore.getValue(),
        error: false,
        loading: true,
        paramListOpen: false,
        renaming: false,
        showConceptSetModal: false,
        showConceptSuccessModal: false,
        timeout: null,
      };
    }

    componentDidMount(): void {
      const {
        item: { count, modifiers },
        workspace: { id, namespace },
      } = this.props;
      const { encounters } = this.state;
      if (count !== undefined) {
        this.setState({ loading: false });
      } else {
        this.getItemCount();
      }
      if (
        !!modifiers &&
        modifiers.some((mod) => mod.name === ModifierType.ENCOUNTERS) &&
        !encounters
      ) {
        cohortBuilderApi()
          .findCriteriaBy(
            namespace,
            id,
            Domain[Domain.VISIT],
            CriteriaType[CriteriaType.VISIT]
          )
          .then((res) => {
            encountersStore.next(res.items);
            this.setState({ encounters: res.items });
          });
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      const {
        item: { searchParameters },
      } = this.props;
      if (prevProps.item.searchParameters !== searchParameters) {
        this.setState({ loading: true }, () => this.getItemCount());
      }
    }

    async getItemCount() {
      const { item, role, updateGroup } = this.props;
      try {
        updateGroup();
        const { id, namespace } = currentWorkspaceStore.getValue();
        const mappedItem = mapGroupItem(item, false);
        const request = {
          includes: [],
          excludes: [],
          dataFilters: [],
          [role]: [{ items: [mappedItem], temporal: false }],
        };
        await cohortBuilderApi()
          .countParticipants(namespace, id, request)
          .then((count) => this.updateSearchRequest('count', count, false));
      } catch (error) {
        console.error(error);
        this.setState({ error: true });
      } finally {
        this.setState({ loading: false });
      }
    }

    enable() {
      AnalyticsTracker.CohortBuilder.SearchGroupItemMenu(
        'Enable suppressed item'
      );
      this.updateSearchRequest('status', 'active', true);
    }

    suppress() {
      AnalyticsTracker.CohortBuilder.SearchGroupItemMenu('Suppress item');
      this.updateSearchRequest('status', 'hidden', true);
    }

    remove() {
      AnalyticsTracker.CohortBuilder.SearchGroupItemMenu('Delete item');
      this.updateSearchRequest('status', 'pending', true);
      const timeout: NodeJS.Timeout = global.setTimeout(() => {
        this.updateSearchRequest(null, null, false, true);
      }, 10000);
      this.setState({ timeout });
    }

    undo() {
      AnalyticsTracker.CohortBuilder.SearchGroupItemMenu('Undo item delete');
      clearTimeout(this.state.timeout);
      this.updateSearchRequest('status', 'active', true);
    }

    rename(newName: string) {
      this.updateSearchRequest('name', newName, false);
      this.setState({ renaming: false });
    }

    updateSearchRequest(
      property: string,
      value: any,
      recalculate: boolean,
      remove?: boolean
    ) {
      const sr = searchRequestStore.getValue();
      const { item, groupId, role, updateGroup } = this.props;
      const groupIndex = sr[role].findIndex((grp) => grp.id === groupId);
      if (groupIndex > -1) {
        const itemIndex = sr[role][groupIndex].items.findIndex(
          (it) => it.id === item.id
        );
        if (itemIndex > -1) {
          if (remove) {
            sr[role][groupIndex].items = sr[role][groupIndex].items.filter(
              (it) => it.id !== item.id
            );
            searchRequestStore.next(sr);
          } else {
            sr[role][groupIndex].items[itemIndex] = {
              ...item,
              [property]: value,
            };
            searchRequestStore.next(sr);
            if (recalculate) {
              updateGroup();
            }
          }
        }
      }
    }

    launchSearch() {
      const { groupId, item, setSearchContext, role } = this.props;
      if (!this.preventItemEdit) {
        const _item = JSON.parse(JSON.stringify(item));
        const { searchParameters } = _item;
        const domain = _item.type;
        // If domain is PERSON, list the type as well as the domain in the label
        const { type, standard } = getTypeAndStandard(searchParameters, domain);
        const label = `Edit ${domainToTitle(domain)}${
          domain === Domain.PERSON ? ' - ' + typeToTitle(type) : ''
        } item - ${role === 'includes' ? 'Include' : 'Exclude'} Criteria`;
        currentCohortCriteriaStore.next(
          JSON.parse(JSON.stringify(searchParameters))
        );
        AnalyticsTracker.CohortBuilder.LaunchSearch(label);
        const context = {
          item: _item,
          domain,
          type,
          role,
          groupId,
          standard,
          count: item.count,
        };
        setSearchContext(context);
      }
    }

    modifierDisplay(mod: Modifier) {
      const { name, operands, operator } = mod;
      switch (name) {
        case ModifierType.ENCOUNTERS:
          const { encounters } = this.state;
          const visit = !!encounters
            ? encounters.find((en) => en.conceptId.toString() === operands[0])
                .name
            : '';
          return (
            <span>
              <b>{MODIFIERS_MAP[name].name}</b> {visit}
            </span>
          );
        case ModifierType.CATI:
          return <span>{MODIFIERS_MAP[name].operators[operands[0]]}</span>;
        case ModifierType.NUMOFOCCURRENCES:
          return (
            <span>
              <b>{MODIFIERS_MAP[name].name}</b> {operands[0]} Or More
            </span>
          );
        default:
          return (
            <span>
              <b>{MODIFIERS_MAP[name].name}</b>{' '}
              {MODIFIERS_MAP[name].operators[operator]} {operands.join(' and ')}
            </span>
          );
      }
    }

    get existingItemNames() {
      const { item, groupId, role } = this.props;
      const searchRequest = searchRequestStore.getValue();
      const group = searchRequest[role].find((grp) => grp.id === groupId);
      return !!group
        ? group.items
            .filter((it) => it.id !== item.id && !!it.name)
            .map((it) => it.name)
        : [];
    }

    get itemName() {
      const {
        item: { name, searchParameters, type },
      } = this.props;
      if (SKIP_DOMAIN_SEARCH.includes(type)) {
        return !!name ? name : searchParameters[0].name;
      } else {
        const codeDisplay = searchParameters.length > 1 ? 'Codes' : 'Code';
        const titleDisplay =
          type === Domain.PERSON.toString()
            ? typeToTitle(searchParameters[0].type)
            : domainToTitle(type);
        return !!name ? name : `Contains ${titleDisplay} ${codeDisplay}`;
      }
    }

    get preventItemEdit() {
      const {
        item: { searchParameters, type },
      } = this.props;
      return (
        SKIP_DOMAIN_SEARCH.includes(type) ||
        (searchParameters[0] &&
          searchParameters[0].type === CriteriaType.DECEASED.toString())
      );
    }

    get hideConceptOption() {
      const {
        item: { searchParameters, type },
      } = this.props;
      return (
        this.preventItemEdit ||
        type === Domain.PERSON.toString() ||
        !searchParameters.some(
          (param) => param.subtype !== CriteriaSubType.ANSWER.toString()
        )
      );
    }

    render() {
      const {
        item: { count, modifiers, name, searchParameters, status, type },
      } = this.props;
      const {
        error,
        loading,
        paramListOpen,
        renaming,
        showConceptSetModal,
        showConceptSuccessModal,
      } = this.state;
      const showCount = !loading && status !== 'hidden' && count !== undefined;
      const actionItems = [
        {
          label: 'Edit criteria name',
          command: () => this.setState({ renaming: true }),
        },
        {
          label: 'Edit criteria',
          command: () => this.launchSearch(),
          style: this.preventItemEdit ? { display: 'none' } : {},
        },
        {
          label: 'Suppress criteria from total count',
          command: () => this.suppress(),
        },
        { label: 'Delete criteria', command: () => this.remove() },
        {
          label: 'Add criteria to concept set',
          command: () => this.setState({ showConceptSetModal: true }),
          style: this.hideConceptOption ? { display: 'none' } : {},
        },
      ];
      return (
        <React.Fragment>
          <style>{itemStyles}</style>
          {status !== 'deleted' && (
            <div style={{ display: 'flex', fontSize: '12px' }}>
              {(status === 'active' || !status) && (
                <div style={styles.lineItem}>
                  <Menu
                    style={styles.menu}
                    appendTo={document.body}
                    model={actionItems}
                    popup={true}
                    ref={(el) => (this.actionsMenu = el)}
                  />
                  <Clickable
                    style={{ display: 'inline-block', paddingRight: '0.75rem' }}
                    onClick={(event) => this.actionsMenu.toggle(event)}
                  >
                    <ClrIcon shape='ellipsis-vertical' />
                  </Clickable>
                  <span
                    className='item-title'
                    style={{ ...styles.codeText, paddingRight: '10px' }}
                    onClick={() => this.launchSearch()}
                  >
                    {this.itemName}
                  </span>
                  {status !== 'hidden' && (
                    <span style={{ ...styles.codeText, paddingRight: '10px' }}>
                      |
                    </span>
                  )}
                  {loading && (
                    <span className='spinner spinner-inline'>Loading...</span>
                  )}
                  {showCount && (
                    <span style={styles.codeText}>
                      {count.toLocaleString()}
                    </span>
                  )}
                  {error && (
                    <span>
                      <ClrIcon
                        style={{ color: colors.warning }}
                        shape='exclamation-triangle'
                        className='is-solid'
                        size={22}
                      />
                    </span>
                  )}
                </div>
              )}
              {status === 'pending' && (
                <div style={styles.suppressedItem}>
                  <ClrIcon
                    shape='exclamation-triangle'
                    className='is-solid'
                    size={23}
                  />
                  <span style={{ margin: '0 0 2px 2px' }}>
                    This criteria has been deleted
                    <Button
                      type='link'
                      style={styles.link}
                      onClick={() => this.undo()}
                    >
                      UNDO
                    </Button>
                  </span>
                </div>
              )}
              {status === 'hidden' && (
                <div style={styles.suppressedItem}>
                  <ClrIcon shape='eye-hide' className='is-solid' size={23} />
                  <span style={{ margin: '0 0 2px 2px' }}>
                    This criteria has been suppressed
                    <Button
                      type='link'
                      style={styles.link}
                      onClick={() => this.enable()}
                    >
                      ENABLE
                    </Button>
                  </span>
                </div>
              )}
              <ClrIcon
                style={{
                  ...styles.caret,
                  ...(paramListOpen ? { transform: 'rotate(90deg)' } : {}),
                }}
                shape={'caret right'}
                size={18}
                onClick={() => this.setState({ paramListOpen: !paramListOpen })}
              />
            </div>
          )}
          <div
            style={{
              ...styles.parameterList,
              maxHeight: paramListOpen ? '22.5rem' : 0,
            }}
          >
            {searchParameters.slice(0, 5).map((param, p) => (
              <SearchGroupItemParameter key={p} parameter={param} />
            ))}
            {searchParameters.length > 5 && (
              <span
                style={status === 'active' ? styles.viewMore : styles.disabled}
                onClick={() => this.launchSearch()}
              >
                View/edit all criteria ({searchParameters.length - 5} more)
              </span>
            )}
            {!!modifiers && modifiers.length > 0 && (
              <React.Fragment>
                <h3 style={{ fontSize: '14px', marginTop: '0.375rem' }}>
                  Modifiers
                </h3>
                {modifiers.map((mod, m) => (
                  <div key={m} style={styles.parameter}>
                    {this.modifierDisplay(mod)}
                  </div>
                ))}
              </React.Fragment>
            )}
          </div>
          {renaming && (
            <RenameModal
              existingNames={this.existingItemNames}
              oldName={name || 'this item'}
              hideDescription={true}
              onCancel={() => this.setState({ renaming: false })}
              onRename={(v) => this.rename(v)}
              resourceType={ResourceType.COHORTSEARCHITEM}
            />
          )}
          {showConceptSetModal && (
            <ConceptAddModal
              activeDomainTab={{
                domain: type,
                name: domainToTitle(type),
                count: 0,
              }}
              selectedConcepts={searchParameters}
              onSave={() =>
                this.setState({
                  showConceptSetModal: false,
                  showConceptSuccessModal: true,
                })
              }
              onClose={() => this.setState({ showConceptSetModal: false })}
            />
          )}
          {showConceptSuccessModal && (
            <Modal>
              <ModalTitle>Concepts successfully saved</ModalTitle>
              <ModalFooter>
                <Button
                  onClick={() =>
                    this.setState({ showConceptSuccessModal: false })
                  }
                >
                  OK
                </Button>
              </ModalFooter>
            </Modal>
          )}
        </React.Fragment>
      );
    }
  }
);
