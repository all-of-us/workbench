# Any git repo will do here, workbench-snippets is small and relevant to AoU's
# notebook images.
! [ -d /tmp/workbench-snippets/ ] || git clone https://github.com/all-of-us/workbench-snippets /tmp/workbench-snippets

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
! touch /tmp/workbench-snippets/should_be_visible.ipynb
! touch /tmp/workbench-snippets/should_be_visible.rst
! touch /tmp/workbench-snippets/LICENSE.should_be_visible
! mkdir -p /tmp/workbench-snippets/some_directory/
! touch /tmp/workbench-snippets/some_directory/should_be_ignored.CSV
! touch /tmp/workbench-snippets/some_directory/should_be_visible.sh
! cd /tmp/workbench-snippets/; git add some_directory

actual = !cd /tmp/workbench-snippets/; git status --porcelain

visible = list(filter(lambda s : 'should_be_visible' in s, actual))
assert len(visible) == 10, f"wrong number of visible files ({len(visible)}): {visible}"

ignored = list(filter(lambda s : 'should_be_ignored' in s, actual))
assert len(ignored) == 0, f"found {len(ignored)} files which should have been ignored: {ignored}"

! cd /tmp/workbench-snippets/; git reset HEAD; git clean -f

print("success")
