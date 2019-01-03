import React, { Component } from 'react';
import { isEqual } from 'lodash';
import {esBaseUrl, domainToQuerySymbol, symbolToField} from './query.js';
import './Search.css';

class Search extends Component {
  constructor(props) {
    super(props);

    this.state = {
      query: '',
      results: null,
      facets: null,
      boolFilter: null
    };
  }

  componentDidMount() {}

  componentWillReceiveProps(nextProps) {
    if (!isEqual(this.props.boolFilter, nextProps.boolFilter)) {
      this.fetchResults(nextProps, this.state.query);
    }
  }

  fetchResults(props, query) {
    if (this.fetchTimeout) {
      clearTimeout(this.fetchTimeout);
    }
    this.fetchTimeout = setTimeout(() => {
      this.fetchTimeout = null;
      fetch(`${esBaseUrl}/concept/concept/_search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({
          size: 20,
          query: {
            match: {
              [props.fieldName]: {
                query,
                operator: 'and',
                fuzziness: 2,
                prefix_length: 2
              }
            }
          }
        })
      }).then(async (response) => {
        if (!response.ok) {
          const j = await response.json();
          this.setState({
            results: null,
            facets: null,
            error: j
          });
          return;
        }
        const results = await response.json();
        this.setState({
          results,
          facets: null,
          error: null
        });
        this.fetchFacets(props, results);
      });
    }, 150);
  }

  fetchFacets(props, results) {
    const concepts = results.hits.hits.map(h => ({
      id: h._source.concept_id,
      fieldName: symbolToField(domainToQuerySymbol(h._source.domain))
    }));
    fetch(`${esBaseUrl}/person/person/_search`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8'
      },
      body: JSON.stringify({
        _source: false,
        size: 0,
        query: props.boolFilter || undefined,
        aggs: {
          concepts: {
            filters: {
              filters: concepts.map(c => ({
                match: {
                  [c.fieldName]: c.id
                }
              }))
            }
          }
        }
      })
    }).then(async (resp) => {
      if (!resp.ok) {
        throw await resp.json();
      }
      const facets = await resp.json();
      this.setState(s => {
        if (isEqual(results, s.results)) {
          return {facets};
        }
      });
    });
  }

  onCriteriaChange(c, not) {
    this.props.onCriteriaChange(c.concept_id, c.domain, not);
  }

  render() {
    let hits = [];
    let facets = {};
    if (this.state.results && this.state.facets) {
      hits = this.state.results.hits.hits.map(h => h._source);
      this.state.facets.aggregations.concepts.buckets.forEach((c, i) => {
        facets[hits[i].concept_id] = c.doc_count;
      });
      if (this.props.boolFilter) {
        hits = hits.filter(h => facets[h.concept_id] > 0);
      }
    }
    return (
        <div className="Search">
          {this.state.error && (
              <p>
                {JSON.stringify(this.state.error, null, '\t')}
              </p>)}
          <input type="text"
                 value={this.state.search}
                 onChange={(e) => {
                   this.setState({query: e.target.value});
                   this.fetchResults(this.props, e.target.value);
               }}></input>
          <div className="results">
            {hits.map(h => (
                <div key={h.concept_id}>
                  <span className="conceptName">{h.name}</span> {facets[h.concept_id].toLocaleString()}
                  <button style={{float: 'right'}}
                          onClick={this.onCriteriaChange.bind(this, h, true)}
                          >-</button>
                  <button style={{float: 'right'}}
                          onClick={this.onCriteriaChange.bind(this, h, false)}
                          >+</button>
                </div>
            ))}
          </div>
        </div>
    );
  }
}

export default Search;
