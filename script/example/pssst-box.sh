#!/bin/bash
# This code is an example that should not be used in a productive environment
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
        echo "$MESSAGE" >> "$HOME/pssst.$1"
        notify-send Pssst "$MESSAGE"
    else
        exit 0
    fi
done