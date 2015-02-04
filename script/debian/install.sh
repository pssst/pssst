#!/bin/bash
set -o errexit
set -o nounset

URL=https://raw.github.com/pssst/pssst/master/app/cli/pssst.py

if [[ $EUID == 0 ]]; then
    DIR=/usr/local/bin
else
    DIR=$HOME
fi

wget -O $DIR/pssst $URL > /dev/null 2>&1
chmod 755 $DIR/pssst

echo "Pssst installed in $DIR"
exit 0