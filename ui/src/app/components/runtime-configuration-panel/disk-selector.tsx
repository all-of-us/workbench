import * as React from 'react';

import { StyledExternalLink } from 'app/components/buttons';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ComputeType } from 'app/utils/machines';
import { DiskConfig } from 'app/utils/runtime-utils';
import { supportUrls } from 'app/utils/zendesk';

import { PersistentDiskSelector } from './persistent-disk-selector';
import { StandardDiskSelector } from './standard-disk-selector';

interface Props {
  diskConfig: DiskConfig;
  onChange: (c: Partial<DiskConfig>) => void;
  disabled: boolean;
  computeType: string | null;
}
export const DiskSelector = ({
  diskConfig,
  onChange,
  disabled,
  computeType,
}: Props) => {
  return (
    <FlexColumn
      style={{ ...styles.controlSection, gap: '11px', marginTop: '11px' }}
    >
      <FlexColumn>
        {computeType === ComputeType.Dataproc && (
          <span style={{ ...styles.sectionTitle, marginBottom: 0 }}>
            Master Node Configuration
          </span>
        )}
        <FlexRow style={{ gap: '8px' }}>
          <span style={{ ...styles.sectionTitle, marginBottom: 0 }}>
            Storage disk options
          </span>
          <StyledExternalLink href={supportUrls.persistentDisk}>
            View documentation
          </StyledExternalLink>
        </FlexRow>
      </FlexColumn>
      {computeType === ComputeType.Standard ? (
        <PersistentDiskSelector {...{ diskConfig, onChange, disabled }} />
      ) : (
        <StandardDiskSelector {...{ diskConfig, onChange, disabled }} />
      )}
    </FlexColumn>
  );
};
