#!/bin/bash
echo '#include "sysy_header.h"' > tmp.c
cat $1 >> tmp.c
gcc tmp.c -I ./include -o out/$1.exe
rm tmp.c