import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

export const styles = reactStyles({
  h1: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px'
  },
  inputLabel: {
    color: colors.primary,
    fontSize: 14,
    fontWeight: 600,
    lineHeight: '22px',
    marginBottom: 6
  },
  inputStyle: {
    width: 300,
    height: 40,
    fontSize: 14,
    marginRight: 20
  },
  dataAccessText: {
    fontWeight: 500,
    color: colors.primary
  },
  longInputContainerStyle: {
    width: 420,
    resize: 'both'
  },
  longInputHeightStyle: {
    height: 175,
  },
  box: {
    backgroundColor: colors.white,
    borderRadius: 8,
    padding: 21
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    width: '40%',
    display: 'inline',
    alignItems: 'flexEnd'
  },
  uneditableProfileElement: {
    paddingLeft: '0.5rem',
    marginRight: 20,
    marginBottom: 20,
    height: '1.5rem',
    color: colors.primary
  },
  fadebox: {
    margin: '1rem 0 0 3%',
    width: '95%',
    padding: '0 0.1rem'
  },
  verticalLine: {
    marginTop: '0.3rem', marginInlineStart: '0rem', width: '100%'
  },
  researchPurposeInfo: {
    fontWeight: 100,
    width: '80%',
    marginTop: '0.5rem',
    marginBottom: '0.3rem'
  },
  freeCreditsBox: {
    borderRadius: '0.4rem',
    height: '3rem',
    marginTop: '0.7rem',
    marginBottom: '1.7rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.disabled, 0.7)
  },
  updateSurveyButton: {
    textTransform: 'none',
    padding: 0,
    height: 'auto'
  }
});
