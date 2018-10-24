#!/usr/bin/python

# encoding: UTF-8

"""
Usage: gen patch-dir temp-dir > w.sh

then, run "bash w.sh"
"""

import sys
import os
import re

def filter_patch_tarbar(dir="."):
    lists=[]
    p = re.compile(r'^ALPS\d+\(.*_P(\d+)\).tar.gz')
    for fname in os.listdir(dir):
        m = p.match(fname)
        if m:
            p_num = int(m.group(1))
            p_name = os.path.join(dir, m.group(0))
            lists.append((p_num, p_name))
    lists.sort(key=lambda tup: tup[0]) #, reverse=True
    return lists

def generate_scripts(file_lists, tempdir="pwork"):
    print("#!/bin/bash")
    for i in file_lists:
        print("repopatch.sh mtk \"%s\" %s" % (i[1], tempdir))

def main(argv):
    if len(argv) < 2:
        print(__doc__)
        sys.exit(1)

    patchdir = argv[0]
    tempdir = argv[1]
    lists = filter_patch_tarbar(patchdir)
    generate_scripts(lists, tempdir)

if __name__ == "__main__":
    main(sys.argv[1:])