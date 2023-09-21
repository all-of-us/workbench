import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  h1: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    lineHeight: '24px',
  },
  inputLabel: {
    color: colors.primary,
    fontSize: 14,
    fontWeight: 600,
    lineHeight: '22px',
    marginBottom: 6,
  },
  inputStyle: {
    width: 300,
    height: 40,
    fontSize: 14,
    marginRight: 20,
  },
  disabledInput: {
    backgroundColor: colorWithWhiteness(colors.disabled, 0.6),
  },
  dataAccessTier: {
    marginBottom: '1.35rem',
    display: 'grid',
    columnGap: '0.375rem',
    width: 459,
    gridTemplateColumns: 'fit-content(3rem) fit-content(15rem) 1fr',
    gridTemplateAreas: `"badge label available"
                        ". primary primary"`,
  },
  dataAccessText: {
    fontWeight: 500,
    color: colors.primary,
    lineHeight: '22px',
  },
  longInputContainerStyle: {
    width: 420,
    resize: 'both',
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
    alignItems: 'flexEnd',
  },
  fadebox: {
    margin: '1.5rem 0 0 3%',
    width: '95%',
    padding: '0 0.15rem',
  },
  verticalLine: {
    marginTop: '0.45rem',
    marginInlineStart: '0rem',
    width: '100%',
  },
  researchPurposeInfo: {
    fontWeight: 100,
    width: '80%',
    marginTop: '0.75rem',
    marginBottom: '0.45rem',
  },
  initialCreditsBox: {
    borderRadius: '0.6rem',
    height: '4.5rem',
    marginTop: '1.05rem',
    marginBottom: '2.55rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.disabled, 0.7),
  },
  updateSurveyButton: {
    textTransform: 'none',
    padding: 0,
    height: 'auto',
  },
  renewalBox: {
    borderRadius: 5,
    display: 'flex',
    alignItems: 'center',
    backgroundColor: colorWithWhiteness(colors.warning, 0.7),
    marginBottom: '2.25rem',
    height: '3.15rem',
    marginRight: '1.125rem',
    border: `1px solid ${colors.warning}`,
  },
  panel: { marginTop: '1.5rem', marginLeft: '1.5rem' },
  panelBody: { color: colors.primary, fontSize: '14px' },
});
