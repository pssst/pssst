#!/bin/bash
set -o errexit

if [ ! -f pssst.py ]; then
    bower install
    ln -s ../cli/pssst.py
fi

python pssst-gui.py $*
