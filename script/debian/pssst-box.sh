#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} || -z ${2:-} ]]; then
    echo Usage: $(basename $0) USERNAME PASSWORD
    exit 2
fi

if [[ ! `ping -c1 api.pssst.name` ]]; then
    exit 1
fi

while true; do
    MESSAGE="$(pssst pull $1:$2)"

    if [[ ! -z $MESSAGE ]]; then
        echo $MESSAGE >> "$HOME/pssst.$1"
    else
        exit 0
    fi
done