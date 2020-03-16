import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

export const styles = reactStyles({
  categoryRow: {
    display: 'flex', flexDirection: 'row', padding: '0.6rem 0', width: '95%'
  },
  checkboxRow: {
    display: 'inline-block', padding: '0.2rem 0', marginRight: '1rem'
  },
  checkboxStyle: {
    marginRight: '.31667rem', zoom: '1.5'
  },
  flexColumnBy2: {
    flex: '1 1 0',
    marginLeft: '1rem'
  },
  header: {
    fontWeight: 600,
    lineHeight: '24px',
    color: colors.primary
  },
  infoIcon: {
    height: '16px',
    marginLeft: '0.2rem',
    width: '16px'
  },
  longDescription: {
    position: 'relative',
    display: 'inline-block',
    minHeight: '1rem',
    cursor: 'text',
    lineHeight: '1rem',
    width: '100%'
  },
  requiredText: {
    fontSize: '13px',
    fontStyle: 'italic',
    fontWeight: 400,
    color: colors.primary,
    marginLeft: '0.2rem'
  },
  researchPurposeRow: {
    backgroundColor: colors.white,
    borderColor: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
    marginLeft: '-1rem',
    paddingTop: '0.3rem',
    paddingBottom: '0.3rem'
  },
  select: {
    display: 'inline-block',
    verticalAlign: 'middle',
    position: 'relative',
    overflow: 'visible',
    width: '11.3rem',
    marginRight: '20px'
  },
  shortDescription: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 600,
    lineHeight: '24px',
    cursor: 'pointer'
  },
  spinner: {
    position: 'fixed',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    height: '100%',
    width: '100%',
  },
  text: {
    fontSize: '13px',
    color: colors.primary,
    fontWeight: 400,
    lineHeight: '24px'
  },
  textArea: {
    height: '15rem',
    resize: 'none',
    width: '50rem',
    borderRadius: '3px 3px 0 0',
    boderColor: colorWithWhiteness(colors.dark, 0.5)
  },
  textBoxCharRemaining: {
    justifyContent: 'space-between',
    width: '50rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.95),
    fontSize: 12,
    colors: colors.primary,
    padding: '0.25rem',
    borderRadius: '0 0 3px 3px', marginTop: '-0.5rem',
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`
  },
  textInput: {
    width: '20rem',
    borderColor: 'rgb(151, 151, 151)',
    borderRadius: '6px',
    marginRight: '20px',
    marginBottom: '5px'
  }
});
