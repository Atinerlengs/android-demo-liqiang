#!/bin/bash

WORKDIR=$(pwd)
CHANGLOGDIR=device/mediatek/changelogs/

function logi() {
    # yellow
    local color=$'\E'"[0;33m"
    local color_reset=$'\E'"[00m"
    echo "$color$@$color_reset"
}

function logw() {
    # purple
    local color=$'\E'"[0;35m"
    local color_reset=$'\E'"[00m"
    echo "$color$@$color_reset" 1>&2
}

function loge() {
    # red
    local color=$'\E'"[0;31m"
    local color_reset=$'\E'"[00m"
    echo "$color$@$color_reset" 1>&2
}

function get_delete_files() {
    local file=$1
    sed -n "s/^delete\s\+\(.*\)$/\1/p" ${file}
}

read -r -d '' filter_script << EOM
status=\$(git status -s -z)
if [ ! -z "\${status}" ];then
    echo "\$REPO_PATH"
fi
EOM

function get_changed_projects() {
    _fiter_script=$filter_script repo forall -c 'bash -c "$_fiter_script"'
}

function get_patch_id() {
    local patchname=$1
    local patchid=$(echo ${patchname} | sed -n "s/.*\(ALPS[0-9]\+\)(.*_\(P[0-9]\+\)).*/\2_\1/p")
    echo ${patchid:-${patchname}}
}

function commit_project() {
    local patch_version=$1
    shift
    patch=${patch_version} && \
        repo forall "$@" -pc 'git add . && git commit -m "[patch/apply] ${patch}"'
}

function get_last_commitid() {
    git log --pretty=format:"%H" -1
}

function get_abs_dir() {
    local file=$1
    dirname $(readlink -f ${file})
}

#--------------------------------------------------------------------------------
# change-log file format
function create_changelog() {
    local patchfullname=$1
    local commit_array=$2
    local logdir=$3
    local patchlog=$(get_patch_id ${patchfullname}).txt

    if [ -f "${patchlog}" ]; then
        logw "warning: ${logdir}/${patchlog} already exists, recreate it!"
    else
        echo "log: create ${logdir}/${patchlog}"
    fi

    cd ${logdir}

    echo "#patchname:${patchfullname}" > ${patchlog}
    for F in $commit_array
    do
        echo $F >> ${patchlog}
    done

    cd ${WORKDIR}
}

function get_commits_from_changelog() {
    local _changelog=$1
    grep -v "^#" ${_changelog}
}

function get_patchname_from_changelog() {
    local _changelog=$1
    sed -n "s/^#patchname:\(.*\)/\1/p" ${_changelog}
}

function commit_changelog() {
    local _changelog=$1
    local patchlog=$(basename $_changelog)

    if [ ! -d "${CHANGLOGDIR}" ]; then
        logw "warning: \$CHANGLOGDIR ${CHANGLOGDIR} not existed, create it at first"
        mkdir -p ${CHANGLOGDIR}
    fi

    cp ${_changelog} ${CHANGLOGDIR}
    cd ${CHANGLOGDIR}
    git add ${patchlog} && git commit -q -m "[patch/log] add ${patchlog}"
    cd ${WORKDIR}
}
#--------------------------------------------------------------------------------
function loop_commit_modified_gits() {
    local patchfullname=$1
    local dirname=$2

    local commit_array
    commit_array=""

    logi "loop modified projects..."
    for F in $(get_changed_projects)
    do
        cd $F
        echo "patch $F"
        git add -u; git add .
        git commit -q -m "[patch/apply] ${patchfullname}"
        if [ "$?" -ne 0 ]; then
            logw "warning: $F commit failed, it may be clean, please fix it"
        else
            commit_array="$commit_array $F:$(get_last_commitid)"
        fi
        cd ${WORKDIR}
    done

    logi "create change log file"
    local logdir=${dirname}/changelogs
    mkdir -p ${logdir}
    create_changelog ${patchfullname} "${commit_array}" ${logdir}

    logi "commit change log file"
    local patchid=$(get_patch_id ${patchfullname})
    commit_changelog ${logdir}/${patchid}.txt
}

function apply_patch_from_tarball() {
    local patchfile=$1
    local dirname=$2
    local _name=$(basename $patchfile)
    local patchfullname=${_name%.tar.gz}
    local patchid=$(get_patch_id ${patchfullname})

    mkdir -p ${dirname}/${patchid}

    logi "tar patch files... "
    tar xf ${patchfile} -C ${dirname}/${patchid}

    logi "override patch files... "
    cp -frp ${dirname}/${patchid}/alps/. ${WORKDIR}
    for F in $(get_delete_files ${dirname}/${patchid}/patch_list.txt)
    do
        rm -rf $F
        echo "delete $F"
    done
    loop_commit_modified_gits $patchfullname $dirname
}

function apply_patch_from_changelog() {
    local _changelog=$1
    local patchfullname=$(get_patchname_from_changelog $_changelog)
    local _src_cmt_array=($(get_commits_from_changelog $_changelog))
    local pick_no_conflicts="true"
    local commit_array
    commit_array=""
    logi "loop patched projects..."
    for i in ${_src_cmt_array[*]}
    do
        local _project=$(echo $i | awk 'BEGIN {FS=":"} {print $1}')
        local _commitid=$(echo $i | awk 'BEGIN {FS=":"} {print $2}')
        cd ${_project}
        echo "pick ${_commitid} from ${_project}"
        git cherry-pick -n ${_commitid}

        if [ "$?" -ne 0 ]; then
            #TODO: how to continue?
            loge "error: <${_project}> pick conflict, please fix it later, now we just skip"
            commit_array="$commit_array ${_project}:----------------------------------------"
            pick_no_conflicts="false"
        else
            echo "picked!"
            git commit -q -m "[patch/apply] ${patchfullname}"
            commit_array="$commit_array $_project:$(get_last_commitid)"
        fi

        cd ${WORKDIR}
    done

    logi "create change log file"
    local _srclogdir=$(dirname ${_changelog})
    local logdir=${_srclogdir}_new
    mkdir -p ${logdir}
    create_changelog ${patchfullname} "${commit_array}" ${logdir}

    logi "commit change log file"
    if [ "${pick_no_conflicts}" = "true" ]; then
        commit_changelog ${logdir}/$(get_patch_id $patchfullname).txt
    else
        logw "warning: run 'logupdate' to update commit chang log file after you fix pick confilct"
    fi
}

function update_commits_of_changelog() {
    local _changelog=$1
    local _src_cmt_lists=($(get_commits_from_changelog $_changelog))
    #local patchfullname=$(get_patchname_from_changelog $_changelog)

    #local _new_cmt_lists
    #_new_cmt_lists=""
    logi "loop patched projects..."
    for i in ${_src_cmt_lists[*]}
    do
        local _project=$(echo $i | awk 'BEGIN {FS=":"} {print $1}')
        local _commitid=$(echo $i | awk 'BEGIN {FS=":"} {print $2}')
        cd ${_project}
        local commitid=$(get_last_commitid)
        #_new_cmt_lists="${_new_cmt_lists} ${_project}:${_commitid}"
        cd ${WORKDIR}

        # FIXME: check commit message format
        if [ "${commitid}" != "${_commitid}" ]; then
            logw "warning: update ${_project}, ${_commitid} -> ${commitid}"
            sed -i "s@\(.*\)"${_commitid}"@\1${commitid}@" ${_changelog}
        fi
    done
}

function clean_files() {
    rm -rf "$@"
}

function repoclean() {
    color=$'\E'"[0;33m" color_reset=$'\E'"[00m" \
    repo forall -c \
    'echo "${color}$REPO_PATH ($REPO_REMOTE)${color_reset}"; git clean -fd ; git reset --hard ;'
}

_dirpath=$(dirname $0)
_scandir_py=${_dirpath:-"."}/scandir.py
function scandir() {
    local manifest=$1
    if [ ! -f "$_scandir_py" ]; then
        loge "error: cannot find scandir.py"
        exit 1
    fi

    if [ ! -f "$manifest" ]; then
        loge "error: $manifest is not exsited!"
        exit 1
    fi

    local _workdir
    if [ -z "$2" ]; then
        _workdir=$(pwd)
    else
        _workdir=$(readlink -f $2)
    fi
    if [ ! -d "$_workdir" ]; then
        loge "error: $2 is not a directory!"
        exit 1
    fi

    cd ${_workdir}
    python $_scandir_py -f . $manifest
    cd ${WORKDIR}
}

function clean_projects_subgits() {
    local projects_file=$1
    local _workdir=$(get_abs_dir ${projects_file})
    local git_lists=($(cat $projects_file))
    for p in ${git_lists[*]}
    do
        if [ -d "$_workdir/$p/.git" ]; then
            echo "rm -rf $_workdir/$p/.git"
            rm -rf $_workdir/$p/.git
        else
            echo "warning: $_workdir/$p/.git not exsited!"
        fi
    done
}

function clean_projects() {
    local projects_file=$1
    local _workdir=$(get_abs_dir ${projects_file})
    local _lists=($(cat $projects_file))
    for p in ${_lists[*]}
    do
        if [ -d "$_workdir/$p" ]; then
            rm -rf $_workdir/$p
        else
            echo "warning: $_workdir/$p not exsited!"
        fi
    done
}

function move_projects_subgits() {
    local projects_file=$1
    local des_dir=$2
    local src_dir=$(get_abs_dir ${projects_file})
    local git_lists=($(cat $projects_file))
    for p in ${git_lists[*]}
    do
        if [ -d "${src_dir}/$p/.git" -a -d "${des_dir}/$p" ]; then
            mv ${src_dir}/$p/.git ${des_dir}/$p/
        else
            if [[ $p/ == *${CHANGLOGDIR}* ]];then
                continue
            fi
            if [ ! -d "${src_dir}/$p/.git" ]; then
                logw "warning: <${src_dir}/$p/.git> not exsited!"
            fi
            if [ ! -d "${des_dir}/$p" ]; then
                logw "warning: <${des_dir}/$p> not exsited!"
            fi
        fi
    done
}

function skip_projects() {
cat <<EOF
./device/asus/
./device/google/
./device/htc/
./device/huawei/
./device/intel/
./device/lge/
./device/linaro/
./device/moto/
./prebuilts/android-emulator/
./prebuilts/qemu-kernel/
EOF
}

function apply_patch_from_mtk_repo() {
    local mtk_repo_xml=$(readlink -f ${1})
    local mtk_repo_dir=$2
    local droi_repo_xml=$(readlink -f ${3})
    local droi_repo_dir=$4
    local tmp_dir=$5

    # clean gits under mtk-repo
    logi ">>> create project.list in ${mtk_repo_dir}"
    scandir ${mtk_repo_xml} ${mtk_repo_dir} 1> /dev/null
    if [ "$?" -ne 0 ]; then
        logw "fail: cannot create project.list in ${mtk_repo_dir}"
        exit 1
    fi
    if [ -d ${tmp_dir}/back_dot_repo ]; then
        loge "fail: ${tmp_dir}/back_dot_repo is already exsited. please check and be careful !"
        exit -1
    fi

    logi ">>> move .repo to ${tmp_dir}/back_dot_repo"
    mkdir -p ${tmp_dir}
    mv ${mtk_repo_dir}/.repo ${tmp_dir}/back_dot_repo

    logi ">>> clean sub gits in ${mtk_repo_dir}"
    clean_projects_subgits ${mtk_repo_dir}/project.list

    echo
    logi ">>> create project.list in ${droi_repo_dir}"
    scandir ${droi_repo_xml} ${droi_repo_dir} 1> /dev/null
    if [ "$?" -ne 0 ]; then
        logw "fail: cannot create project.list in ${droi_repo_dir}"
        exit 1
    fi

    logi ">>> move .repo and sub gits from ${droi_repo_dir}"
    mv ${droi_repo_dir}/.repo ${mtk_repo_dir}
    move_projects_subgits ${droi_repo_dir}/project.list ${mtk_repo_dir}
    logi ">>> now $mtk_repo_dir has become a droi repo!"

    # fix repo: 1) move changelogs 2) restore .gitignore 3) skip some projects. 
    if [ -d "${droi_repo_dir}/${CHANGLOGDIR}" ]; then
        cp -frp ${droi_repo_dir}/${CHANGLOGDIR} $(dirname ${mtk_repo_dir}/${CHANGLOGDIR})
    fi
    if [ ! -f $(dirname ${mtk_repo_dir}/${CHANGLOGDIR})/.gitignore ];then
        git -C $(dirname ${mtk_repo_dir}/${CHANGLOGDIR}) checkout .gitignore
    fi
    if [ ! -f ${mtk_repo_dir}/device/.gitignore ];then
        git -C ${mtk_repo_dir}/device checkout .gitignore
    fi
    if [ -f ${mtk_repo_dir}/project.skip ]; then
        clean_projects ${mtk_repo_dir}/project.skip
    else
        logw "should you remove the following projects under ${mtk_repo_dir}?"
        skip_projects
    fi
}

function remove_gits_under_dir() {
    cd $1
    local lists=($(find . -name .repo -prune -o -type d -name .git -print))
    for F in ${lists[*]}
    do
        echo "remove $F in $1"
        rm -rf $F
    done
    cd ${WORKDIR}
}

function restore_mtk_repo() {
    local mtk_repo_dir=$1
    local droi_repo_dir=$2
    local tmp_dir=$3

    if [ -d "$mtk_repo_dir/.repo" -a ! -d ${droi_repo_dir}/.repo -a -d ${tmp_dir}/back_dot_repo ]; then
        mv $mtk_repo_dir/.repo $droi_repo_dir
        remove_gits_under_dir ${mtk_repo_dir}
        mv $tmp_dir/back_dot_repo $mtk_repo_dir/.repo
    else
        loge "fail: invalid args!"
        exit 1
    fi

    logi ">>> restore mtk repo: ${mtk_repo_dir}"
    cd $(readlink -f ${mtk_repo_dir})
    repo sync -l

    logi ">>> restore droi repo: ${droi_repo_dir}"
    cd $(readlink -f ${droi_repo_dir})
    repo sync -l

    cd ${WORKDIR}
}

# ------------------------------------------------------------------------------
gerrituser=$(git config user.name)

function gerrit_query() {
    local commitid=$1
    ssh -p 29418 ${gerrituser}@10.20.40.19 gerrit query --current-patch-set ${commitid}
}

function gerrit_cherry_pick() {
    local commitid=$1
    local info=$(gerrit_query $commitid)
    local refs=$(echo "$info" | grep "   ref:" | awk 'BEGIN {FS=":"} {print $2}')
    local project=$(echo "$info" | grep "  project:" | awk 'BEGIN {FS=":"} {print $2}')
    project=$(echo $project)
    logi "----------------------------------------------------------------------"
    logi "cherry-pick ${project} ${refs}"
    git fetch ssh://${gerrituser}@10.20.40.19:29418/${project} ${refs} && git cherry-pick FETCH_HEAD
}

function git_remote_branch_exist() {
    local branch=$1
    git branch -a | grep "remotes/origin/$branch"
}

function merge_patch_from_changelog() {
    local _changelog=$1
    local _remote_branch=$2
    local patchfullname=$(get_patchname_from_changelog $_changelog)
    local _src_cmt_array=($(get_commits_from_changelog $_changelog))
    local pick_no_conflicts="true"
    local commit_array=""

    logi "loop patched projects..."
    for i in ${_src_cmt_array[*]}
    do
        local _project=$(echo $i | awk 'BEGIN {FS=":"} {print $1}')
        local _commitid=$(echo $i | awk 'BEGIN {FS=":"} {print $2}')
        cd ${_project}
        local info=$(git_remote_branch_exist $_remote_branch)
        if [ x"$info" == x ]; then
            logw "warning: <${_project}> has no $_remote_branch, just skip, you may fix it by hand"
            cd ${WORKDIR}
            continue
        fi

        echo "pick ${_commitid} from ${_project}"
        git checkout ${_remote_branch}
        gerrit_cherry_pick ${_commitid}
        if [ "$?" -ne 0 ]; then
            #TODO: how to continue?
            loge "error: <${_project}> pick conflict, please fix it later, now we just skip"
            commit_array="$commit_array ${_project}:----------------------------------------"
            pick_no_conflicts="false"
        else
            logi "picked!"
            commit_array="$commit_array $_project:$(get_last_commitid)"
            git push origin HEAD:refs/for/${_remote_branch}
        fi

        cd ${WORKDIR}
    done

    # check if there is a specified branch exsited in changelog's git"
    local CHANGELOGDIR_P=$(dirname ${CHANGLOGDIR})
    cd ${CHANGELOGDIR_P}
    local info=$(git_remote_branch_exist $_remote_branch)
    if [ x"$info" == x ]; then
        logw "warning: <${CHANGLOGDIR}> has no ${_remote_branch}, don't save change logs"
	cd ${WORKDIR}
        return
    fi

    git checkout ${_remote_branch}
    cd ${WORKDIR}

    logi "create change log file"
    local _srclogdir=$(dirname ${_changelog})
    local logdir=${_srclogdir}_${_remote_branch}
    mkdir -p ${logdir}

    create_changelog ${patchfullname} "${commit_array}" ${logdir}

    logi "commit change log file"
    if [ "${pick_no_conflicts}" = "true" ]; then
        commit_changelog ${logdir}/$(get_patch_id $patchfullname).txt
        cd ${CHANGELOGDIR_P}
        git push origin HEAD:refs/for/${_remote_branch}
        cd ${WORKDIR}
    else
        logw "warning: run 'logupdate' to update commit chang log file after you fix pick confilct"
    fi
}

# ------------------------------------------------------------------------------
function help() {
cat <<EOF
usage: repopatch.sh <command> [<args>]
- patchmtk: patch mtk code with the the patch file from mtk.
    repopatch.sh patchmtk <the-patch-tar-gz-file> <tempdir>
- patchdroi: cherry-pick from changelog file
    repopatch.sh patchdroi <the-patch-change-log-file>
- logupdate: update the commit id in changelog file
    repopatch.sh logupdate <the-patch-change-log-file>
EOF
}

function check() {
    if [ ! -d ".repo" ]; then
        loge "error: must run in the root directory of Repo"
        exit 1
    fi
}

action=$1
shift
case $action in
    help)
        help
        ;;
    mtk|patchmtk)
        check
        if [ ! -f "$1" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi

        if [ ! -d "$2" ]; then
            loge "error: $2 not existed!"
            exit 1
        fi

        apply_patch_from_tarball $1 $2
        ;;
    droi|patchdroi)
        check
        if [ ! -f "$1" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi
        apply_patch_from_changelog $1
        ;;
    merge|mergepatch)
        check
        if [ ! -f "$1" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi
        merge_patch_from_changelog $1 $2
        ;;
    log|logupdate)
        check
        if [ ! -f "$1" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi
        update_commits_of_changelog $1
        logi "commit change log file"
        commit_changelog $1
        ;;
    clean)
        check
        repoclean
        ;;
    git_mtk_move)
        if [ ! -f "$1" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi
        if [ ! -d "$2" ]; then
            loge "error: $2 not existed!"
            exit 1
        fi
        if [ ! -f "$3" ]; then
            loge "error: $1 not existed!"
            exit 1
        fi
        if [ ! -d "$4" ]; then
            loge "error: $2 not existed!"
            exit 1
        fi
        apply_patch_from_mtk_repo $1 $2 $3 $4 $5
        ;;
    git_mtk_commit)
        if [ -z "$1" ]; then
            loge "error: please provide the patch id!"
            exit 1
        fi

        if [ ! -d "$2" ]; then
            loge "error: $2 not existed!"
            exit 1
        fi
        loop_commit_modified_gits $1 $2
        ;;
    git_mtk_restore)
        restore_mtk_repo $1 $2 $3
        ;;
    *)
        echo "fail: unknown subcommand!"
        exit 1
        ;;
esac
