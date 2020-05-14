#! /bin/bash

declare -a SERVERS=(40 60 80 100 120)
declare RUNS=3 # rerun same configuration
declare FLAG_FIELD_BITS="--fieldBase_bits=256"
declare FLAG_GENERATOR_BITS="--generator_bits=128"
declare FLAG_CONSTRUCTION="--construction=1"

# This has to be set before running
declare PATH_TO_JAR=""
declare JAR_NAME="./build/libs/client-0.0.1-SNAPSHOT-all.jar"
declare OUTPUT_FILE="sample.csv"

for server in "${SERVERS[@]}"; do
  for ((TSECURE = 15; TSECURE < $server; TSECURE += 20)); do
    for run in $(seq 1 $RUNS); do
      now=$(date +%H:%M:%S)
      echo "["$now"] Test $run/$RUNS with t_secure="$TSECURE" and serves="$server""
      java11 -jar "$JAR_NAME" --test --numberOfServers="$server" --t_secure="$TSECURE" "$FLAG_CONSTRUCTION" "$FLAG_FIELD_BITS" "$FLAG_GENERATOR_BITS" | grep "F,\|S," >>"$OUTPUT_FILE"
    done
  done
done
