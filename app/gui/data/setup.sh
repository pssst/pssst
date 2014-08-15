#!/bin/bash
set -o errexit
set -o nounset

cd ${0%/*}

mkdir tmp

BOOT_VERSION="3.2.0"
BOOT_URL="https://github.com/twbs/bootstrap/releases/download/v$BOOT_VERSION/"
BOOT_ZIP="bootstrap-$BOOT_VERSION-dist.zip"

FONT_VERSION="4.1.0"
FONT_URL="https://fortawesome.github.io/Font-Awesome/assets/"
FONT_ZIP="font-awesome-$FONT_VERSION.zip"

CRYPTO_VERSION="3.1.2"
CRYPTO_URL="https://crypto-js.googlecode.com/files/"
CRYPTO_ZIP="CryptoJS v$CRYPTO_VERSION.zip"

JQUERY_VERSION="2.1.1"
JQUERY_URL="https://code.jquery.com/jquery-$JQUERY_VERSION.min.js"

# Install Bootstrap
wget -qP tmp $BOOT_URL$BOOT_ZIP && \
unzip -q tmp/$BOOT_ZIP -d tmp && \
cp -n tmp/bootstrap-$BOOT_VERSION-dist/css/bootstrap.min.css css/ && \
cp -n tmp/bootstrap-$BOOT_VERSION-dist/js/bootstrap.min.js js/

# Install Font Awesome
wget -qP tmp $FONT_URL$FONT_ZIP && \
unzip -q tmp/$FONT_ZIP -d tmp && \
cp -n tmp/font-awesome-$FONT_VERSION/css/font-awesome.min.css css/ && \
cp -n tmp/font-awesome-$FONT_VERSION/fonts/fontawesome-webfont.* fonts/

# Install Crypto-JS
wget -qP tmp "$CRYPTO_URL$CRYPTO_ZIP" && \
unzip -q "tmp/$CRYPTO_ZIP" -d tmp && \
cp -n tmp/rollups/aes.js js/aes.min.js

# Install jQuery
wget -qP js $JQUERY_URL

rm -rf tmp
