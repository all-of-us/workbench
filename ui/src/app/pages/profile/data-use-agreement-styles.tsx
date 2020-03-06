import {styles as headerStyles} from 'app/components/headers';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as React from 'react';


export const indentedListStyles = {
  margin: '0.5rem 0 0.5rem 1.5rem', listStylePosition: 'outside'
};

export const dataUseAgreementStyles = reactStyles({
  dataUseAgreementPage: {
    paddingTop: '2rem',
    paddingLeft: '3rem',
    paddingBottom: '2rem',
    maxWidth: '50rem',
    height: '100%',
    color: colors.primary,
  },
  h2: {...headerStyles.h2, lineHeight: '1rem', fontWeight: 600, fontSize: '0.67rem'},
  sanctionModalTitle: {
    fontFamily: 'Montserrat',
    fontSize: '0.67rem',
    fontWeight: 600,
    lineHeight: '0.8rem'
  },
  modalLabel: {
    fontFamily: 'Montserrat',
    fontSize: '0.5rem',
    lineHeight: '1rem',
    color: colors.primary
  }
});

export const SecondHeader = (props) => {
  return <h2 style={{...dataUseAgreementStyles.h2, ...props.style}}>{props.children}</h2>;
};

export const IndentedUnorderedList = (props) => {
  return <ul style={{...indentedListStyles, ...props.style}}>{props.children}</ul>;
};

export const IndentedOrderedList = (props) => {
  return <ol style={{...indentedListStyles, ...props.style}}>{props.children}</ol>;
};

export const IndentedListItem = (props) => {
  return <li style={{marginTop: '0.5rem', ...props.style}}>{props.children}</li>;
};
