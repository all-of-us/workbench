import React, { Component } from 'react';
import {queryToES} from './query.js';
import './QueryBox.css';

class QueryBox extends Component {
  constructor(props) {
    super(props);

    this.state = {
      query: props.q,
      queryError: null
    };
  }

  componentWillReceiveProps(nextProps) {
    this.setState({query: nextProps.q});
  }

  applyQuery(query) {
    let boolFilter = null;
    let queryError = null;

    if (query.trim()) {
      try {
        boolFilter = queryToES(query);
      } catch(e) {
        console.log('failed to parse query: ', e.message);
        queryError = e.message;
      }
    }
    this.setState({query, queryError});
    if (!queryError) {
      this.props.onBoolFilterChange(query, boolFilter);
    }
  }

  render() {
    return (
        <input type="text"
               className={this.state.queryError ?
                          'err' : (this.state.query ? 'valid' : '')
               }
               style={{width: '80%', fontSize: '2rem'}}
               value={this.state.query}
               onChange={(e) => this.applyQuery(e.target.value)}
          ></input>
    );
  }

  // expr: {
  //   left: expr, right: expr, op: and|or
  // }|{
  //   expr: expr, op: not
  // }|{
  //   value: CID, op: has
  // }
  // }|{
  //   left: CID, op: =|<|>, right: value
  // }
}

//console.log(queryToES('has 1125315', 'drug_ids'));
//console.log(queryToES('((has 123) and not has 345)', 'drug_ids'));
//console.log(parse('(has 1 and has 2 and has 3 or (has 4 or has 5))')[1]);
//console.log(parse('(val(1337) < 40)')[1]);

export default QueryBox;
