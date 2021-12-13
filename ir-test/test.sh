#!/bin/bash
cd /home/coekjan/prj/mipsysy-compiler/ir-test || exit 1
path=$1
li=$(find ../testcases/"$path" -name "testfile*.txt")
for fn in $li
do
  n=$(echo "$fn" | tr -cd "0-9")
  cp ../testcases/"$path"/testfile"$n".txt ./testfile.txt
  cp ../testcases/"$path"/input"$n".txt ./input.txt
  cp ../testcases/"$path"/output"$n".txt ./std.txt
  echo "> RUNNING $1 | testfile$n.txt"
  java -jar ../out/artifacts/mipsysy_compiler_jar/mipsysy-compiler.jar -ea --ir < ./input.txt > ./out.txt
  if ! diff ./std.txt ./out.txt --suppress-common-lines --side-by-side --ignore-all-space --ignore-blank-lines
  then
    echo WRONG ANSWER!
    exit 1
  fi
done

echo "$1" ACCEPTED!