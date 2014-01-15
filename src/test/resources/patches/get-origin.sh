#!/bin/bash

set -eux

javas=(
  com/thoughtworks/selenium/webdriven/WebDriverCommandProcessor.java
  com/thoughtworks/selenium/webdriven/commands/Windows.java
)

if [ "${1:-}" = -f ]; then
  for java in "${javas[@]}"; do
    f=${java##*/}
    rm -f "$f"
  done
fi

for java in "${javas[@]}"; do
  f=${java##*/}
  if [ ! -f "$f" ]; then
    wget https://selenium.googlecode.com/git/java/client/src/$java
  fi
done
