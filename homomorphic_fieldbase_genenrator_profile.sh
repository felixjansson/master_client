
#! /usr/bin/bash

declare -a FIELDBASE_BITS_ARR=(16 32 64 128 256 512 1024 2048 4096)
declare REPEAT_RUNS=3 # rerun same configuration
declare GENERATOR_BITS=8
declare CONSTRUCTION=1
declare TSECURE=15
declare SERVERS=20
declare WARMUP_RUNS=2500
declare RUNS=10000
declare JAVA_S="/cygdrive/c/Users/SS170679/AppData/Local/JetBrains/IntelliJ IDEA 2019.3.3/jbr/bin/java.exe"
declare JAVA_F="/cygdrive/c/Users/FJ865990/AppData/Local/JetBrains/IntelliJ IDEA 2020.1.1/jbr/bin/java.exe"
declare JAVA_PATH="$JAVA_F"


declare JAR_NAME="./build/libs/client-0.0.1-SNAPSHOT-all.jar"
now=$(date +%H:%M:%S)
declare OUTPUT_FILE=""$now"_homomorphic_fieldbase_genenrator_sample.csv"

# Print the column labels
if [ ! -f "$OUTPUT_FILE" ]
then
  echo "tag,construction,runs,warmup_runs,t_secure,numberOfServers,fieldBase_bits,generator_bits,date,time(ms),comments" >> "$OUTPUT_FILE"
fi

for fieldbase_bits in "${FIELDBASE_BITS_ARR[@]}"; do

  for ((generator_bits = "$GENERATOR_BITS"; generator_bits <= $fieldbase_bits; generator_bits *= 2)); do

    now=$(date +%H:%M:%S)
    echo "["$now"] NEW TEST ROUND: generator_bits="$generator_bits" and fieldBase_bits="$fieldbase_bits""
    
    for run in $(seq 1 $REPEAT_RUNS); do
      now=$(date +%H:%M:%S)
      echo "["$now"] Test $run/$REPEAT_RUNS"
      # Execute the test
      "$JAVA_PATH" -jar "$JAR_NAME" --test \
        --t_secure="$TSECURE" \
        --numberOfServers="$SERVERS" \
        --construction="$CONSTRUCTION" \
        --fieldBase_bits="$fieldbase_bits" \
        --generator_bits="$generator_bits" \
        --runs="$RUNS" \
        --warmup_runs="$WARMUP_RUNS" \
        | grep "F,\|S," >>"$OUTPUT_FILE"
    done

  done

done