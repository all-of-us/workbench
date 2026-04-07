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
  helperText: {
    fontSize: 12,
    color: '#666',
    marginTop: 8,
  },
});

interface Props {
  folders: string[];
  onContinue: (folders: string[]) => Promise<void>;
  workspaceName?: string;
  onBack?: () => void;
  onClose?: () => void;
}

export const FolderSelection = ({
  folders,
  onContinue,
  workspaceName,
  onBack,
  onClose,
}: Props) => {
  const [selectedFolders, setSelectedFolders] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const toggleFolder = (folder: string) => {
    if (isLoading) {
      return;
    } // prevent interaction while loading

    if (selectedFolders.includes(folder)) {
      setSelectedFolders(selectedFolders.filter((f) => f !== folder));
    } else {
      setSelectedFolders([...selectedFolders, folder]);
    }
  };

  const handleContinue = async () => {
    if (isLoading) {
      return;
    } // prevent double click

    setIsLoading(true);
    try {
      await onContinue(selectedFolders);
    } catch (e: any) {
      setError(e.message || 'Failed to start migration');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <FlexColumn>
      {/* Header */}
      <FlexRow style={styles.header}>
        <div style={styles.title}>
          Entire bucket will be migrated
          {workspaceName ? ` – ${workspaceName}` : ''}
        </div>

        {onClose && !isLoading && <CloseButton onClose={onClose} />}
      </FlexRow>

      {/* Folder List */}
      {folders.map((folder) => (
        <FlexRow key={folder} style={styles.folderRow}>
          <input
            type='checkbox'
            checked={selectedFolders.includes(folder)}
            onChange={() => toggleFolder(folder)}
            disabled={isLoading}
          />
          <span>{folder}</span>
        </FlexRow>
      ))}

      {/* Footer Buttons */}
      <FlexRow style={styles.footer}>
        {onBack && (
          <Button type='secondary' onClick={onBack} disabled={isLoading}>
            Back
          </Button>
        )}

        <Button disabled={isLoading} onClick={handleContinue}>
          {isLoading ? 'Migrating...' : 'Continue Migration'}
        </Button>
      </FlexRow>

      {error && <div style={{ color: 'red', marginTop: 8 }}>{error}</div>}

      {/* Helper Text */}
      {isLoading && (
        <div style={styles.helperText}>Starting migration... please wait</div>
      )}
    </FlexColumn>
  );
};
