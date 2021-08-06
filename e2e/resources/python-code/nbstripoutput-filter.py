# Any git repo will do here, workbench-snippets is small and relevant to AoU's
# notebook images.
! [ -d /tmp/workbench-snippets/ ] || git clone https://github.com/all-of-us/workbench-snippets /tmp/workbench-snippets

import json

nb = {
  "cells": [
    {
      "cell_type": "code",
      "execution_count": 1,
      "id": "97f5ae46",
      "metadata": {},
      "outputs": [
        {
          "name": "stdout",
          "output_type": "stream",
          "text": [
            "my outputs\r\n"
          ]
        }
      ],
      "source": [
        "!./script.sh my inputs"
      ]
    }
  ],
  "metadata": {},
  "nbformat": 4,
  "nbformat_minor": 5
}

with open('/tmp/workbench-snippets/notebook.ipynb', 'w') as f:
    json.dump(nb, f)

! cd /tmp/workbench-snippets; git add notebook.ipynb
lines = !cd /tmp/workbench-snippets/; git diff --staged
actual = '\n'.join(lines)

assert 'my inputs' in actual, f"notebook should still contain source, got {actual}"
assert 'my outputs' not in actual, f"notebook should omit outputs, got {actual}"

! cd /tmp/workbench-snippets/; git reset HEAD; git clean -f

print("success")
