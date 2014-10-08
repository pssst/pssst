#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]] && [[ -z ${2:-} ]]; then
    echo Usage: $(basename $0) BRANCH APP
    exit 2
fi

BRANCH=$1
HEROKU=$2

TMP=/tmp/pssst

git clone https://github.com/pssst/pssst -b $BRANCH $TMP

if [[ ! -d $HEROKU ]]; then
    mkdir -p $HEROKU

    cd $HEROKU

    git init
    git remote add heroku git@heroku.com:$HEROKU.git

    heroku login
else
    cd $HEROKU

    git pull heroku master
    git checkout master
fi

cp -r -u $TMP/server/* .
rm -rf $TMP

git add .
git commit -a -m "Checked out $BRANCH"
git push heroku master

cd ..

echo "Done"
exit 0
