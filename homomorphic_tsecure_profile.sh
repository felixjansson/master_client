#! /usr/bin/bash

declare -a SERVERS=(40 60 80 100 120 140 160 180 200)
declare REPEAT_RUNS=3 # rerun same test
declare FIELD_BITS=256
declare GENERATOR_BITS=128
declare CONSTRUCTION=1
declare WARMUP_RUNS=2500
declare RUNS=10000
declare JAVA_SIMON="/cygdrive/c/Users/SS170679/AppData/Local/JetBrains/IntelliJ IDEA 2019.3.3/jbr/bin/java.exe"
declare JAVA_FELIX=""
declare JAVA_PATH=java

# This has to be set before running
declare JAR_NAME="./build/libs/client-0.0.1-SNAPSHOT-all.jar"
now=$(date +%H:%M:%S)
declare OUTPUT_FILE=""$now"_homomorphic_tsecure_sample.csv"

# Print the column labels
if [ ! -f "$OUTPUT_FILE" ]
then
  echo "tag,construction,runs,warmup_runs,t_secure,numberOfServers,fieldBase_bits,generator_bits,date,time(ms),comments" >> "$OUTPUT_FILE"
fi

for server in "${SERVERS[@]}"; do
  for ((TSECURE = 15; TSECURE < $server; TSECURE += 20)); do

    now=$(date +%H:%M:%S)
    echo "["$now"] NEW TEST ROUND: t_secure="$TSECURE" and serves="$server""

    for run in $(seq 1 $REPEAT_RUNS); do
      now=$(date +%H:%M:%S)
      echo "["$now"] Test $run/$REPEAT_RUNS"
      # Execute the test
      "$JAVA_PATH" -jar "$JAR_NAME" --test \
        --numberOfServers="$server" \
        --t_secure="$TSECURE" \
        --construction="$CONSTRUCTION" \
        --fieldBase_bits="$FIELD_BITS" \
        --generator_bits="$GENERATOR_BITS" \
        --runs="$RUNS" \
        --warmup_runs="$WARMUP_RUNS" \
        | grep "F,\|S," >> "$OUTPUT_FILE"
    done
  done
done
