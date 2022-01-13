import * as fp from 'lodash/fp';

export const formatUsd = (number) => {
  if (fp.isNaN(number)) {
    return 'unknown';
  } else if (number > 0 && number < 0.01) {
    return '< $0.01';
  } else {
    return Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(number);
  }
};
