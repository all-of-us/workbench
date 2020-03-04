import {Component, Input} from '@angular/core';
import * as React from 'react';

import {autocompleteStore, ppiQuestions, scrollStore, selectionsStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, highlightMatches, stripHtml, subTypeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType} from 'generated/fetch';
import {Subscription} from 'rxjs/Subscription';

interface TreeNodeProps {
  node: any;
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
    this.subscription = selectionsStore.subscribe(selections => {
      this.setState({selected: selections.includes(this.paramId)});
    });
  }

  loadChildren() {
    const {node: {count, domainId, id, isStandard, name, type}} = this.props;
    const {empty} = this.state;
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (domainId === DomainType.SURVEY) {
      this.trackEvent();
    }
    this.setState({loading: true});
    const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const criteriaType = domainId === DomainType.DRUG ? CriteriaType[CriteriaType.ATC] : type;
    try {
      cohortBuilderApi().findCriteriaBy(cdrId, domainId, criteriaType, isStandard, id)
        .then(resp => {
          if (resp.items.length === 0 && domainId === DomainType.DRUG) {
            cohortBuilderApi()
              .findCriteriaBy(cdrId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
              .then(rxResp => {
                this.setState({empty: rxResp.items.length === 0, children: rxResp.items, loading: false});
              }, () => this.setState({error: true}));
          } else {
            this.setState({empty: resp.items.length === 0, children: resp.items, loading: false});
            if (!empty && domainId === DomainType.SURVEY && !resp.items[0].group) {
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
      const formattedName = domainId === DomainType.SURVEY ? name : subTypeToTitle(subtype);
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `${domainToTitle(domainId)} - ${formattedName} - Expand`
      );
    }
  }

  toggleExpanded() {
    const {expanded} = this.state;
    this.setState({expanded: !expanded});
  }

  get paramId() {
    const {node: {code, conceptId, domainId, id}} = this.props;
    return `param${conceptId && !(domainId === DomainType.SURVEY) ? (conceptId + code) : id}`;
  }

  select() {
    // select node
  }

  render() {
    const {node} = this.props;
    const {children, expanded, loading} = this.state;
    return <React.Fragment>
      <div id={`node${node.id}`} className='clr-treenode-link container' onClick={() => this.toggleExpanded()}>
        {/*  node-info */}
        {loading && <Spinner size={16}/>}
        {node.group && <ClrIcon shape={'angle ' + (expanded ? 'down' : 'right')} size='20' onClick={() => this.loadChildren()}/>}
        {node.selectable && <ClrIcon shape='plus-circle' size='20' onClick={() => this.select()}/>}
      </div>
      {node.group && <div className='node-list'>
        {node.children.map((child, c) => <TreeNode key={c} node={child}/>)}
      </div>}
    </React.Fragment>;
  }
}

interface Props {
  back: Function;
  node: any;
  selections: Array<string>;
  wizard: any;
}

interface State {
  children: any;
  empty: boolean;
  error: boolean;
  expanded: boolean;
  ingredients: any;
  loading: boolean;
  searchTerms: string;
  selected: boolean;
}

export const CriteriaTree = withCurrentWorkspace()(class extends React.Component<Props, State> {
  subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      children: undefined,
      empty: false,
      error: false,
      expanded: false,
      ingredients: undefined,
      loading: false,
      searchTerms: undefined,
      selected: false
    };
  }

  componentDidMount(): void {
    const {node, wizard} = this.props;
    const {children, error} = this.state;
    if (!wizard.fullTree || node.id === 0) {
      this.subscription = subtreePathStore.subscribe(path => {
        this.setState({expanded: path.includes(node.id.toString())});
      });

      this.subscription.add(subtreeSelectedStore.subscribe(id => {
        const selected = id === node.id;
        this.setState({selected});
        if (selected) {
          setTimeout(() => scrollStore.next(node.id));
        }
        if (error && id !== undefined) {
          subtreeSelectedStore.next(undefined);
        }
      }));

      this.subscription.add(autocompleteStore.subscribe(searchTerms => {
        this.setState({searchTerms});
        if (wizard.fullTree && children) {
          const filteredChildren = this.filterTree(JSON.parse(JSON.stringify(children)), () => {});
          this.setState({children: filteredChildren});
        }
      }));
    }
    if (wizard && wizard.fullTree) {
      this.setState({expanded: node.expanded || false});
    }
  }

  componentWillUnmount(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.state.selected) {
      subtreeSelectedStore.next(undefined);
      scrollStore.next(undefined);
    }
  }

  loadChildren(event) {
    const {node: {count, domainId, id, isStandard, name, type}, wizard} = this.props;
    const {children, empty} = this.state;
    // TODO remove condition to only track SURVEY domain for 'Phase 2' of CB Google Analytics
    if (domainId === DomainType.SURVEY) {
      this.trackEvent();
    }
    if (!event || (id !== 0 && !!children)) { return; }
    this.setState({loading: true});
    const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
    const criteriaType = domainId === DomainType.DRUG ? CriteriaType[CriteriaType.ATC] : type;
    try {
      cohortBuilderApi().findCriteriaBy(cdrId, domainId, criteriaType, isStandard, id)
        .then(resp => {
          if (resp.items.length === 0 && domainId === DomainType.DRUG) {
            cohortBuilderApi()
              .findCriteriaBy(cdrId, domainId, CriteriaType[CriteriaType.RXNORM], isStandard, id)
              .then(rxResp => {
                this.setState({empty: rxResp.items.length === 0, children: rxResp.items, loading: false});
              }, () => this.setState({error: true}));
          } else {
            this.setState({empty: resp.items.length === 0, children: resp.items, loading: false});
            if (!empty && domainId === DomainType.SURVEY && !resp.items[0].group) {
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
    if (parentId === 0 && this.state.expanded) {
      const formattedName = domainId === DomainType.SURVEY ? name : subTypeToTitle(subtype);
      triggerEvent(
        'Cohort Builder Search',
        'Click',
        `${domainToTitle(domainId)} - ${formattedName} - Expand`
      );
    }
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
    const {node: {domainId}} = this.props;
    return domainId !== DomainType[DomainType.PERSON] && domainId !== DomainType.VISIT;
  }

  get showHeader() {
    const {node: {domainId}} = this.props;
    return domainId !== DomainType.PHYSICALMEASUREMENT
      && domainId !== DomainType.SURVEY
      && domainId !== DomainType.VISIT;
  }

  get isEmpty() {
    const {empty, error, loading} = this.state;
    return !loading && (empty || error);
  }

  render() {
    const {back} = this.props;
    const {children, ingredients} = this.state;
    return <React.Fragment>
      <div className='dropdown-search-container'>
        <div className='search-container'>
          <input />
        </div>
      </div>
      <div className='tree-container'>
        {this.showHeader && <div className='tree-header'>
          {!!ingredients && <div className='ingredients'>
            Ingredients in this brand: {ingredients.join(', ')}
          </div>}
          <button className='btn btn-link' onClick={() => back()}>Return to list</button>
        </div>}
        {this.isEmpty && <div className='alert alert-warning'>
          <ClrIcon className='alert-icon is-solid' shape='exclamation-triangle' />
          Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.
        </div>}
        {children.map((child, c) => <TreeNode key={c} node={child}/>)}
    </div>
    <div className='spinner root-spinner'>
      Loading...
    </div>
    </React.Fragment>;
  }
});

@Component({
  selector: 'crit-tree',
  template: '<div #root></div>'
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
