import * as React from 'react';

const styles = {
  fadeTop: {
    height: 8,
    border: 'solid #b7b7b7',
    borderWidth: '1px 1px 0 1px',
    borderRadius: '8px 8px 0 0',
    backgroundColor: '#fff',
  },
  fadeBottom: {
    padding: 'calc(1rem - 8px) 1rem 0',
    minHeight: '8rem',
    background: 'linear-gradient(180deg, #ffffff 0, rgba(255, 255, 255, 0) 8rem)',
    border: 'solid transparent',
    borderImage: 'linear-gradient(#b7b7b7, rgba(183, 183, 183, 0) 8rem) 1 100% / 1 / 0 stretch',
    borderWidth: '0 1px'
  }
};

export const FadeBox = ({children, ...props}) => {
  return <div {...props}>
    <div style={styles.fadeTop} />
    <div style={styles.fadeBottom}>{children}</div>
  </div>;
};
