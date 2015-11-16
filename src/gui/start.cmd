@echo off
IF EXIST pssst.py GOTO START

bower install
copy ..\cli\pssst.py pssst.py

:START
python pssst-gui.py %*
