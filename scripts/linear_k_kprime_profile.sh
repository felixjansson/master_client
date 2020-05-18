
#! /usr/bin/bash

declare -a K_BITS_ARR=(16 32 64 128 256 512 1024 2048 4096)
declare -a K_PRIME_BITS_ARR=(16 32 64 128 256 512 1024 2048 4096)
declare REPEAT_RUNS=3 # rerun same configuration
declare CONSTRUCTION=3
declare TSECURE=15
declare SERVERS=20
declare WARMUP_RUNS=2500
declare RUNS=10000
declare JAVA_S="/cygdrive/c/Users/SS170679/AppData/Local/JetBrains/IntelliJ IDEA 2019.3.3/jbr/bin/java.exe"
declare JAVA_F="/cygdrive/c/Users/FJ865990/AppData/Local/JetBrains/IntelliJ IDEA 2020.1.1/jbr/bin/java.exe"
declare JAVA_PATH="$JAVA_S"


declare JAR_NAME="./build/libs/client-0.0.1-SNAPSHOT-all.jar"
now=$(date +%m%d-%H:%M:%S)
declare OUTPUT_FILE=""$now"_linear_k_kprime_sample.csv"

# Print the column labels
if [ ! -f "$OUTPUT_FILE" ]
then
  echo "tag,construction,runs,warmup_runs,t_secure,numberOfServers,k_bits,k_prime_bits,date,time(ms),comments" >> "$OUTPUT_FILE"
fi

for k_bits in "${K_BITS_ARR[@]}"; do

  for k_prime_bits in "${K_PRIME_BITS_ARR[@]}"; do

    now=$(date +%H:%M:%S)
    echo "["$now"] NEW TEST ROUND: k_bits="$k_bits" and k_prime_bits="$k_prime_bits""
    
    for run in $(seq 1 $REPEAT_RUNS); do
      now=$(date +%H:%M:%S)
      echo "["$now"] Test $run/$REPEAT_RUNS"
      # Execute the test
      "$JAVA_PATH" -jar "$JAR_NAME" --test \
        --t_secure="$TSECURE" \
        --numberOfServers="$SERVERS" \
        --construction="$CONSTRUCTION" \
        --k="$k_bits" \
        --k_prime="$k_prime_bits" \
        --runs="$RUNS" \
        --warmup_runs="$WARMUP_RUNS" \
        | grep "F,\|S," >>"$OUTPUT_FILE"
    done

  done

done