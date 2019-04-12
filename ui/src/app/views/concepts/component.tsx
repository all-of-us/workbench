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

    clearSearch() {
      this.setState({currentSearchString: ''});
      this.searchConcepts();
    }

    browseDomain(domain: DomainInfo) {
      const {conceptDomainCounts} = this.state;
      this.setState({currentSearchString: '',
        selectedDomain: conceptDomainCounts.find(domainCount => domainCount.domain === domain.domain)});
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
interface VocabularyCountSelected extends VocabularyCount {
  selected: boolean;
}
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

// @Component({
//   template: '<div #root></div>'
// })
//
// export class ConceptsComponent implements OnInit {
//   loadingDomains = true;
//   searchTerm = '';
//   standardConceptsOnly = true;
//   searching = false;
//   currentSearchString = '';
//   searchLoading = false;
//   selectedDomain: DomainCount = {
//     name: '',
//     domain: undefined,
//     conceptCount: 0
//   };
//   showSearchError = false;
//
//   @ViewChild(ConceptTableComponent)
//   conceptTable: ConceptTableComponent;
//
//   @ViewChild(ToolTipComponent)
//   toolTip: ToolTipComponent;
//
//   conceptDomainList: Array<DomainInfo> = [];
//   conceptDomainCounts: Array<DomainCount> = [];
//
//   // Maps Domain Name with number of concept selected
//   selectedConceptDomainMap: Map<String, number>  = new Map<string, number>();
//   concepts: Array<ConceptInfo> = [];
//   conceptsCache: Array<ConceptCacheSet> = [];
//   completedDomainSearches: Array<Domain> = [];
//   placeholderValue = '';
//   vocabularies: Array<VocabularyCountSelected> = [];
//   wsNamespace: string;
//   wsId: string;
//   conceptsToAdd: Concept[] = [];
//
//   // For some reason clr checkboxes trigger click events twice on click. This
//   // is a workaround to not allow multiple filter events to get triggered.
//   blockMultipleSearchFromFilter = true;
//   maxConceptFetch = 100;
//   conceptsSavedText = '';
//
//   conceptAddOpen = false;
//
//   ngOnInit(): void {
//     const {ns, wsid} = urlParamsStore.getValue();
//     this.wsNamespace = ns;
//     this.wsId = wsid;
//     this.loadingDomains = true;
//     this.conceptsService.getDomainInfo(this.wsNamespace, this.wsId).subscribe((response) => {
//       this.conceptDomainList = response.items;
//       this.conceptDomainList.forEach((domain) => {
//         this.conceptsCache.push({
//           domain: domain.domain,
//           items: [],
//           vocabularyList: []
//         });
//         this.conceptDomainCounts.push({
//           domain: domain.domain,
//           name: domain.name,
//           conceptCount: 0
//         });
//         this.selectedDomain = this.conceptDomainCounts[0];
//       });
//       const {domain: currentDomain} = queryParamsStore.getValue();
//       if (currentDomain !== undefined) {
//         this.browseDomain(
//           this.conceptDomainList.find(
//             domainInfo => domainInfo.domain === currentDomain));
//       }
//       this.loadingDomains = false;
//     });
//   }
//
//   openAddModal(): void {
//     this.conceptAddOpen = true;
//   }
//
//   closeAddModal(): void {
//     this.conceptAddOpen = false;
//   }
//
//   selectDomain(domainCount: DomainCount) {
//     if (!this.selectedConceptDomainMap[domainCount.domain]) {
//       this.selectedConceptDomainMap[domainCount.domain] = 0;
//     }
//     this.selectedDomain = domainCount;
//     this.placeholderValue = this.noConceptsConstant;
//     this.setConceptsAndVocabularies();
//   }
//
//   searchButton() {
//     const searchTermLength = this.searchTerm.trim().length;
//     if (searchTermLength === 0) {
//       this.clearSearch();
//       return;
//     }
//     if (searchTermLength < 3) {
//       this.showSearchError = true;
//       return;
//     }
//     this.showSearchError = false;
//     this.currentSearchString = this.searchTerm;
//     this.reset();
//     this.searchConcepts();
//   }
//
//   returnToConcepts() {
//     this.clearSearch();
//     this.searching = false;
//   }
//
//   private rebuildSelectedConceptDomainMap() {
//     this.selectedConceptDomainMap = new Map<string, number>();
//     this.conceptDomainList.forEach((domain) => {
//       this.selectedConceptDomainMap.set(domain.name, 0);
//     });
//   }
//
//   reset() {
//     this.rebuildSelectedConceptDomainMap();
//     this.conceptDomainCounts = [];
//   }
//
//   clearSearch() {
//     this.searchTerm = '';
//     this.currentSearchString = '';
//     this.rebuildSelectedConceptDomainMap();
//     this.searchConcepts();
//   }
//
//   browseDomain(domain: DomainInfo) {
//     this.currentSearchString = '';
//     this.selectedDomain =
//       this.conceptDomainCounts.find(domainCount => domainCount.domain === domain.domain);
//     this.searchConcepts();
//   }
//
//   searchConcepts() {
//     if (this.conceptTable) {
//       this.conceptTable.selectedConcepts = [];
//       this.conceptsToAdd = [];
//       this.rebuildSelectedConceptDomainMap();
//     }
//     this.searching = true;
//     this.searchLoading = true;
//     this.placeholderValue = this.noConceptsConstant;
//     let standardConceptFilter: StandardConceptFilter;
//     if (this.standardConceptsOnly) {
//       standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
//     } else {
//       standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
//     }
//     this.completedDomainSearches = [];
//
//     let numCalls = 0;
//     const numCallsSubject = new Subject<Domain>();
//     const numCallsSubject$ = numCallsSubject.asObservable();
//
//     numCallsSubject$.subscribe((finishedCall) => {
//       this.completedDomainSearches.push(finishedCall);
//     });
//
//     this.conceptsCache.forEach((conceptDomain) => {
//       const activeTabSearch = conceptDomain.domain === this.selectedDomain.domain;
//       const request = {
//         query: this.currentSearchString,
//         standardConceptFilter: standardConceptFilter,
//         domain: conceptDomain.domain,
//         includeDomainCounts: activeTabSearch,
//         includeVocabularyCounts: true,
//         maxResults: this.maxConceptFetch
//       };
//       this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
//         .subscribe((response) => {
//           numCalls += 1;
//           numCallsSubject.next(conceptDomain.domain);
//           conceptDomain.items = this.convertToConceptInfo(response.items);
//           conceptDomain.vocabularyList = response.vocabularyCounts;
//           if (activeTabSearch) {
//             this.searchLoading = false;
//             this.conceptDomainCounts = response.domainCounts;
//             this.selectedDomain =
//               this.conceptDomainCounts.find(domainCount => domainCount.domain === request.domain);
//             this.setConceptsAndVocabularies();
//           }
//         });
//     });
//   }
//
//   setConceptsAndVocabularies() {
//     const cacheItem = this.conceptsCache.find(
//       conceptDomain => conceptDomain.domain === this.selectedDomain.domain);
//     this.concepts = cacheItem.items;
//     this.conceptsToAdd = this.concepts.filter((c) => c.selected);
//     this.vocabularies = [];
//     this.vocabularies = cacheItem.vocabularyList.map((vocabulary) => {
//       return {
//         ...vocabulary,
//         selected: true
//       };
//     });
//   }
//
//   filterList() {
//     if (this.blockMultipleSearchFromFilter) {
//       this.blockMultipleSearchFromFilter = false;
//       return;
//     }
//     if (this.vocabularies.filter(vocabulary => vocabulary.selected).length === 0) {
//       this.concepts = [];
//       this.conceptsToAdd = [];
//       this.conceptTable.selectedConcepts = [];
//       this.rebuildSelectedConceptDomainMap();
//       this.placeholderValue = 'No vocabularies selected. Please select at least one vocabulary.';
//       return;
//     }
//     this.blockMultipleSearchFromFilter = true;
//     let standardConceptFilter: StandardConceptFilter;
//     if (this.standardConceptsOnly) {
//       standardConceptFilter = StandardConceptFilter.STANDARDCONCEPTS;
//     } else {
//       standardConceptFilter = StandardConceptFilter.ALLCONCEPTS;
//     }
//     this.searchLoading = true;
//     this.placeholderValue = this.noConceptsConstant;
//
//     const request = {
//       query: this.currentSearchString,
//       standardConceptFilter: standardConceptFilter,
//       domain: this.selectedDomain.domain,
//       vocabularyIds: this.vocabularies
//         .filter(vocabulary => vocabulary.selected).map(vocabulary => vocabulary.vocabularyId),
//       maxResults: this.maxConceptFetch
//     };
//     this.conceptsService.searchConcepts(this.wsNamespace, this.wsId, request)
//       .subscribe((response) => {
//         this.searchLoading = false;
//         this.concepts = this.convertToConceptInfo(response.items);
//         this.filterConceptSelection(this.concepts);
//         this.conceptTable.selectedConcepts = this.concepts.filter(v => v.selected);
//         this.conceptsToAdd = this.conceptTable.selectedConcepts;
//         this.selectedConceptDomainMap[this.selectedDomain.domain] =
//           this.conceptTable.selectedConcepts.length;
//       });
//   }
//
//   selectConcept(concepts: ConceptInfo[]) {
//     // TODO Check why clr-datagrid is sending empty and duplicate values
//     this.conceptsToAdd = concepts;
//     const domainName = this.selectedDomain.domain;
//     this.selectedConceptDomainMap[domainName] = concepts.filter(concept => {
//       return concept.domainId.toLowerCase() === this.selectedDomain.domain.toString().toLowerCase();
//     }).length;
//   }
//
//   afterConceptsSaved() {
//     this.setConceptsSaveText();
//     // Once concepts are saved clear the selection from concept homepage for active Domain
//     this.conceptTable.selectedConcepts.length = 0;
//     this.selectedConceptDomainMap[this.selectedDomain.domain] = 0;
//     this.cloneCacheConcepts();
//     this.conceptAddOpen = false;
//   }
//
//   setConceptsSaveText() {
//     const conceptsCount = this.selectedConceptDomainMap[this.selectedDomain.domain];
//     this.conceptsSavedText = conceptsCount + ' ' + this.selectedDomain.name +
//       (conceptsCount > 1 ? ' concepts ' : ' concept ') + 'have been added ';
//     setTimeout(() => {
//       this.conceptsSavedText = '';
//     }, 5000);
//   }
//
//   /* This is done because clr-datagrid has a bug which causes unselected entries to
//    appear as selected on refresh*/
//   cloneCacheConcepts() {
//     const cacheItem = this.conceptsCache.find(
//       conceptDomain => conceptDomain.domain === this.selectedDomain.domain);
//     const cloneConcepts = cacheItem.items.map(x => Object.assign({}, x));
//     cacheItem.items = cloneConcepts;
//   }
//
//   get activeSelectedConceptCount(): number {
//     if (!this.selectedDomain
//       || !this.selectedDomain.domain
//       || !this.selectedConceptDomainMap[this.selectedDomain.domain]
//       || this.selectedConceptDomainMap[this.selectedDomain.domain] === 0) {
//       return 0;
//     }
//     return this.selectedConceptDomainMap[this.selectedDomain.domain];
//   }
//
//   get noConceptsConstant() {
//     return 'No concepts found for domain \'' + this.selectedDomain.name + '\' this search.';
//   }
//
//   get addToSetText(): string {
//     const count = this.activeSelectedConceptCount;
//     return count === 0 ? 'Add to set' : 'Add (' + count + ') to set';
//   }
//
//   domainLoading(domain) {
//     return this.searchLoading || !this.completedDomainSearches.includes(domain.domain);
//   }
//
//   convertToConceptInfo(concepts: Concept[]): ConceptInfo[] {
//     const conceptInfos = concepts.map((concept) => {
//       return {
//         ...concept,
//         selected: false
//       };
//     });
//     this.filterConceptSelection(conceptInfos);
//     return conceptInfos;
//   }
//
//   private filterConceptSelection(concepts: ConceptInfo[]) {
//     const conceptSet = new Set(this.conceptsToAdd.map(c => c.conceptId));
//     concepts.forEach((concept) => {
//       concept.selected = conceptSet.has(concept.conceptId);
//     });
//   }
// }
