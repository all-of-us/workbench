import * as fp from 'lodash/fp';
import {ReactWrapper} from 'enzyme';


export const findNodesByExactText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && node.text() === text);
}));

export const findNodesContainingText = fp.curry((wrapper: ReactWrapper, text) => wrapper.findWhere(node => {
  return (node.name() === null && node.text().includes(text));
}));