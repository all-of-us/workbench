import os
import re

disk_file = os.environ.get('HOME') + '/disk_size.txt'
assert os.path.exists(disk_file), f"expected file to persist on disk @ {disk_file}"

def parse_gb(str_gb):
    m = re.search('(\d+)G', str_gb)
    assert m, f"Input value is misformatted: '{str_gb}'"
    return int(m.group(1))

with open(disk_file, 'r') as f:
    txt = f.read()
    old_gb = parse_gb(txt)

new_gb_str = ! df -h --output=size $HOME | sed 1d
new_gb = parse_gb(new_gb_str[0])

# The test increases the disk size by 10GB. Use a more conservative value here
# to account for rounding and unavailable space.
assert new_gb - old_gb > 7, f"expected disk size to increase by at least 7GB, got {old_gb} -> {new_gb} GB"

print('success')
