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
import {currentWorkspaceStore} from 'app/utils/navigation';
import {AttrName, Criteria, CriteriaSubType, CriteriaType, DomainType, Operator} from 'generated/fetch';

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
  selected: {
    cursor: 'not-allowed',
    opacity: 0.4
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
    const {offsetWidth, scrollWidth} = this.name;
    this.setState({truncated: scrollWidth > offsetWidth});
  }

  componentDidUpdate(prevProps: Readonly<TreeNodeProps>): void {
    const {autocompleteSelection, node: {domainId, group}, searchTerms} = this.props;
    if (domainId === DomainType.PHYSICALMEASUREMENT.toString() && group && searchTerms !== prevProps.searchTerms) {
      this.searchChildren();
    }
    if (!!autocompleteSelection && autocompleteSelection !== prevProps.autocompleteSelection) {
      this.checkAutocomplete();
    }
  }

  loadChildren() {
    const {node: {count, domainId, id, isStandard, name, type}} = this.props;
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === DomainType.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, id)
      .then(resp => {
        if (resp.items.length === 0 && domainId === DomainType.DRUG.toString()) {
          cohortBuilderApi()
            .findCriteriaBy(+cdrVersionId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
            .then(rxResp => {
              this.setState({children: rxResp.items, loading: false});
            }, () => this.setState({error: true}));
        } else {
          this.setState({children: resp.items, loading: false});
          if (resp.items.length > 0 && domainId === DomainType.SURVEY.toString() && !resp.items[0].group) {
            // save questions in the store so we can display them along with answers if selected
            const questions = ppiQuestions.getValue();
            questions[id] = {count, name};
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
    const {node: {domainId, group, name, parentId, subtype}} = this.props;
    if (group) {
      const {children, expanded} = this.state;
      if (!expanded) {
        if (parentId === 0) {
          const labelName = domainId === DomainType.SURVEY.toString() ? name : subTypeToTitle(subtype);
          triggerEvent('Cohort Builder Search', 'Click', `${domainToTitle(domainId)} - ${labelName} - Expand`);
        }
        if (domainId !== DomainType.PHYSICALMEASUREMENT.toString() && !children) {
          this.loadChildren();
        }
      }
      this.setState({expanded: !expanded});
    }
  }

  get paramId() {
    const {node: {code, conceptId, domainId, id}} = this.props;
    return `param${!!conceptId && domainId !== DomainType.SURVEY.toString() ? (conceptId + code) : id}`;
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
    if (!selectedIds.includes(this.paramId)) {
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
      } else if (domainId === DomainType.SURVEY.toString() && !group) {
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
        parameterId: this.paramId,
        attributes,
        name
      };
      select(param);
    }
  }

  render() {
    const {autocompleteSelection, groupSelections, node,
      node: {code, count, domainId, id, group, hasAttributes, name, parentId, selectable}, scrollToMatch, searchTerms, select, selectedIds,
      setAttributes} = this.props;
    const {children, error, expanded, hover, loading, searchMatch} = this.state;
    const nodeChildren = domainId === DomainType.PHYSICALMEASUREMENT.toString() ? node.children : children;
    const selected = selectedIds.includes(this.paramId) || groupSelections.includes(parentId);
    const displayName = domainId === DomainType.PHYSICALMEASUREMENT.toString() && !!searchTerms
      ? highlightSearchTerm(searchTerms, name, colors.success)
      : name;
    return <React.Fragment>
      <div style={{...styles.treeNode}} id={`node${id}`} onClick={() => this.toggleExpanded()}>
        {group && <button style={styles.iconButton}>
          {loading
            ? <Spinner size={16}/>
            : <ClrIcon style={{color: colors.disabled}}
              shape={'angle ' + (expanded ? 'down' : 'right')}
              size='16' onClick={() => this.toggleExpanded()}/>}
        </button>}
        <div style={hover ? {...styles.treeNodeContent, background: colors.light} : styles.treeNodeContent}
          onMouseEnter={() => this.setState({hover: true})}
          onMouseLeave={() => this.setState({hover: false})}>
          {selectable && <button style={styles.iconButton}>
            {hasAttributes
              ? <ClrIcon style={{color: colors.accent}}
                  shape='slider' dir='right' size='20'
                  onClick={() => setAttributes(node)}/>
              : selected
                ? <ClrIcon style={{...styles.selectIcon, ...styles.selected}}
                    shape='check-circle' size='20'/>
                : <ClrIcon style={styles.selectIcon}
                    shape='plus-circle' size='20'
                    onClick={(e) => this.select(e)}/>
            }
          </button>}
          <div style={styles.code}>{code}</div>
          <TooltipTrigger content={<div>{displayName}</div>} disabled={!this.state.truncated}>
            <div style={styles.name} ref={(e) => this.name = e}>
              <span style={searchMatch ? styles.searchMatch : {}}>{displayName}</span>
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
