import React, { Component } from 'react';
import { isEqual } from 'lodash';
import {esBaseUrl} from './query.js';

class CannedFacets extends Component {
  constructor(props) {
    super(props);

    this.state ={
      facets: null
    };
  }

  componentDidMount() {
    this.fetchFacets(this.props);
  }

  componentWillReceiveProps(nextProps) {
    if (!isEqual(this.props.boolFilter, nextProps.boolFilter)) {
      this.fetchFacets(nextProps);
    }
  }

  fetchFacets(props) {
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
              filters: props.concepts.map(c => ({
                match: {
                  [props.fieldName]: c
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
      this.setState({facets: await resp.json()});
    });
  }

  render() {
    let buckets = [];
    if (this.state.facets) {
      buckets = this.state.facets.aggregations.concepts.buckets;
    }
    return (
        <div>
          {buckets.map((b, i) => {
            return <div>{this.props.concepts[i]} - {b.doc_count}</div>;
          })}
        </div>
    );
  }
}

export default CannedFacets;
