import React, { Component } from 'react';
import { isEqual } from 'lodash';
import {esBaseUrl} from './query.js';

class Facets extends Component {
  constructor(props) {
    super(props);

    this.state = {};
  }

  componentDidMount() {
    this.fetchFacets(this.props);
  }

  componentWillReceiveProps(nextProps) {
    if (!isEqual(this.props.boolFilter, nextProps.boolFilter) ||
        this.props.fieldName !== nextProps.fieldName) {
      this.fetchFacets(nextProps);
    }
  }

  multiGetConcepts(ids) {
    if (!ids.length) {
      return Promise.resolve({});
    }
    return fetch(`${esBaseUrl}/concept/concept/_mget`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8'
      },
      body: JSON.stringify({ids})
    }).then(async (resp) => {
      if (!resp.ok) {
        throw await resp.json();
      }
      return (await resp.json()).docs.map(d => d._source);
    });
  }

  fetchFacets(props) {
    let aggs = {
      results: {
        terms: { field: props.fieldName }
      }
    };
    if (props.nestedPath) {
      // Note: This is extremely slow (~10s).
      aggs = {
        [props.nestedPath]: {
          nested: {path: props.nestedPath},
          aggs: {
            results: {
              terms: { field: `${props.nestedPath}.${props.fieldName}` }
            }
          }
        }
      };
    }

    const boolFilter = props.boolFilter;
    fetch(`${esBaseUrl}/person/person/_search`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8'
      },
      body: JSON.stringify({
        aggs,
        _source: false,
        size: 10,
        query: boolFilter || undefined
      })
    }).then(async (resp) => {
      if (!resp.ok) {
        throw await resp.json();
      }
      if (!isEqual(boolFilter, this.props.boolFilter)) {
        return;
      }
      const facets = await resp.json();
      let results;
      if (props.nestedPath) {
        results = facets.aggregations[props.nestedPath].results;
      } else {
        results = facets.aggregations.results;
      }
      const buckets = results.buckets.filter(b => b.key !== '0');
      const concepts = await this.multiGetConcepts(buckets.map(f => f.key));
      if (!isEqual(boolFilter, this.props.boolFilter)) {
        return;
      }
      this.setState({buckets: buckets.map((f, i) => ({
        ...concepts[i],
        count: f.doc_count
      }))});
    });
  }

  onCriteriaChange(c, not) {
    this.props.onCriteriaChange(c.concept_id, c.domain, not);
  }

  render() {
    const buckets = this.state.buckets || [];
    return (
        <div>
          {buckets.map((b) => {
            return <div key={b.name} className="results">
                <span className="conceptName">{b.name}</span> {b.count.toLocaleString()}
                <button style={{float: 'right'}}
                        onClick={this.onCriteriaChange.bind(this, b, true)}
                        >-</button>
                <button style={{float: 'right'}}
                        onClick={this.onCriteriaChange.bind(this, b, false)}
                        >+</button>
          </div>;
          })}
        </div>
    );
  }
}

export default Facets;
