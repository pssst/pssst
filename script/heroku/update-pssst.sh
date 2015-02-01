#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]] && [[ -z ${2:-} ]]; then
    echo Usage: $(basename $0) APP BRANCH
    exit 2
fi

HEROKU=$1
BRANCH=$2

TMP=/tmp/pssst-update-$(date +%s)

if [[ ! -f $HOME/.netrc ]]; then
    heroku login
fi

git clone https://github.com/pssst/pssst -b $BRANCH $TMP

if [[ ! -d $HEROKU ]]; then
    mkdir -p $HEROKU/www

    cd $HEROKU

    git init
    git remote add heroku git@heroku.com:$HEROKU.git
    git pull heroku master

    openssl genrsa -out ./id_rsa 4096
    openssl rsa -in ./id_rsa -pubout > ./www/key
else
    cd $HEROKU

    git pull heroku master
    git checkout master
fi

cp -r $TMP/server/* .
rm -rf $TMP

git add .
git commit -a -m "Checked out $BRANCH"
git push heroku master

cd ..

echo "Done"
exit 0