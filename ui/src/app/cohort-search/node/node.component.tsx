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
  icon: {},
  iconContainer: {
    cursor: 'pointer',
    display: 'inline-block',
    flex: '0 0 1.25rem',
    height: '1.25rem',
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
  treeContainer: {
    margin: '3rem 0 1rem',
    width: '99%',
  },
  treeHeader: {
    overflow: 'auto',
    background: colorWithWhiteness(colors.black, 0.97),
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
  }
});

const iconStyles = reactStyles({
  attributesIcon: {
    ...styles.icon,
    color: colors.accent
  },
  caretIcon: {
    ...styles.icon,
    color: colors.disabled
  },
  selectIcon: {
    ...styles.icon,
    color: colors.success
  },
  selectedIcon: {
    ...styles.icon,
    color: colors.success,
    cursor: 'not-allowed',
    opacity: 0.4
  },
});

interface TreeNodeProps {
  node: Criteria;
}

interface TreeNodeState {
  children: Array<any>;
  empty: boolean;
  error: boolean;
  expanded: boolean;
  loading: boolean;
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
      selected: false
    };
  }

  componentDidMount(): void {
    const {node} = this.props;
    const {children, error} = this.state;
    this.subscription = subtreePathStore.subscribe(path => {
      const expanded = path.includes(node.id.toString());
      this.setState({expanded});
      if (expanded && !children) {
        this.loadChildren();
      }
    });

    this.subscription.add(selectionsStore.subscribe(selections => {
      this.setState({selected: selections.includes(this.paramId)});
    }));

    this.subscription.add(subtreeSelectedStore.subscribe(id => {
      const selected = id === node.id;
      // this.setState({selected});
      if (selected) {
        setTimeout(() => scrollStore.next(node.id));
      }
      if (error && id !== undefined) {
        subtreeSelectedStore.next(undefined);
      }
    }));
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
    if (this.props.node.group) {
      const {children, expanded} = this.state;
      if (!expanded && !children) {
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
    const {node, node: {id, group, hasAttributes, name, selectable}} = this.props;
    const {children, expanded, loading, selected} = this.state;
    return <React.Fragment>
      <div id={`node${id}`} onClick={() => this.toggleExpanded()}>
        {group && <div style={styles.iconContainer}>
          {loading
            ? <Spinner size={16}/>
            : <ClrIcon style={iconStyles.caretIcon} shape={'angle ' + (expanded ? 'down' : 'right')}
              size='16' onClick={() => this.toggleExpanded()}/>}
        </div>}
        {selectable && <div style={styles.iconContainer}>
          {hasAttributes
            ? <ClrIcon style={iconStyles.attributesIcon} shape='slider' dir='right' size='20' onClick={() => attributesStore.next(node)}/>
            : <React.Fragment>
              {!selected && <ClrIcon style={iconStyles.selectIcon} shape='plus-circle' size='20' onClick={() => this.select()}/>}
              {selected && <ClrIcon style={iconStyles.selectedIcon} shape='check-circle' size='20'/>}
            </React.Fragment>
          }
        </div>}
        <div className='clr-treenode-link container'>{name}</div>
      </div>
      {expanded && !!children && <div style={{marginLeft: '1rem'}} className='node-list'>
        {children.map((child, c) => <TreeNode key={c} node={child}/>)}
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
    const {children} = this.state;
    this.loadRootNodes();
    this.subscription = autocompleteStore.subscribe(searchTerms => {
      this.setState({searchTerms});
      if (wizard.fullTree && children) {
        const filteredChildren = this.filterTree(JSON.parse(JSON.stringify(children)), () => {});
        this.setState({children: filteredChildren});
      }
    });
  }

  componentWillUnmount(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  loadRootNodes() {
    const {node: {domainId, id, isStandard, type}} = this.props;
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (domainId === DomainType.SURVEY.toString()) {
      this.trackEvent();
    }
    this.setState({loading: true});
    const {cdrVersionId} = (currentWorkspaceStore.getValue());
    const criteriaType = domainId === DomainType.DRUG.toString() ? CriteriaType.ATC.toString() : type;
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, domainId, criteriaType, isStandard, id)
      .then(resp => this.setState({children: resp.items, loading: false}))
      .catch(error => {
        console.error(error);
        this.setState({error: true, loading: false});
        subtreeSelectedStore.next(undefined);
      });
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
    const {node: {domainId, name, parentId, subtype}} = this.props;
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

  get showSearch() {
    return this.props.node.domainId !== DomainType.VISIT.toString();
  }

  get showHeader() {
    const {node: {domainId}} = this.props;
    return domainId !== DomainType.PHYSICALMEASUREMENT.toString()
      && domainId !== DomainType.SURVEY.toString()
      && domainId !== DomainType.VISIT.toString();
  }

  get isEmpty() {
    const {empty, error, loading} = this.state;
    return !loading && (empty || error);
  }

  render() {
    const {back, node} = this.props;
    const {children, ingredients, loading} = this.state;
    return <React.Fragment>
      <div style={styles.searchBarContainer}>
        <SearchBar node={node} setIngredients={(i) => this.setState({ingredients: i})}/>
      </div>
      <div style={this.showHeader
        ? {...styles.treeContainer, border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`}
        : styles.treeContainer}>
        {this.showHeader && <div style={styles.treeHeader}>
          {!!ingredients && <div style={styles.ingredients}>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button style={styles.returnLink} onClick={() => back()}>Return to list</button>
        </div>}
        {this.isEmpty && <div className='alert alert-warning'>
          <ClrIcon className='alert-icon is-solid' shape='exclamation-triangle' />
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        {!loading && !!children && children.map((child, c) => <TreeNode key={c} node={child}/>)}
    </div>
      {loading && <SpinnerOverlay/>}
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
