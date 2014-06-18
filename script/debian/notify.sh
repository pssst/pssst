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

MESSAGE=$(pssst pull $1:$2)

if [[ ! -z $MESSAGE ]]; then
    notify-send -t 30000 "$MESSAGE"
fi

exit 0