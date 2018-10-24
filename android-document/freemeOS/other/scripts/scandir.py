#!/usr/bin/python

# -*- coding:utf-8 -*-

from xml.etree import ElementTree as ET
import sys
import os

#reference
#https://docs.python.org/2/library/xml.etree.elementtree.html

def is_a_project(dirname, root_node):
    for project in root_node.findall('project'):
        project_path = project.get('path') or project.get('name')
        if project_path == dirname :
            return "full"
        if project_path.find(dirname) == 0:
            return "part"
    return "not"

# mainifestxml: should be android's repo mainifest.xml 
# rootdir: the root dir of android source code provided by vendor in which .repo/.git is deleted
# path: a interator variable
def walkdir(root_node, rootdir, path):
    for subdir in os.listdir(path):
        subdir_full = os.path.join(path, subdir)
        if os.path.isdir(subdir_full):
            rel_path = os.path.relpath(subdir_full, rootdir)
            ret = is_a_project(rel_path, root_node)
            if ret == "full":
                pass
            elif ret == "part":
                walkdir(root_node, rootdir, subdir_full)
            else:
                warning("warning: <", rel_path, "> has no git project")
    return

def walkdir_withlevel(path, level_max, level, outlist):
    if level >= level_max:
        return

    for subdir in os.listdir(path):
        subdir_full = os.path.join(path, subdir)
        if os.path.isdir(subdir_full):
            if level == level_max-1:
                outlist.append(subdir_full)
            walkdir_withlevel(subdir_full, level_max, level+1, outlist)
    return

def create_tyd_project(rootdir, level_max = 3):
    tyddir = os.path.join(rootdir, 'tyd')
    if os.path.isdir(tyddir) is False:
        print "Error: please provide an freemeos android code with `tyd' dir"
        exit(-1)
    tyd_list = []
    walkdir_withlevel(tyddir, level_max, 0, tyd_list)
    for i in tyd_list:
        s = i.split('/')
        print "<project path=\"%s\" name=\"tyd/v2.84_droi6737m_65_m0/%s/%s\" \>" \
            %(os.path.relpath(i, rootdir), s[-2], s[-1])

# descripton:
#   diff the android code directory with an repo mainifest.xml, to figure out the
#   directories not specified in mainifest.xml. So, we need create git
#   project in gerrit, add add them in mainifest.xml.

def diffdir_with_mainifestxml(androiddir, mainifestxml):
    tree = ET.parse(mainifestxml)
    root = tree.getroot()
    walkdir(root, androiddir, androiddir)

def createxml(root_node, prefix=''):
    template_header = '''<?xml version="1.0" encoding="UTF-8"?>
<manifest>

  <include name="base/base.xml"/>

'''
    template_tail = '''
</manifest>
'''
    outfile = file("droi.xml", 'w')
    outfile.write(template_header)
    for project in root_node.findall('project'):
        if project.get('has') != 'false':
            p_path = project.get('path')
            p_name = project.get('name')
            if p_path is None:
                warning("warning: no attr <", p_name, ">")
                p_path = p_name;
            #droi/freemeos/
            item = '  <project path="' +  p_path + '" name="' + prefix + p_name + '" />\n'
            outfile.write(item)

    outfile.write(template_tail)
    outfile.close()

def createlndir(root_node, dirsrc, dirdes):
    template_header = '''#!/bin/bash\n'''
    outfile = file("lndir.sh", 'w')
    outfile.write(template_header)
    for project in root_node.findall('project'):
        if project.get('has') != 'false':
            p = project.get("path")
            item = 'ln -s ' +  dirsrc + '/' + p + '/.git ' + dirdes +'/'+ p + '/.git\n'
            outfile.write(item)

    outfile.close()

def warning(*args):
    print '\033[1;31;40m',
    for i in args:
        print i,
    print '\033[0m'

# descripton:
#   diff the android code directory with an repo mainifest.xml, to figure out the
#   git projects not exsited in this android code. so we need remove those items in
#   manifest.xml
def parsexml(ddir, mainifestxml):
    project_list = file("project.list", 'w')
    project_gerrit = file("project.gerrit", 'w')
    tree = ET.parse(mainifestxml)
    root = tree.getroot()
    for project in root.iter('project'):
        p_path = project.get('path')
        p_name = project.get('name')
        if p_path is None:
            warning("warning: no attr <", p_name, ">")
            p_path = p_name;

        fullpath = ddir+'/'+p_path
        if os.path.exists(fullpath) is False:
            warning("warning: <", p_path, "> is not existed")
            project.set('has', 'false')
        else:
            project_list.write(fullpath+'\n')
            project_gerrit.write(p_name+'\n')
            ##modify the name attribute
            #project.set('name', 'droi/freemeos/'+p_name)
            ##add a new attribute for filter when write new xml

    project_list.close()
    project_gerrit.close()
    return root

def scandir(androiddir, listfile):
    f = file(listfile)
    #l = " "
    #while l != '':
    #    l = f.readline()
    #    print l
    line_lists = f.readlines()
    dir_lists = [ i[:-1] for i in line_lists]
    dir_rel_lists = []
    for p in dir_lists:
        fullpath = androiddir+'/'+p
        if os.path.exists(fullpath) is False:
            print "warning:", fullpath, "is not existed"
        else:
            dir_rel_lists.append(fullpath)
    return dir_rel_lists;

def doCreateGit(git_lists):
    for i in git_lists:
        print i

def showhelp():
    usage = '''
    <Usage>
        scandir.py -f <android-code-dir> <mainifest.xml>
            create the status file of the difference between the dir and xml
        scandir.py -l <mainifest.xml> <dir-src> <dir-des>
            create link shell script
        scandir.py -tyd <freeme-os-code-dir>
            find the `tyd' dir, and print the part of manifest
    '''
    print usage

if __name__ == "__main__":
    if len(sys.argv) < 2:
        showhelp()
        exit(-1)
    elif sys.argv[1] == '-l':
        if len(sys.argv) != 5:
            showhelp()
            exit(-1)
        mainifest_xml = sys.argv[2]
        dsrc = sys.argv[3]
        ddes = sys.argv[4]
        createlndir(ET.parse(mainifest_xml).getroot(), dsrc, ddes)
    elif sys.argv[1] == '-c':
        if len(sys.argv) < 4:
            showhelp()
            exit(-1)
        mainifest_xml = sys.argv[2]
        prefix = sys.argv[3]
        createxml(ET.parse(mainifest_xml).getroot(), prefix)
    elif sys.argv[1] == '-tyd':
        ddir = sys.argv[2]
        create_tyd_project(ddir)
    elif sys.argv[1] == '-f':
        if len(sys.argv) != 4:
            showhelp()
            exit(-1)
        ddir = sys.argv[2]
        mainifest_xml = sys.argv[3]
        print "####  check extra git projects in mainifest.xml"
        root = parsexml(ddir, mainifest_xml)
        createxml(root)
        print "\n####  check extra directories in ", ddir
        diffdir_with_mainifestxml(ddir, mainifest_xml)
