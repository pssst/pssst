@echo off

copy ..\..\app\cli\pssst.py pssst.py

python makebin.py py2exe %*

del /q pssst.py?
rd /s /q build
