import * as React from 'react';

import {PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {ppiQuestions} from 'app/cohort-search/search-state.service';
import {domainToTitle, subTypeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {
  attributesSelectionStore,
  currentCohortCriteriaStore,
  currentConceptStore,
  currentWorkspaceStore,
  setSidebarActiveIconStore
} from 'app/utils/navigation';
import {AttrName, Criteria, CriteriaSubType, CriteriaType, Domain, Operator} from 'generated/fetch';

const COPE_SURVEY_ID = 1333342;
export const COPE_SURVEY_GROUP_NAME = 'COVID-19 Participant Experience (COPE) Survey';
const styles = reactStyles({
  code: {
    color: colors.dark,
    fontWeight: 'bold',
    marginRight: '0.25rem',
    whiteSpace: 'nowrap'
  },
  count: {
    alignItems: 'center',
    background: colors.accent,
    borderRadius: '10px',
    color: colors.white,
    display: 'inline-flex',
    fontSize: '10px',
    height: '0.625rem',
    justifyContent: 'center',
    lineHeight: 'normal',
    margin: '0 0.25rem',
    minWidth: '0.675rem',
    padding: '0 4px',
    verticalAlign: 'middle'
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.5)}`,
    borderRadius: '5px',
    marginTop: '0.25rem',
    padding: '8px',
  },
  iconButton: {
    background: 'transparent',
    border: 0,
    cursor: 'pointer',
    flex: '0 0 1.25rem',
    height: '1.25rem',
    lineHeight: '1rem',
    padding: 0,
    width: '1.25rem',
  },
  name: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  searchMatch: {
    color: colors.select,
    fontWeight: 'bolder',
    backgroundColor: colorWithWhiteness(colors.select, 0.8),
    padding: '2px 0',
    whiteSpace: 'nowrap'
  },
  selectIcon: {
    color: colors.select,
    margin: '5px'
  },
  disableIcon: {
    opacity: 0.4,
    cursor: 'not-allowed'
  },
  treeNode: {
    alignItems: 'center',
    display: 'flex'
  },
  treeNodeContent: {
    cursor: 'pointer',
    display: 'flex',
    flexFlow: 'row nowrap',
    lineHeight: '1.25rem',
    margin: 0,
    paddingLeft: '0.25rem',
    width: '90%'
  }
});

export interface NodeProp extends Criteria {
  children: Array<NodeProp>;
}

interface TreeNodeProps {
  autocompleteSelection: any;
  expand?: Function;
  groupSelections: Array<number>;
  node: NodeProp;
  source?: string;
  scrollToMatch: Function;
  searchTerms: string;
  select: Function;
  selectedIds: Array<string>;
  setAttributes: Function;
}

interface TreeNodeState {
  children: Array<any>;
  error: boolean;
  expanded: boolean;
  hover: boolean;
  loading: boolean;
  searchMatch: boolean;
  truncated: boolean;
}

export class TreeNode extends React.Component<TreeNodeProps, TreeNodeState> {
  name: HTMLDivElement;
  constructor(props) {
    super(props);
    this.state = {
      children: undefined,
      error: false,
      expanded: false,
      hover: false,
      loading: false,
      searchMatch: false,
      truncated: false
    };
  }

  componentDidMount(): void {
    if (!!this.props.autocompleteSelection) {
      this.checkAutocomplete();
    }
    if (!!this.name) {
      const {offsetWidth, scrollWidth} = this.name;
      this.setState({truncated: scrollWidth > offsetWidth});
    }
  }

  componentDidUpdate(prevProps: Readonly<TreeNodeProps>): void {
    const {autocompleteSelection, node: {domainId, group}, searchTerms} = this.props;
    if (domainId === Domain.PHYSICALMEASUREMENT.toString() && group && searchTerms !== prevProps.searchTerms) {
      this.searchChildren();
    }
    if (!!autocompleteSelection && autocompleteSelection !== prevProps.autocompleteSelection) {
      this.checkAutocomplete();
    }
  }

  loadChildren() {
    const {node: {conceptId, count, domainId, id, isStandard, name, type}} = this.props;
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === Domain.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, id)
      .then(resp => {
        if (resp.items.length === 0 && domainId === Domain.DRUG.toString()) {
          cohortBuilderApi()
            .findCriteriaBy(+cdrVersionId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
            .then(rxResp => {
              this.setState({children: rxResp.items, loading: false});
            }, () => this.setState({error: true}));
        } else {
          this.setState({children: resp.items, loading: false});
          if (resp.items.length > 0 && domainId === Domain.SURVEY.toString() && !resp.items[0].group) {
            // save questions in the store so we can display them along with answers if selected
            const questions = ppiQuestions.getValue();
            questions[id] = {conceptId, count, name};
            ppiQuestions.next(questions);
          }
        }
      })
      .catch(error => {
        console.error(error);
        this.setState({error: true, loading: false});
      });
  }

  searchChildren() {
    const {expand, node: {children, parentId}, searchTerms} = this.props;
    if (!!searchTerms && children.some(child => child.name.toLowerCase().includes(searchTerms.toLowerCase()))) {
      if (parentId !== 0) {
        setTimeout(() => expand());
      }
      this.setState({expanded: true});
    } else {
      this.setState({expanded: false});
    }
  }

  checkAutocomplete() {
    const {autocompleteSelection, node: {id}, scrollToMatch} = this.props;
    const subtree = autocompleteSelection.path.split('.');
    const expanded = subtree.includes(id.toString());
    const searchMatch = subtree[subtree.length - 1] === id.toString();
    if (expanded && !this.state.children) {
      this.loadChildren();
    }
    if (searchMatch) {
      scrollToMatch(id);
    }
    this.setState({expanded, searchMatch});
  }

  toggleExpanded() {
    const {node: {domainId, group, name, parentId, subtype}, source} = this.props;
    if (group) {
      const {children, expanded} = this.state;
      if (!expanded) {
        if (parentId === 0) {
          const labelName = domainId === Domain.SURVEY.toString() ? name : subTypeToTitle(subtype);
          const message = source === 'concept' ? 'Concept Search' : 'Cohort Builder Search';
          triggerEvent(message, 'Click', `${domainToTitle(domainId)} - ${labelName} - Expand`);
        }
        if (domainId !== Domain.PHYSICALMEASUREMENT.toString() && !children) {
          this.loadChildren();
        }
      }
      this.setState({expanded: !expanded});
    }
  }

  paramId(crit?: Criteria) {
    const node = crit || this.props.node;
    const {code, conceptId, domainId, id, isStandard} = node;
    return `param${!!conceptId && domainId !== Domain.SURVEY.toString() ? (conceptId + code + isStandard) : id}`;
  }

  get isPMCat() {
    return [CriteriaSubType.WHEEL, CriteriaSubType.PREG, CriteriaSubType.HRIRR, CriteriaSubType.HRNOIRR]
      .map(st => st.toString())
      .includes(this.props.node.subtype);
  }

  get showCount() {
    const {node: {code, count, group, selectable, subtype, type}} = this.props;
    return count > -1 &&
      (selectable || (subtype === CriteriaSubType.LAB.toString() && group && code !== null) || type === CriteriaType.CPT4.toString());
  }

  select(event: Event) {
    event.stopPropagation();
    const {node, node: {conceptId, domainId, group, parentId, subtype, value}, select, selectedIds} = this.props;
    let {node: {name}} = this.props;
    if (!selectedIds.includes(this.paramId())) {
      let attributes = [];
      if (subtype === CriteriaSubType.BP.toString()) {
        Object.keys(PREDEFINED_ATTRIBUTES).forEach(key => {
          if (name.indexOf(key) === 0) {
            attributes = PREDEFINED_ATTRIBUTES[key];
          }
        });
      } else if (this.isPMCat) {
        attributes.push({
          name: AttrName.CAT,
          operator: Operator.IN,
          operands: [value]
        });
      } else if (domainId === Domain.SURVEY.toString() && !group) {
        const question = ppiQuestions.getValue()[parentId];
        if (question) {
          name = `${question.name} - ${name}`;
        }
        const attribute = conceptId === 1585747
          ? {name: AttrName.NUM, operator: Operator.EQUAL, operands: [value]}
          : {name: AttrName.CAT, operator: Operator.IN, operands: [value]};
        attributes.push(attribute);
      }
      const param = {
        ...node as Object,
        parameterId: this.paramId(),
        attributes,
        name
      };
      select(param);
    }
  }

  setAttributes(event: Event, node: NodeProp) {
    event.stopPropagation();
    delete node.children;
    attributesSelectionStore.next(node);
    setSidebarActiveIconStore.next('criteria');
  }

  get isCOPESurvey() {
    const {node: {conceptId, name, subtype}} = this.props;
    return subtype === CriteriaSubType.SURVEY.toString() && conceptId === COPE_SURVEY_ID &&
       name === COPE_SURVEY_GROUP_NAME;
  }

  get showCode() {
    const {node: {code, domainId, name}} = this.props;
    return domainId !== Domain.SURVEY.toString() && !!code && code !== name;
  }

  get nodeIsSelected() {
    const {node: {domainId, path}, source} = this.props;
    return source === 'cohort'
      ? currentCohortCriteriaStore.getValue().some(crit =>
        crit.parameterId === this.paramId()
          || (![Domain.PHYSICALMEASUREMENT.toString(), Domain.VISIT.toString()].includes(domainId)
          && !!crit.id && path.split('.').includes(crit.id.toString()))
      )
      : currentConceptStore.getValue().some(crit =>
        this.paramId(crit) === this.paramId() || path.split('.').includes(crit.id.toString()));
  }

  selectIconDisabled() {
    const {selectedIds, source} = this.props;
    return source !== 'cohort' && selectedIds && selectedIds.length >= 1000;
  }

  render() {
    const {autocompleteSelection, groupSelections, node, node: {code, count, domainId, id, group, hasAttributes, name, selectable},
      source, scrollToMatch, searchTerms, select, selectedIds, setAttributes} = this.props;
    const {children, error, expanded, hover, loading, searchMatch} = this.state;
    const nodeChildren = domainId === Domain.PHYSICALMEASUREMENT.toString() ? node.children : children;
    const displayName = domainId === Domain.PHYSICALMEASUREMENT.toString() && !!searchTerms
      ? highlightSearchTerm(searchTerms, name, colors.success)
      : name;
    const selectIconStyle = this.selectIconDisabled() ? {...styles.selectIcon, ...styles.disableIcon} : styles.selectIcon;

    return <React.Fragment>
      <div style={{...styles.treeNode}} id={`node${id}`} onClick={() => this.toggleExpanded()}>
        {group && <button style={styles.iconButton}>
          {loading
            ? <Spinner size={16}/>
            : <ClrIcon style={{color: colors.disabled}}
              shape={'angle ' + (expanded ? 'down' : 'right')}
              size='16'/>}
        </button>}
        <div style={hover ? {...styles.treeNodeContent, background: colors.light} : styles.treeNodeContent}
          onMouseEnter={() => this.setState({hover: true})}
          onMouseLeave={() => this.setState({hover: false})}>
          {(selectable && (source === 'cohort' || node.subtype !== 'ANSWER')) && <button style={styles.iconButton}>
            {hasAttributes && source === 'cohort'
              ? <ClrIcon style={{color: colors.accent}}
                  shape='slider' dir='right' size='20'
                  onClick={(e) => this.setAttributes(e, node)}/>
              : this.nodeIsSelected
                ? <ClrIcon style={{...styles.selectIcon, ...styles.disableIcon}}
                    shape='check-circle' size='20'/>
                : <ClrIcon style={selectIconStyle}
                    shape='plus-circle' size='20'
                    onClick={(e) => this.select(e)}/>
            }
          </button>}
          {this.showCode && <div style={styles.code}>{code}</div>}
          <TooltipTrigger content={<div>{displayName}</div>} disabled={!this.state.truncated}>
            <div style={styles.name} ref={(e) => this.name = e}>
              <span data-test-id='displayName' style={searchMatch ? styles.searchMatch : {}}>{displayName}
              {this.isCOPESurvey && <span style={{paddingRight: '0.1rem'}}> - <i> Versioned</i> </span>}
              </span>
            </div>
          </TooltipTrigger>
          {this.showCount && <div style={{whiteSpace: 'nowrap'}}>
            <span style={styles.count}>{count.toLocaleString()}</span>
          </div>}
        </div>
      </div>
      {!!nodeChildren && nodeChildren.length > 0 &&
        <div style={{display: expanded ? 'block' : 'none', marginLeft: nodeChildren[0].group ? '0.875rem' : '2rem'}}>
          {nodeChildren.map((child, c) => <TreeNode key={c}
                                                      autocompleteSelection={autocompleteSelection}
                                                      expand={() => this.setState({expanded: true})}
                                                      groupSelections={groupSelections}
                                                      node={child}
                                                      source={source}
                                                      scrollToMatch={scrollToMatch}
                                                      searchTerms={searchTerms}
                                                      select={(s) => select(s)}
                                                      selectedIds={selectedIds}
                                                      setAttributes={setAttributes}/>)
          }
        </div>
      }
      {error && <div style={styles.error}>
        <ClrIcon style={{color: colors.white}} className='is-solid' shape='exclamation-triangle' />
        Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
      </div>}
    </React.Fragment>;
  }
}
