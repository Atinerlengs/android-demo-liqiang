#!/bin/bash

function filter() {
    local file=$1
    sed -n 's/!\[[a-z]\+\](\([^()]\+\))/\1/p' ${file}
}

function mvfiles() {
    local file=$1
    local dir=$2
    for i in $(filter $file)
    do
        echo $1
        mv $i $dir
    done
}

function refactor_pic_dir() {
    local file=$1
    local dir=$2
    sed -i 's:!\[[a-z]\+\](\([^/]\+\)/\([^)]\+\)):!\[gerrit\]('$dir'/\2):g' ${file}
}

action=$1
shift
case "$action" in
    mv)
        mvfiles "$@"
        ;;
    refactor)
        refactor_pic_dir "$@"
        ;;
    *)
        echo "bad $action"
        ;;
esac
