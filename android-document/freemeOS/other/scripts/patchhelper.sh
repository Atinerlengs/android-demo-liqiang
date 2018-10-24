#!/bin/bash

# FIXME: modify the lists as needed.
projects=(build device/droi device/mediatek kernel-3.18 system vendor)

function abs_path() {
    readlink -f $1
}

function convert() {
    local name=$1
    echo $name | tr '/' "_"
}

function create_patch() {
    local outdir=$1
    local HERE=$(pwd)
    mkdir -p ${outdir}
    for i in ${projects[*]}; do
        cd $i
        git add . && git commit -m "driver/patch: merge from customer"
        git format-patch -1
        local patchname=$(basename $(find . -maxdepth 1 -name "0001*.patch"))
        cd $HERE
        mv $i/$patchname ${outdir}/$(convert $i)_$patchname
    done
}

function apply_patch() {
    local srcdir=$(abs_path $1)
    local HERE=$(pwd)
    mkdir -p ${srcdir}
    for i in ${projects[*]}; do
        local patchname=$(basename $(find $srcdir -maxdepth 1 -name "$(convert $i)*.patch"))
        echo "apply: ${srcdir}/$patchname"
        cd $i
        git apply ${srcdir}/$patchname
        cd $HERE
    done
}


action=$1
shift

case $action in
    c|create)
        create_patch $1
        ;;
    a|apply)
        apply_patch $1
        ;;
esac
