#!/bin/bash
cd /home/coekjan/prj/mipsysy-compiler/ir-test || exit 1
path=$1
li=$(find "$path" -name "testfile*.txt")
for fn in $li
do
  n=$(echo "$fn" | tr -cd "0-9")
  cp "$path"/testfile"$n".txt ./testfile.txt
  cp "$path"/input"$n".txt ./input.txt
  cp "$path"/output"$n".txt ./std.txt
  echo "> RUNNING testfile$n.txt"
  java -jar ../out/artifacts/mipsysy_compiler_jar/mipsysy-compiler.jar < ./input.txt > ./out.txt
  if ! diff ./std.txt ./out.txt --suppress-common-lines --side-by-side --ignore-all-space --ignore-blank-lines
  then
    echo WRONG ANWSER!
    exit 1
  fi
done

echo ACCEPTED!