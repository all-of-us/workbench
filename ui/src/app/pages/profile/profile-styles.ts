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
  dataAccessTier: {
    marginBottom: '0.9rem',
    display: 'grid',
    columnGap: '0.25rem',
    width: 459,
    gridTemplateColumns: 'fit-content(2rem) fit-content(10rem) 1fr',
    gridTemplateAreas: `"badge label available"
                        ". primary primary"`
  },
  dataAccessText: {
    fontWeight: 500,
    color: colors.primary,
    lineHeight: '22px',
  },
  longInputContainerStyle: {
    width: 420,
    resize: 'both'
  },
  longInputHeightStyle: {
    height: 175,
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    width: '40%',
    display: 'inline',
    alignItems: 'flexEnd'
  },
  fadebox: {
    margin: '1rem 0 0 3%',
    width: '95%',
    padding: '0 0.1rem'
  },
  verticalLine: {
    marginTop: '0.3rem',
    marginInlineStart: '0rem',
    width: '100%',
  },
  researchPurposeInfo: {
    fontWeight: 100,
    width: '80%',
    marginTop: '0.5rem',
    marginBottom: '0.3rem'
  },
  initialCreditsBox: {
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
  },
  renewalBox: {
    borderRadius: 5,
    display: 'flex',
    alignItems: 'center',
    backgroundColor: colorWithWhiteness(colors.warning, 0.7),
    marginBottom: '1.5rem',
    height: '2.1rem',
    marginRight: '0.75rem',
    border: `1px solid ${colors.warning}`
  }
});
