#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]] && [[ -z ${2:-} ]]; then
    echo Usage: $(basename $0) BRANCH APP
    exit 2
fi

BRANCH=$1
HEROKU=$2
ORIGIN=/tmp/pssst
CONFIG='{"debug":0,"port":0,"deny":null,"db":{"source":0,"number":0}}'

git clone https://github.com/pssst/pssst -b $BRANCH $ORIGIN

if [[ ! -d $HEROKU ]]; then
    mkdir -p $HEROKU/config
    mkdir -p $HEROKU/public

    cd $HEROKU && git init

    heroku login
    heroku git:remote -a $HEROKU
else
    cd $HEROKU
fi

git pull heroku master
git checkout master

cp -r -u $ORIGIN/server/* .
rm config/config.json.sample
echo $CONFIG > config/config.json

rm -rf $ORIGIN

git add .
git commit -a -m "Checked out $BRANCH"
git push heroku master

cd ..

echo "Done"
exit 0
