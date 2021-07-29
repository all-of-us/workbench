! git clone https://github.com/all-of-us/workbench-snippets.git /tmp/workbench-snippets

! touch /tmp/workbench-snippets/should_be_ignored.png
! touch /tmp/workbench-snippets/should_be_ignored.csv
! touch /tmp/workbench-snippets/should_be_ignored.tsv
! touch /tmp/workbench-snippets/should_be_ignored.CSV
! touch /tmp/workbench-snippets/should_be_ignored.TSV
! touch /tmp/workbench-snippets/should_be_visible.r
! touch /tmp/workbench-snippets/should_be_visible.R
! touch /tmp/workbench-snippets/should_be_visible.py
! touch /tmp/workbench-snippets/should_be_visible.wdl
! touch /tmp/workbench-snippets/should_be_visible.sh
! touch /tmp/workbench-snippets/should_be_visible.md
! mkdir -p /tmp/workbench-snippets/some_diretory/
! touch /tmp/workbench-snippets/some_diretory/should_be_ignored.CSV
! touch /tmp/workbench-snippets/some_diretory/should_be_visible.sh

actual = !cd /tmp/workbench-snippets/; git status

assert(any('should_be_visible' in s for s in actual))
assert(not any('should_be_ignored' in s for s in actual))

print("success")
