import {BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip} from 'recharts';
import React, { Component } from 'react';
import { isEqual } from 'lodash';
import {esBaseUrl} from './query.js';

class Charts extends Component {
  constructor(props) {
    super(props);

    this.state = {
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
        query: props.boolFilter || undefined,
        aggs: {
          results: {
            terms: { field: props.fieldName + '.keyword' },
            aggs: {
              ages: {
                range: {
                  field: 'age',
                  ranges: [
                    {from: 19, to: 45},
                    {from: 45, to: 65},
                    {from: 65}
                  ]
                }
              }
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
    let data = [];
    let flatData = [];
    let total = 0;
    const ages = new Set();
    if (this.state.facets) {
      total = this.state.facets.hits.total;
      data = this.state.facets.aggregations.results.buckets;
      flatData = data.map(gender => {
        if (gender.key === 'PMI Other (Qualifier Value)') {
          gender.key = 'Other';
        } else if (gender.key === 'None of these describe me, and Id like to consider additional options') {
          gender.key = 'N/A';
        } else if (gender.key === 'PMI Prefer Not To Answer') {
          gender.key = 'Decline';
        }
        const out = {gender: gender.key};
        gender.ages.buckets.forEach(age => {
          out[age.key] = age.doc_count;
          ages.add(age.key);
        });
        return out;
      });
    }
    flatData.sort((a, b) => a.gender.localeCompare(b.gender));

    const colors = ['#8884d8', '#82ca9d', '#e1bb75'];
    const ageKeys = [];
    for (let age of ages) {
      const color = colors.pop() || 'black';
      ageKeys.push({age, color});
    }
  	return (<>
            {/*
    	<BarChart width={600} height={300} data={data}
            margin={{top: 5, right: 30, left: 20, bottom: 5}}>
       <CartesianGrid strokeDasharray="3 3"/>
       <XAxis dataKey="key"/>
       <YAxis/>
       <Tooltip/>
       <Bar dataKey="doc_count" fill="#8884d8" />
      </BarChart>
             */}
    	<BarChart width={700} height={400} data={flatData}
            margin={{top: 5, right: 30, left: 20, bottom: 5}}>
       <CartesianGrid strokeDasharray="3 3"/>
       <XAxis dataKey="gender"/>
       <YAxis/>
       <Tooltip/>
       {ageKeys.map((age) => (
           <Bar key={age.age} dataKey={age.age} stackId="a" fill={age.color} />
       ))}
      </BarChart>
      <div style={{marginBottom: '20px', textAlign: 'center'}}>Cohort size: {total.toLocaleString()}</div>
      </>
    );
  }
}

export default Charts;
