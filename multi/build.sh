#!/bin/sh
# Builds multi-1.0.jar and runs the tests against stub Minecraft/Forge classes (no Minecraft needed).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out/stubs out/main out/test
javac -nowarn -encoding UTF-8 --release 8 -d out/stubs $(find tests/stubs -name '*.java')
javac -nowarn -encoding UTF-8 --release 8 -cp out/stubs -d out/main src/multi/*.java
javac -nowarn -encoding UTF-8 --release 8 -cp out/stubs:out/main -d out/test tests/Run.java
java -cp out/stubs:out/main:out/test Run
cp -r res/* out/main/
(cd out/main && jar cf ../../multi-1.0.jar .)
echo built multi-1.0.jar
