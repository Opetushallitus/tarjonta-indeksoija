#!/bin/bash
cd "${0%/*}"
zip -r -v hunspell.zip hunspell
zip -r -v decompound.zip decompound
cd -