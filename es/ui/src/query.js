const esBaseUrl = 'http://localhost:9200';

const domainToSymbol = {
  Drug: 'd',
  Condition: 'c',
  Measurement: 'm'
};
const fieldMap = {
  c: 'condition_ids',
  d: 'drug_ids',
  m: 'measurement_ids'
};
const nestedFieldMap = {
  c: {
    f: 'conditions',
    id: 'condition_concept_id',
    v: 'condition_start_date'
  },
  d: {
    f: 'drugs',
    id: 'drug_concept_id',
    v: 'drug_exposure_start_date'
  },
  m: {
    f: 'measurements',
    id: 'measurement_source_concept_id',
    v: 'measurement_source_value'
  }
};

const domainToQuerySymbol = (d) => domainToSymbol[d];
const symbolToField = (s) => fieldMap[s];

const toES = (p) => {
  const f = fieldMap[p.domain];
  const nested = nestedFieldMap[p.domain];
  if (p.domain && (!f || !nested)) throw Error(`unknown domain code ${f}`);
  switch(p.op) {
    case 'and':
      return {
        bool: {
          filter: [toES(p.left), toES(p.right)]
        }
      };
    case 'or':
      return {
        bool: {
          should: [toES(p.left), toES(p.right)]
        }
      };
    case 'not':
      return {
        bool: {
          must_not: toES(p.expr)
        }
      };
    case 'has':
      return {
        term: {
          [f]: p.value
        }
      };
    case '<':
    case '>':
      const esOp = p.op === '<' ? 'lt' : 'gt';
      return {
        nested: {
          path: nested.f,
          query: {
            bool: {
              filter: [
                {
                  term: {
                    [`${nested.f}.${nested.id}`]: p.left
                  }
                },
                {
                  range: {
                    [`${nested.f}.${nested.v}`]: {
                      [esOp]: p.right,
                      format: 'date_time'
                    }
                  }
                }
              ]
            }
          }
        }
      };
    default:
      throw new Error(`bad query structure: ${JSON.stringify(p)}`);
  }
};


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
const parse = (q) => {
  q = q.trim();
  if (!q) {
    throw Error(`Invalid query: ${q}`);
  }
  if (q.startsWith('(')) {
    let expr;
    [q, expr] = parse(q.substring(1));
    q = q.trim();
    for (;;) {
      if (q.startsWith('and ') || q.startsWith('or ')) {
        let op = q.startsWith('and') ? 'and' : 'or';
        let left = expr;
        let right;
        [q, right] = parse(q.replace(/^(and|or) /, ''));
        expr = {left, right, op};
      } else if (q.startsWith(')')) {
        break;
      } else {
        throw Error(`Unrecognized token, wanted and|or|): ${q}`);
      }
      q = q.trim();
    }
    return [q.substring(1), expr];
  } else if (q.startsWith('has')) {
    const m = q.match(/^has ([a-z])(\d+)/);
    if (!m) {
      throw Error(`has without a valid concept ID: ${q}`);
    }
    return [
      q.substring(m[0].length),
      {
        op: 'has',
        domain: m[1],
        value: Number(m[2])
      }
    ];
  } else if (q.startsWith('val(')) {
    const m = q.match(/^val\(([a-z])(\d+)\) +(>|<|=) ([/0-9]+)/);

    if (!m) {
      throw Error(`val without a concept ID ${q}`);
    }
    return [
      q.substring(m[0].length),
      {
        domain: m[1],
        left: Number(m[2]),
        op: m[3],
        right: new Date(m[4])
      }
    ];
  } else if (q.startsWith('not ')) {
    q = q.substring('not '.length);
    let expr;
    [q, expr] = parse(q);
    return [q, {
      expr,
      op: 'not'
    }];
  }
  throw Error(`Unrecognized initial token ${q}`);
};

const hasOuterParens = (q) => {
  q = q.trim();
  if (!q.startsWith('(')) {
    return false;
  }
  const stack = [];
  for (let i = 0; i < q.length; i++) {
    if (q[i] === '(') {
      stack.push(1);
    } else if (q[i] === ')') {
      stack.pop();
    }
    if (i < q.length-1 && !stack.length) {
      return false;
    }
  }
  return true;
};

const queryToES = (q, fieldMap) => {
  if (!hasOuterParens(q)) {
    q = `(${q})`;
  }
  return {
    bool: {
      filter: toES(parse(q)[1])
    }
  };
};

export {domainToQuerySymbol, symbolToField, esBaseUrl, queryToES};
