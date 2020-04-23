import {styles as headerStyles} from 'app/components/headers';
import {TextInput} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as fp from 'lodash/fp';
import * as React from 'react';


export const indentedListStyles = {
  margin: '0.5rem 0 0.5rem 1.5rem', listStylePosition: 'outside'
};

export const dataUserCodeOfConductStyles = reactStyles({
  dataUserCodeOfConductPage: {
    paddingTop: '2rem',
    paddingLeft: '3rem',
    paddingBottom: '2rem',
    maxWidth: '48rem',
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
  },
  textInput: {
    padding: '0 1ex',
    width: '12rem',
    fontSize: 10,
    borderRadius: 6
  }
});

export const SecondHeader = (props) => {
  return <h2 style={{...dataUserCodeOfConductStyles.h2, ...props.style}}>{props.children}</h2>;
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

export const DuaTextInput = (props) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return <TextInput {...fp.omit(['data-test-id'], props)}
                    style={{
                      ...dataUserCodeOfConductStyles.textInput,
                      ...props.style
                    }}/>;
};

export const InitialsAgreement = (props) => {
  return <div style={{display: 'flex', marginTop: '0.5rem'}}>
    <DuaTextInput onChange={props.onChange} value={props.value}
                  placeholder='INITIALS' data-test-id='dua-initials-input'
                  style={{width: '4ex', textAlign: 'center', padding: 0}}/>
    <div style={{marginLeft: '0.5rem'}}>{props.children}</div>
  </div>;
};
