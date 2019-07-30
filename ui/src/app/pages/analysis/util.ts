export function dropNotebookFileSuffix(filename: string) {
  if (filename.endsWith('.ipynb')) {
    filename = filename.substring(0, filename.length - 6);
  }

  return filename;
}

export function appendNotebookFileSuffix(filename: string) {
  if (!filename.endsWith('.ipynb')) {
    filename = filename + '.ipynb';
  }

  return filename;
}