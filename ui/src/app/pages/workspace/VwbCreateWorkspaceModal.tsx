import * as React from 'react';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import colors, { addOpacity } from 'app/styles/colors';

interface Props {
  onClose: () => void;
}

export const VwbCreateWorkspaceModal = ({ onClose }: Props) => {
  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        background: addOpacity(colors.black, 0.4).toString(),
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
    >
      <FlexColumn
        style={{
          background: colors.white,
          borderRadius: '12px',
          padding: '2rem',
          width: '420px',
        }}
      >
        {/* TITLE */}
        <Header style={{ fontSize: '20px', fontWeight: 600 }}>
          Researcher Workbench is moving
        </Header>

        {/* TEXT */}
        <p style={{ marginTop: '1rem', color: colors.primary }}>
          New workspaces can only be created in Researcher Workbench 2.0
        </p>

        {/* ACTIONS */}
        <FlexRow
          style={{
            justifyContent: 'flex-end',
            marginTop: '1.5rem',
            gap: '1rem',
          }}
        >
          {/* CANCEL */}
          <Button
            style={{
              background: colors.white,
              color: colors.primary,
              border: `1px solid ${colors.primary}`,
            }}
            onClick={onClose}
          >
            Cancel
          </Button>

          {/* PRIMARY CTA */}
          <Button
            style={{
              background: colors.accent,
              color: colors.white,
            }}
            onClick={() => {
              window.open(environment.vwbUiUrl, '_blank');
            }}
          >
            Open Verily Workbench
          </Button>
        </FlexRow>
      </FlexColumn>
    </div>
  );
};
