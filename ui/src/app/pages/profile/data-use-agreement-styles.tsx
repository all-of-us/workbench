import * as fp from 'lodash/fp';
import * as React from 'react';
import {styles as headerStyles} from '../../components/headers';
import {TextInput} from '../../components/inputs';
import colors from '../../styles/colors';
import {reactStyles} from '../../utils';



export const AoUTitle = () => {
  return <span><i>All of Us</i> Research Program</span>;
};

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
  h2: {...headerStyles.h2, lineHeight: '24px', fontWeight: 600, fontSize: '16px'},
  sanctionModalTitle: {
    fontFamily: 'Montserrat',
    fontSize: 16,
    fontWeight: 600,
    lineHeight: '19px'
  },
  modalLabel: {
    fontFamily: 'Montserrat',
    fontSize: 14,
    lineHeight: '24px',
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
