#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]]; then
    echo Usage: $(basename $0) BRANCH
    exit 2
fi

BRANCH=$1
CONFIG=$1

if [ "$BRANCH" = "master" ]; then
    SERVER=live
else
    SERVER=test
fi

RUN=$HOME/run/pssst
CFG=$HOME/etc/pssst
ETC=$HOME/etc/run-pssst.$SERVER
SVC=$HOME/service/pssst.$SERVER

DIR=$RUN/$SERVER

if [[ ! -d $CFG/$CONFIG ]]; then
    CONFIG=default
fi

echo "Checking out $SERVER server (Branch $BRANCH) in $DIR..."

if [[ -d $SVC ]]; then
    svc -dx $SVC
    sleep 3
fi

rm -rf $SVC
rm -rf $ETC
rm -rf $DIR

git clone https://github.com/pssst/pssst.git -b $BRANCH $DIR/tmp

mv $DIR/tmp/server/* $DIR/
rm -rf $DIR/tmp

cd $DIR && npm install
cp $CFG/$CONFIG/* .
mv pssst.key app/app.key
mv pssst.pub files/key

echo $BRANCH > files/branch
node start.js -v | sed 's/[^0-9.]*\([0-9.]*\).*/\1/' > files/version

uberspace-setup-service pssst.$SERVER node $DIR/start.js

echo "Done"
exit 0
