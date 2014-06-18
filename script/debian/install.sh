#!/bin/bash
set -o errexit
set -o nounset

URL=https://raw.github.com/pssst/pssst/master/app/cli/pssst.py

if [[ $EUID == 0 ]]; then
    BIN=/usr/local/bin
else
    BIN=$HOME/bin
fi

wget -O $BIN/pssst $URL > /dev/null 2>&1
chmod 755 $BIN/pssst

echo "Installed in $BIN"
exit 0