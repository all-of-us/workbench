import {Component, Input} from '@angular/core';
import * as React from 'react';

import {PREDEFINED_ATTRIBUTES} from 'app/cohort-search/constant';
import {SearchBar} from 'app/cohort-search/search-bar/search-bar.component';
import {
  attributesStore,
  autocompleteStore,
  groupSelectionsStore,
  ppiQuestions,
  scrollStore,
  selectionsStore,
  subtreePathStore,
  subtreeSelectedStore,
  wizardStore
} from 'app/cohort-search/search-state.service';
import {domainToTitle, highlightMatches, stripHtml, subTypeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {AttrName, Criteria, CriteriaSubType, CriteriaType, DomainType, Operator} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

const styles = reactStyles({
  code: {
    color: colors.dark,
    fontWeight: 'bold',
    marginRight: '0.25rem'
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
    marginLeft: '0.25rem',
    minWidth: '0.675rem',
    padding: '0 4px'
  },
  error: {
    background: colors.warning,
    color: colors.white,
    fontSize: '12px',
    fontWeight: 500,
    textAlign: 'left',
    border: '1px solid #ebafa6',
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
    padding: 0,
    width: '1.25rem',
  },
  ingredients: {
    float: 'left',
    fontWeight: 'bold',
    padding: '0.5rem',
  },
  returnLink: {
    background: 'transparent',
    border: 0,
    color: colors.accent,
    cursor: 'pointer',
    float: 'right',
    fontSize: '12px',
    height: '1.5rem',
    margin: '0.25rem 0',
    padding: '0 0.5rem',
  },
  searchBarContainer: {
    position: 'absolute',
    width: '95%',
    marginTop: '-1px',
    display: 'flex',
    padding: '0.4rem 0',
    backgroundColor: colors.white,
    zIndex: 1,
  },
  searchMatch: {
    color: '#659F3D',
    fontWeight: 'bolder',
    backgroundColor: 'rgba(101,159,61,0.2)',
    padding: '2px 0',
  },
  selectIcon: {
    color: colors.select,
    margin: '5px'
  },
  selectedIcon: {
    color: colors.select,
    cursor: 'not-allowed',
    margin: '5px',
    opacity: 0.4
  },
  treeContainer: {
    margin: '3rem 0 1rem',
    width: '99%',
  },
  treeHeader: {
    overflow: 'auto',
    background: colorWithWhiteness(colors.black, 0.97),
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
  },
  treeNode: {
    alignItems: 'center',
    display: 'flex'
  },
  treeNodeContent: {
    cursor: 'pointer',
    margin: 0,
    paddingLeft: '0.25rem',
    width: '90%'
  }
});

interface NodeProp extends Criteria {
  children: Array<NodeProp>;
}

interface TreeNodeProps {
  fullTree: boolean;
  node: NodeProp;
}

interface TreeNodeState {
  children: Array<any>;
  empty: boolean;
  error: boolean;
  expanded: boolean;
  loading: boolean;
  searchMatch: boolean;
  selected: boolean;
}

class TreeNode extends React.Component<TreeNodeProps, TreeNodeState> {
  subscription: Subscription;
  constructor(props) {
    super(props);
    this.state = {
      children: undefined,
      empty: false,
      error: false,
      expanded: false,
      loading: false,
      searchMatch: false,
      selected: false
    };
  }

  componentDidMount(): void {
    const {node} = this.props;
    const {error} = this.state;
    this.subscription = subtreePathStore.subscribe(path => {
      const expanded = path.includes(node.id.toString());
      this.setState({expanded});
      if (expanded && !this.state.children) {
        this.loadChildren();
      }
    });

    this.subscription.add(selectionsStore.subscribe(selections => {
      this.setState({selected: selections.includes(this.paramId)});
    }));

    this.subscription.add(subtreeSelectedStore.subscribe(id => {
      const selected = id === node.id;
      this.setState({searchMatch: selected});
      if (selected) {
        setTimeout(() => scrollStore.next(node.id));
      }
      if (error && id !== undefined) {
        subtreeSelectedStore.next(undefined);
      }
    }));
  }

  componentWillUnmount(): void {
    this.subscription.unsubscribe();
  }

  loadChildren() {
    const {node: {count, domainId, id, isStandard, name, type}} = this.props;
    const {empty} = this.state;
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (domainId === DomainType.SURVEY.toString()) {
      this.trackEvent();
    }
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === DomainType.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    try {
      cohortBuilderApi().findCriteriaBy(cdrId, domainId, criteriaType, isStandard, id)
        .then(resp => {
          if (resp.items.length === 0 && domainId === DomainType.DRUG.toString()) {
            cohortBuilderApi()
              .findCriteriaBy(cdrId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
              .then(rxResp => {
                this.setState({empty: rxResp.items.length === 0, children: rxResp.items, loading: false});
              }, () => this.setState({error: true}));
          } else {
            this.setState({empty: resp.items.length === 0, children: resp.items, loading: false});
            if (!empty && domainId === DomainType.SURVEY.toString() && !resp.items[0].group) {
              // save questions in the store so we can display them along with answers if selected
              const questions = ppiQuestions.getValue();
              questions[id] = {count, name};
              ppiQuestions.next(questions);
            }
          }
        });
    } catch (error) {
      console.error(error);
      this.setState({error: true, loading: false});
      subtreeSelectedStore.next(undefined);
    }
  }

  trackEvent() {
    const {node: {domainId, name, parentId, subtype}} = this.props;
    if (parentId === 0 && this.state.expanded) {
      const formattedName = domainId === DomainType.SURVEY.toString() ? name : subTypeToTitle(subtype);
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `${domainToTitle(domainId)} - ${formattedName} - Expand`
      );
    }
  }

  toggleExpanded() {
    const {fullTree, node: {group}} = this.props;
    if (group) {
      const {children, expanded} = this.state;
      if (!fullTree && !expanded && !children) {
        this.loadChildren();
      }
      this.setState({expanded: !expanded});
    }
  }

  get paramId() {
    const {node: {code, conceptId, domainId, id}} = this.props;
    return `param${conceptId && !(domainId === DomainType.SURVEY.toString()) ? (conceptId + code) : id}`;
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

  select() {
    const {node, node: {conceptId, domainId, group, id, name, parentId, subtype, value}} = this.props;
    let selections = selectionsStore.getValue();
    if (!selections.includes(this.paramId)) {
      if (group) {
        const groups = [...groupSelectionsStore.getValue(), id];
        groupSelectionsStore.next(groups);
      }
      let modifiedName = name;
      if (domainId === DomainType.SURVEY.toString()) {
        // get PPI question from store
        const question = ppiQuestions.getValue()[parentId];
        if (question) {
          modifiedName = question.name + ' - ' + modifiedName;
        }
      }
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
        if (conceptId === 1585747) {
          attributes.push({
            name: AttrName.NUM,
            operator: Operator.EQUAL,
            operands: [value]
          });
        } else {
          attributes.push({
            name: AttrName.CAT,
            operator: Operator.IN,
            operands: [value]
          });
        }
      }
      const param = {
        ...node as Object,
        parameterId: this.paramId,
        attributes: attributes,
        name: modifiedName
      };
      const wizard = wizardStore.getValue();
      wizard.item.searchParameters.push(param);
      selections = [this.paramId, ...selections];
      selectionsStore.next(selections);
      wizardStore.next(wizard);
    }
  }

  render() {
    const {fullTree, node, node: {code, count, id, group, hasAttributes, name, parentId, selectable}} = this.props;
    const {children, expanded, loading, searchMatch, selected} = this.state;
    const nodeChildren = fullTree ? node.children : children;
    return <React.Fragment>
      <div style={{...styles.treeNode}} id={`node${id}`} onClick={() => this.toggleExpanded()}>
        {group && <button style={styles.iconButton}>
          {loading
            ? <Spinner size={16}/>
            : <ClrIcon style={{color: colors.disabled}} shape={'angle ' + (expanded ? 'down' : 'right')}
              size='16' onClick={() => this.toggleExpanded()}/>}
        </button>}
        <div style={styles.treeNodeContent}>
          {selectable && <button style={styles.iconButton}>
            {hasAttributes
              ? <ClrIcon style={{color: colors.accent}}
                  shape='slider' dir='right' size='20'
                  onClick={() => attributesStore.next(node)}/>
              : selected
                ? <ClrIcon style={styles.selectedIcon} shape='check-circle' size='20'/>
                : <ClrIcon style={styles.selectIcon} shape='plus-circle' size='20' onClick={() => this.select()}/>
            }
          </button>}
          <div style={{display: 'inline-block'}}>
            <span style={styles.code}>{code}</span>
            <span style={searchMatch ? styles.searchMatch : {}}>{name}</span>
            {this.showCount && <span style={styles.count}>{count.toLocaleString()}</span>}
          </div>
        </div>
      </div>
      {expanded && !!nodeChildren && nodeChildren.length > 0 && <div style={{marginLeft: nodeChildren[0].group ? '0.875rem' : '2rem'}}>
        {nodeChildren.map((child, c) => <TreeNode key={c} fullTree={fullTree} node={child}/>)}
      </div>}
    </React.Fragment>;
  }
}

interface Props {
  back: Function;
  node: Criteria;
  selections: Array<string>;
  wizard: any;
}

interface State {
  children: any;
  empty: boolean;
  error: boolean;
  ingredients: any;
  loading: boolean;
  searchTerms: string;
}

export const CriteriaTree = withCurrentWorkspace()(class extends React.Component<Props, State> {
  subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      children: undefined,
      empty: false,
      error: false,
      ingredients: undefined,
      loading: true,
      searchTerms: undefined,
    };
  }

  componentDidMount(): void {
    const {wizard} = this.props;
    this.loadRootNodes();
    this.subscription = autocompleteStore.subscribe(searchTerms => {
      this.setState({searchTerms});
      const {children} = this.state;
      if (wizard.fullTree && children) {
        const filteredChildren = this.filterTree(JSON.parse(JSON.stringify(children)), () => {});
        this.setState({children: filteredChildren});
      }
    });
  }

  componentWillUnmount(): void {
    this.subscription.unsubscribe();
  }

  loadRootNodes() {
    const {node: {domainId, id, isStandard, type}, wizard: {fullTree}} = this.props;
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (domainId === DomainType.SURVEY.toString()) {
      this.trackEvent();
    }
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === DomainType.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    const parentId = fullTree ? null : id;
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, parentId)
      .then(resp => {
        if (fullTree) {
          let children = [];
          resp.items.forEach(child => {
            child['children'] = [];
            if (child.parentId === 0) {
              children.push(child);
            } else {
              children = this.addChildToParent(child, children);
            }
          });
          this.setState({children});
        } else {
          this.setState({children: resp.items});
        }
      })
      .catch(error => {
        console.error(error);
        this.setState({error: true});
        subtreeSelectedStore.next(undefined);
      })
      .finally(() => this.setState({loading: false}));
  }

  addChildToParent(child, itemList) {
    for (const item of itemList) {
      if (!item.group) {
        continue;
      }
      if (item.id === child.parentId) {
        item.children.push(child);
        return itemList;
      }
      if (item.children.length) {
        const childList = this.addChildToParent(child, item.children);
        if (childList) {
          item.children = childList;
          return itemList;
        }
      }
    }
  }

  trackEvent() {
    const {node: {domainId, name, subtype}} = this.props;
    const formattedName = domainId === DomainType.SURVEY.toString() ? name : subTypeToTitle(subtype);
    triggerEvent(
      'Cohort Builder Search',
      'Click',
      `${domainToTitle(domainId)} - ${formattedName} - Expand`
    );
  }

  isMatch(name: string) {
    return stripHtml(name).toLowerCase().includes(this.state.searchTerms.toLowerCase());
  }

  filterTree(tree: Array<any>, expand: Function) {
    const {searchTerms} = this.state;
    return tree.map((item) => {
      item.name = stripHtml(item.name);
      if (searchTerms.length > 1) {
        item.expanded = item.children.some(it => this.isMatch(it.name));
      } else {
        item.expanded = false;
      }
      const func = () => {
        item.expanded = true;
        expand();
      };
      if (searchTerms.length > 1 && this.isMatch(item.name)) {
        item.name = highlightMatches([searchTerms], item.name, false);
        expand();
      }
      if (item.children.length) {
        item.children = this.filterTree(item.children, func);
      }
      return item;
    });
  }

  get showHeader() {
    const {node: {domainId}} = this.props;
    return domainId !== DomainType.PHYSICALMEASUREMENT.toString()
      && domainId !== DomainType.SURVEY.toString()
      && domainId !== DomainType.VISIT.toString();
  }

  get hasError() {
    const {empty, error, loading} = this.state;
    return !loading && (empty || error);
  }

  render() {
    const {back, node, wizard: {fullTree}} = this.props;
    const {children, ingredients, loading} = this.state;
    return <React.Fragment>
      {node.domainId !== DomainType.VISIT.toString() && <div style={styles.searchBarContainer}>
        <SearchBar node={node} setIngredients={(i) => this.setState({ingredients: i})}/>
      </div>}
      <div style={this.showHeader
        ? {...styles.treeContainer, border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`}
        : styles.treeContainer}>
        {this.showHeader && <div style={styles.treeHeader}>
          {!!ingredients && <div style={styles.ingredients}>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button style={styles.returnLink} onClick={() => back()}>Return to list</button>
        </div>}
        {this.hasError && <div style={styles.error}>
          <ClrIcon style={{color: colors.white}} className='is-solid' shape='exclamation-triangle' />
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        {!loading && !!children && children.map((child, c) => <TreeNode key={c} fullTree={fullTree} node={child}/>)}
    </div>
      {loading && !this.showHeader && <SpinnerOverlay/>}
    </React.Fragment>;
  }
});

@Component({
  selector: 'crit-tree',
  template: '<div #root style="display: inline"></div>'
})
export class CriteriaTreeComponent extends ReactWrapperBase {
  @Input('back') back: Props['back'];
  @Input('node') node: Props['node'];
  @Input('selections') selections: Props['selections'];
  @Input('wizard') wizard: Props['wizard'];

  constructor() {
    super(CriteriaTree, ['back', 'node', 'selections', 'wizard']);
  }
}
