#!/bin/bash
set -o errexit
set -o nounset

if [ ! -f pssst.py ]; then
    sh data/setup.sh
    ln -s ../cli/pssst.py
fi

python pssst-gui.py $*
