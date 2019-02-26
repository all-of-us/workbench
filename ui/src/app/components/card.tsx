import {reactStyles, withStyle} from 'app/utils';

export const styles = reactStyles({
  card: {
    padding: '1rem',
    borderRadius: '0.2rem',
    boxShadow: '0 0.125rem 0.125rem 0 #d7d7d7',
    backgroundColor: '#fff',
    border: '1px solid #d7d7d7',
    display: 'flex',
    flexDirection: 'column',
    width: '12rem',
    height: '9rem',
    margin: '0 1rem 1rem 0'
  }
});

export const Card = withStyle(styles.card)('div');
