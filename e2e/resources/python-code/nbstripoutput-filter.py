# Any git repo will do here, workbench-snippets is small and relevant to AoU's
# notebook images.
! [ -d /tmp/workbench-snippets/ ] || git clone https://github.com/all-of-us/workbench-snippets /tmp/workbench-snippets

# Jupyter is very finnicky with input formatting, even in Markdown mode.
# Dictionaries and triple quotes both fail due to Markdown auto-indent.
nb = ('{\n'
'  "cells": [\n'
'    {\n'
'      "cell_type": "code",\n'
'      "execution_count": 1,\n'
'      "id": "97f5ae46",\n'
'      "metadata": {},\n'
'      "outputs": [\n'
'        {\n'
'          "name": "stdout",\n'
'          "output_type": "stream",\n'
'          "text": [\n'
'            "my outputs\\r\\n"\n'
'          ]\n'
'        }\n'
'      ],\n'
'      "source": [\n'
'        "!./script.sh my inputs"\n'
'      ]\n'
'    }\n'
'  ],\n'
'  "metadata": {},\n'
'  "nbformat": 4,\n'
'  "nbformat_minor": 5\n'
'}')

f = open('/tmp/workbench-snippets/notebook.ipynb', 'w')
f.write(nb)
f.close()

! cd /tmp/workbench-snippets; git add notebook.ipynb
lines = !cd /tmp/workbench-snippets/; git diff --staged
actual = '\n'.join(lines)

assert 'my inputs' in actual, f"notebook should still contain source, got {actual}"
assert 'my outputs' not in actual, f"notebook should omit outputs, got {actual}"

! cd /tmp/workbench-snippets/; git reset HEAD; git clean -f

print("success")
