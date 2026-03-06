import * as React from 'react';
import { useState } from 'react';

import { Button, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 600,
  },
  folderRow: {
    padding: '8px 0',
    borderBottom: '1px solid #eee',
    alignItems: 'center',
    gap: 8,
  },
  footer: {
    marginTop: 20,
    gap: 12,
  },
});

interface Props {
  onContinue: (folders: string[]) => void;
  workspaceName?: string;
  onBack?: () => void;
  onClose?: () => void;
}

export const FolderSelection = ({
  onContinue,
  workspaceName,
  onBack,
  onClose,
}: Props) => {
  // TODO: Replace dummy folder list with GET /migration/bucket-contents API
  const dummyFolders = ['notebooks/', 'data/', 'results/', 'analysis/'];

  const [selectedFolders, setSelectedFolders] = useState<string[]>([]);

  const toggleFolder = (folder: string) => {
    if (selectedFolders.includes(folder)) {
      setSelectedFolders(selectedFolders.filter((f) => f !== folder));
    } else {
      setSelectedFolders([...selectedFolders, folder]);
    }
  };

  return (
    <FlexColumn>
      {/* Header */}
      <FlexRow style={styles.header}>
        <div style={styles.title}>
          Select folders to migrate
          {workspaceName ? ` – ${workspaceName}` : ''}
        </div>

        {onClose && <CloseButton onClose={onClose} />}
      </FlexRow>

      {/* Folder List */}
      {dummyFolders.map((folder) => (
        <FlexRow key={folder} style={styles.folderRow}>
          <input
            type='checkbox'
            checked={selectedFolders.includes(folder)}
            onChange={() => toggleFolder(folder)}
          />
          <span>{folder}</span>
        </FlexRow>
      ))}

      {/* Footer Buttons */}
      <FlexRow style={styles.footer}>
        {onBack && (
          <Button type='secondary' onClick={onBack}>
            Back
          </Button>
        )}

        <Button
          disabled={selectedFolders.length === 0}
          onClick={() => onContinue(selectedFolders)}
        >
          Continue Migration
        </Button>
      </FlexRow>
    </FlexColumn>
  );
};
