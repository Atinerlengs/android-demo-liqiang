#!/usr/bin/python3

''' BSD 3-Clause License — but if it was useful to you, you may tell me :)
Copyright (c) 2016-2017, Alexandre `Alex131089` Levavasseur
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the copyright holder nor the names of its
      contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
'''

import sys, io
from struct import unpack
from uuid import UUID

# Utils
try:
    # See https://gist.github.com/Alex131089/b3a23c9461e95433387f285f6e0860ca
    # and put guids.py in the same location
    from guids import guids
except:
	guids = {}
# https://stackoverflow.com/questions/1094841/reusable-library-to-get-human-readable-version-of-file-size
def sizeof_fmt(num, suffix='B'):
    for unit in ['','K','M','G','T','P','E','Z']:
        if abs(num) < 1024.0:
                return "%g %s%s" % (num, unit, suffix)
        num /= 1024.0
    return "%g %s%s" % (num, 'Y', suffix)


# Help
if len(sys.argv) < 2:
    print('Usage: ./{} <gpt.bin>'.format(sys.argv[0]))
    exit(-1)

header_len = 512

with open(sys.argv[1], "rb") as binary_file:
    # Read the whole file at once
    data = binary_file.read()

h_magic, h_revision, h_size, h_checksum, _, \
h_current_lba, h_backup_lba, h_fisrt_usable_lba, h_last_usable_lba, \
h_guid, h_lba_array_entries_addr, h_partition_entry_number, h_partition_entry_size, h_partition_arry_entries_crc, _ \
= unpack('<8sLLLLQQQQ16sQLLL420s', data[:header_len])
#header = unpack('<8sLLLLQQQQ16sQLLL', data[:header_len])

partition_entries = [data[i:i+h_partition_entry_size] for i in range(header_len, len(data)-header_len, h_partition_entry_size)]

print ('{:<20} {:<8} {:8}\t{:16} {:36}\t{}'.format('Partition name', 'Lba addr', 'Count', 'Size', 'GPT Type GUID', 'Partition UUID'))
print ('{:-<20} {:-<8} {:-<8}\t{:-<16} {:-<36}\t{:-<36}'.format('', '', '', '', '', ''))
for entry in partition_entries:
    entry = io.BytesIO(entry)
    #size = unpack('<16s16sQQQ72s', entry)

    puid = UUID(bytes_le=entry.read(0x10))
    puid_name = guids[puid] if puid in guids else str(puid)
    #print(puid)

    guid = UUID(bytes_le=entry.read(0x10))
    guid_name = guids[guid] if guid in guids else str(guid)
    #print(guid)

    first_lba, last_lba, attr = unpack('<QQQ', entry.read(24))

    lba_count = last_lba - first_lba + 1
    size_pp = sizeof_fmt(lba_count * 512)

    name = entry.read(0x48).decode('utf_16_le').strip('\0')
    #print(name)

    print ('{:<20} {:<8} {:8}\t{:16} {:36}\t{}'.format(name, first_lba, lba_count, size_pp, guid_name, puid_name))

    end = entry.read()
    if len(end) > 0:
        print('Extra data:', end)

    if name == "flashinfo":
        break;
