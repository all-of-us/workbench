import React, { Component } from 'react';
import './App.css';
import Charts from './Charts.js';
import Facets from './Facets.js';
import QueryBox from './QueryBox.js';
import Search from './Search.js';
import {domainToQuerySymbol, symbolToField, esBaseUrl, queryToES} from './query.js';

//  const cannedConcepts = [
//  // Conditions:
//    //  133714,
//  //  134438,
//  //  134668,
//  //  134736,
//    //  135777
//    1125315,
//    1000560,
//    1112807,
//    1126658,
//    1154029,
//    1135766
//  ];

class App extends Component {

  constructor(props) {
    super(props);

    this.state = {
      indexMeta: {},
      q: '',
      filterSearch: false,
      boolFilter: null,
      hideMeta: true,
      topDomain: 'Drug'
    };
  }

  componentDidMount() {
    fetch(`${esBaseUrl}/person`)
        .then(async (resp) => {
          if (!resp.ok) {
            throw await resp.json();
          }
          this.setState({indexMeta: await resp.json()});
        });
  }

  toggleMeta() {
    this.setState(state => {
      return {hideMeta: !state.hideMeta};
    });
  }

  onCriteriaChange(id, domain, not) {
    this.setState(state => {
      let q = state.q;
      if (q) {
        q += ' and';
      }
      if (not) {
        q += ' not';
      }
      const d = domainToQuerySymbol(domain);
      q += ` has ${d}${id}`;
      return {q, boolFilter: queryToES(q)};
    });
  }

  render() {
    const domain = this.state.topDomain;
    const indexMeta = this.state.indexMeta || {};
    const q = this.state.q;
    const boolFilter = this.state.boolFilter;
    return (
      <div className="App">
        <div className="queryBox">
          <QueryBox q={q} onBoolFilterChange={(q, boolFilter) => {
              this.setState({q, boolFilter});
            }} />
        </div>
        <div className="facets">
          <div>
            {['Drug', 'Condition', 'Measurement'].map((d) => {
              let style = {};
              if (this.state.topDomain === d) {
                style.backgroundColor = 'gray';
              }
              return <button style={style}
                        key={d}
                        onClick={() => this.setState({topDomain: d})}>
                  {d}
              </button>
            })}
            <h3>Top {domain} Hits</h3>
            <Facets fieldName={symbolToField(domainToQuerySymbol(domain))}
                    onCriteriaChange={this.onCriteriaChange.bind(this)}
                    boolFilter={boolFilter} />
          </div>
          <div>
            <h3>Concept Search</h3>
            <input id="filterSearch" type="checkbox"
                   onChange={(e) => this.setState({filterSearch: e.target.checked})}>
            </input>
            <label htmlFor="filterSearch">Filtered to Cohort</label>
            <Search fieldName="name"
                    onCriteriaChange={this.onCriteriaChange.bind(this)}
                    boolFilter={this.state.filterSearch ? boolFilter : null}
                    />
          </div>
        </div>
        <div className="charts">
          <Charts fieldName="gender" boolFilter={boolFilter} />
        </div>
        <div>
          <span
            onClick={this.toggleMeta.bind(this)} style={{cursor: 'pointer'}}>
            {this.state.hideMeta ? '+' : '-'}
          </span>
          <p style={{display: this.state.hideMeta ? 'none': null}}>
            {JSON.stringify(indexMeta, null, '\t')}
          </p>
        </div>
      </div>
    );
  }
}

export default App;
