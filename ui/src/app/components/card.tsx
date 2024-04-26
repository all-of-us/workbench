import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withStyle } from 'app/utils';

const cardBorderColor = colorWithWhiteness(colors.dark, 0.6);

export const baseStyles = reactStyles({
  card: {
    margin: '0 1.5rem 1.5rem 0',
    borderRadius: '0.3rem',
    boxShadow: `0 0.1875rem 0.1875rem 0 ${cardBorderColor}`,
    backgroundColor: colors.white,
    border: `1px solid ${cardBorderColor}`,
    display: 'flex',
    flexDirection: 'column',
    padding: '1.5rem',
  },
});

export const styles = reactStyles({
  workspaceCard: {
    ...baseStyles.card,
    padding: '0px',
    minWidth: '300px',
    maxWidth: '300px',
    minHeight: '223px',
    maxHeight: '223px',
  },
  domainCard: {
    ...baseStyles.card,
    justifyContent: 'space-between',
    minWidth: '300px',
    maxWidth: '300px',
    minHeight: '223px',
    maxHeight: '223px',
  },
  cohortActionCard: {
    ...baseStyles.card,
    width: '30%',
    height: '18rem',
  },
  actionAuditCardBase: {
    ...baseStyles.card,
    width: 'auto',
    height: 'auto',
  },
});

export const WorkspaceCardBase = withStyle(styles.workspaceCard)('div');
export const DomainCardBase = withStyle(styles.domainCard)('div');
export const ActionCardBase = withStyle(styles.cohortActionCard)('div');
export const ActionAuditCardBase = withStyle(styles.actionAuditCardBase)('div');
