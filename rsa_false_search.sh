#! /usr/bin/bash

declare K_BITS=0
declare K_PRIME_BITS=0
declare REPEAT_RUNS=1 # rerun same configuration
declare CONSTRUCTION=2
declare TSECURE=5
declare SERVERS=10
declare WARMUP_RUNS=0
declare RUNS=10
declare FIELDBASE_BITS_START=4
declare FIELDBASE_BITS_END=32
declare RSA_BITS_START=6
declare RSA_BITS_END=24
declare JAVA_S="/cygdrive/c/Users/SS170679/AppData/Local/JetBrains/IntelliJ IDEA 2019.3.3/jbr/bin/java.exe"
declare JAVA_F="/cygdrive/c/Users/FJ865990/AppData/Local/JetBrains/IntelliJ IDEA 2020.1.1/jbr/bin/java.exe"
declare JAVA_PATH=java


declare JAR_NAME="./build/libs/client-0.0.1-SNAPSHOT-all.jar"
now=$(date +%m%d-%H:%M:%S)
declare OUTPUT_FILE=""$now"_rsa_false_search_sample.csv"

# Print the column labels
if [ ! -f "$OUTPUT_FILE" ]
then
  echo "valid,construction,numberOfServers,fieldBase_bits,generator_bits,t_secure,k_bits,k_prime_bits,runs,rsa_bits,warmup_runs,comments" >> "$OUTPUT_FILE"
fi

for run in $(seq 1 $REPEAT_RUNS); do
  for ((FIELDBASE_BITS=$FIELDBASE_BITS_START; FIELDBASE_BITS < $FIELDBASE_BITS_END; FIELDBASE_BITS += 1)) do
    for ((RSA_BITS=$RSA_BITS_START; RSA_BITS < $RSA_BITS_END; RSA_BITS += 1)) do
      now=$(date +%H:%M:%S)
      GENERATOR_BITS=$(($FIELDBASE_BITS / 2))
      echo "["$now"] NEW TEST ROUND: fieldbase="$FIELDBASE_BITS" and RSA="$RSA_BITS""
      # Execute the test
      "$JAVA_PATH" -jar "$JAR_NAME" --local \
        --runs="$RUNS" \
        --warmup_runs="$WARMUP_RUNS" \
        --construction="$CONSTRUCTION" \
        --t_secure="$TSECURE" \
        --numberOfServers="$SERVERS" \
        --fieldBase_bits=$FIELDBASE_BITS \
        --generator_bits=$GENERATOR_BITS \
        --RSA_BIT_LENGTH=$RSA_BITS \
        --k="$K_BITS" \
        --k_prime="$K_PRIME_BITS" \
        < input_rsa_false_search \
        | grep "true,\|false," >> "$OUTPUT_FILE"
        done
    done
done
