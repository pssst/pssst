#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]]; then
    BRANCH=master
else
    BRANCH=$1
fi

git clone https://github.com/pssst/pssst -b $BRANCH

VERSION=$(cat pssst/app/cli/pssst.py | grep '__version__')
VERSION=$(echo $VERSION | sed 's/[^0-9.]*\([0-9.]*\).*/\1/')

DEB=pssst-$VERSION/DEBIAN
BIN=pssst-$VERSION/usr/bin
MAN=pssst-$VERSION/usr/share/man/man1
DOC=pssst-$VERSION/usr/share/doc/pssst

mkdir -p $DEB $BIN $MAN $DOC

gzip -c pssst/app/man/pssst > $MAN/pssst.1.gz
cp pssst/app/cli/pssst.py $BIN/pssst
cp pssst/* $DOC/ || true
chmod a+x $BIN/pssst

cat > $DEB/control <<EOF
Package: pssst
Section: web
Version: $VERSION
Depends: python (>= 2.7)
Priority: optional
Architecture: all
Installed-Size: 73
Maintainer: Christian & Christian <hello@pssst.name>
Description: Pssst CLI
EOF

cat > $DEB/copyright <<EOF
Format-Specification: http://svn.debian.org/wsvn/dep/web/deps/dep5.mdwn?op=file&rev=135

Files: *
Copyright: 2013-2015 Christian & Christian <hello@pssst.name>
License: GPL-3+

License: GPL-3+
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.

 On Debian systems, the full text of the GNU General Public License
 version 2 can be found in the file 'usr/share/common-licenses/GPL-3'.
EOF

dpkg -b pssst-$VERSION pssst_$VERSION-1_all.deb

rm -rf pssst pssst-$VERSION

echo "Created file pssst_$VERSION-1_all.deb"
exit 0