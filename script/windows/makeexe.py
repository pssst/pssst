#!/usr/bin/env python
import distutils
import sys


try:
    from pssst import __version__
except ImportError:
    sys.exit("Please execute the make script")


try:
    import py2exe
except ImportError:
    sys.exit("Requires py2exe (https://py2exe.org)")


distutils.core.setup(
    console=[{
        "name": "Pssst",
        "script": "pssst.py",
        "version": __version__
    }],
    options={
        "py2exe": {
            "dist_dir": ".",
            "dll_excludes": [
                "MSVCP90.dll",
                "mswsock.dll",
                "powrprof.dll",
                "w9xpopen.exe"
            ],
            "optimize": 1,
            "compressed": True,
            "unbuffered": True,
            "bundle_files": 1,
        }
    },
    zipfile=None
)
