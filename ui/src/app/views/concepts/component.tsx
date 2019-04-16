import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';



import {AlertDanger} from 'app/components/alert';
import {Clickable} from 'app/components/buttons';
import {WorkspaceCardBase} from 'app/components/card';
import {ClrIcon} from 'app/components/icons';
import {CheckBox, TextInput} from 'app/components/inputs';
import {SpinnerOverlay} from 'app/components/spinners';
import {conceptsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {
  Concept,
  Domain,
  DomainCount,
  DomainInfo,
  VocabularyCount,
} from 'generated/fetch';

const styles = reactStyles({
  searchBar: {
    marginLeft: '1%',
    boxShadow: '0 4px 12px 0 rgba(0,0,0,0.15)',
    height: '3rem',
    width: '64.3%',
    backgroundColor: '#A3D3F232',
    fontSize: '16px',
    lineHeight: '19px',
    paddingLeft: '2rem'
  },
  domainBoxHeader: {
    color: '#2691D0',
    fontSize: '18px',
    lineHeight: '22px'
  },
  conceptText: {
    marginTop: '0.3rem',
    fontSize: '14px',
    fontWeight: 400,
    color: '#4A4A4A',
    display: 'flex',
    flexDirection: 'column'
  }
});

const DomainBox: React.FunctionComponent<{conceptDomainInfo: DomainInfo,
  standardConceptsOnly: boolean}> =
    ({conceptDomainInfo, standardConceptsOnly}) => {
      const conceptCount = standardConceptsOnly ?
          conceptDomainInfo.standardConceptCount : conceptDomainInfo.allConceptCount;
      return <WorkspaceCardBase style={{minWidth: '11rem'}}>
        <div style={styles.domainBoxHeader}>{conceptDomainInfo.name}</div>
        <div style={styles.conceptText}>
          <span style={{fontSize: '30px'}}>{conceptCount}</span> concepts in this domain. <p/>
          <b>{conceptDomainInfo.participantCount}</b> participants in domain.</div>
        <Clickable>Browse Domain</Clickable>
      </WorkspaceCardBase>;
    };


export const ConceptWrapper = withCurrentWorkspace()(
  class extends React.Component<{workspace: WorkspaceData},
      {loadingDomains: boolean, currentSearchString: string, standardConceptsOnly: boolean,
        searching: boolean, showSearchError: boolean, selectedDomain: DomainCount,
        conceptDomainList: Array<DomainInfo>, conceptDomainCounts: Array<DomainCount>,
        concepts: Array<ConceptInfo>, conceptsCache: Array<ConceptCacheSet>,
        selectedConceptDomainMap: Map<String, number>}> {

    constructor(props) {
      super(props);
      this.state = {
        loadingDomains: true,
        currentSearchString: '',
        standardConceptsOnly: true,
        searching: false,
        showSearchError: false,
        selectedDomain: {
          name: '',
          domain: undefined,
          conceptCount: 0
        },
        conceptDomainList: [],
        conceptDomainCounts: [],
        concepts: [],
        conceptsCache: [],
        selectedConceptDomainMap: new Map<string, number>()
      };
    }

    componentDidMount() {
      this.loadDomains();
    }

    async loadDomains() {
      const {namespace, id} = this.props.workspace;
      try {
        const conceptsCache: ConceptCacheSet[] = [];
        const conceptDomainCounts: DomainCount[] = [];
        const resp = await conceptsApi().getDomainInfo(namespace, id);
        this.setState({conceptDomainList: resp.items});
        resp.items.forEach((domain) => {
          conceptsCache.push({
            domain: domain.domain,
            items: [],
            vocabularyList: []
          });
          conceptDomainCounts.push({
            domain: domain.domain,
            name: domain.name,
            conceptCount: 0
          });
        });
        this.setState({
          conceptsCache: conceptsCache,
          conceptDomainCounts: conceptDomainCounts,
          selectedDomain: conceptDomainCounts[0],
          loadingDomains: false});

        console.log(this.state);
      } catch (e) {
        console.error(e);
      }
    }

    searchButton(e) {
      // search on enter key
      if (e.keyCode === 13) {
        const searchTermLength = e.target.value.trim().length;
        if (searchTermLength < 3) {
          this.setState({showSearchError: true});
        } else {
          this.setState({currentSearchString: e.target.value});
          this.searchConcepts();
        }
      }
    }

    selectDomain(domainCount: DomainCount) {
      if (!this.state.selectedConceptDomainMap[domainCount.domain]) {
        this.setState(fp.update(['selectedConceptDomainMap', domainCount.domain], fp.pull(0)));
      }
      this.setState({selectedDomain: domainCount});
      this.setConceptsAndVocabularies();
    }

    setConceptsAndVocabularies() {
      // TODO
    }

    searchConcepts() {
      // TODO
    }

    filterList() {
      // TODO
    }

    selectConcept(concepts: ConceptInfo[]) {
      // TODO
    }

    clearSearch() {
      this.setState({currentSearchString: ''});
      this.searchConcepts();
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      this.setState({currentSearchString: '',
        selectedDomain: conceptDomainCounts
          .find(domainCount => domainCount.domain === domain.domain)});
      this.searchConcepts();
    }

    render() {
      const {loadingDomains, conceptDomainList, standardConceptsOnly, showSearchError} = this.state;
      return <React.Fragment>
        <div style={{display: 'flex', alignItems: 'center', marginTop: '1.5%', marginBottom: '6%'}}>
          <ClrIcon shape='search' style={{position: 'absolute', height: '1rem', width: '1rem',
            fill: '#216FB4', left: 'calc(1rem + 4.5%)'}}/>
          <TextInput style={styles.searchBar}
                     placeholder='Search concepts in domain'
                     onKeyDown={e => {this.searchButton(e); }}/>
          <CheckBox checked={standardConceptsOnly}
                    style={{marginLeft: '0.5rem', height: '16px', width: '16px'}}
                    onChange={() => this.setState({standardConceptsOnly: !standardConceptsOnly})}/>
          <label style={{marginLeft: '0.2rem'}}>
            Standard concepts only
          </label>
        </div>
        {showSearchError && <AlertDanger style={{width: '64.3%', marginLeft: '1%'}}>
          Minimum concept search length is three characters.
        </AlertDanger>}
        {loadingDomains ? <SpinnerOverlay/> :
          (<div style={{display: 'flex', flexDirection: 'row', width: '94.3%'}}>
            {conceptDomainList.map((domain) => {
              return <DomainBox conceptDomainInfo={domain}
                                standardConceptsOnly={standardConceptsOnly}/>;
            })}
          </div>)
        }
      </React.Fragment>;
    }
  }
);


interface ConceptCacheSet {
  domain: Domain;

  vocabularyList: Array<VocabularyCount>;

  items: Array<ConceptInfo>;
}
// interface VocabularyCountSelected extends VocabularyCount {
//   selected: boolean;
// }
interface ConceptInfo extends Concept {
  selected: boolean;
}

@Component({
  template: '<div #root></div>'
})
export class ConceptsComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptWrapper, []);
  }
}

