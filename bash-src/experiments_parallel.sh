#!/usr/bin/env bash

function setup()
{
    DATASET_CATEGORY_SOURCE=$1; export DATASET_CATEGORY_SOURCE

    DATASET_SOURCE=$2;          export DATASET_SOURCE

    NUM_VEHICLES_SOURCE=$3;     export NUM_VEHICLES_SOURCE


    DATASET_CATEGORY_TARGET=$4; export DATASET_CATEGORY_TARGET

    DATASET_TARGET=$5;          export DATASET_TARGET

    NUM_VEHICLES_TARGET=$6;     export NUM_VEHICLES_TARGET
}


# setup gdb gdb23   10  gdb gdb23   11 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: 7 1
# 
# setup gdb gdb23   10  gdb gdb23   9 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: 7 1
# 
# setup gdb gdb8   10  gdb gdb8   12 # This requires to run all experiments. !!!!!!!!!!!
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb8   10  gdb gdb8   8 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 

 
# setup gdb gdb1   5   gdb gdb1   6 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
#  
# setup gdb gdb1   5   gdb gdb1   4 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb2   6   gdb gdb2   7 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb2   6   gdb gdb2   5 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
 
# setup gdb gdb8   10  gdb gdb8   11
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb8   10  gdb gdb8   9
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
#  
# setup gdb gdb9   10  gdb gdb9   9 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb9   10  gdb gdb9   11
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

# setup gdb gdb21  6   gdb gdb21  7 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`
# 
# setup gdb gdb21  6   gdb gdb21  5 
# parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup gdb gdb23  10  gdb gdb23  11
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup gdb gdb23  10  gdb gdb23  9 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val9C  5   val val9C  6 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val9C  5   val val9C  4 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30` 

setup val val9D  10  val val9D  9 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val9D  10  val val9D  11
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val10C 5   val val10C 4 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val10C 5   val val10C 6 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val10D 10   val val10D 9 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

setup val val10D 10   val val10D 11 
parallel --results /local/scratch/para_results -j9 ./paralle_script.sh ::: `seq 30`

# setup val val10D 10  val val10D 11

# 
# setup val val10D 10  val val10D 9 
