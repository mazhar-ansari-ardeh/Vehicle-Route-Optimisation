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

NUM_THREADS=5

GRIDSCRIPT_BASE="Reset.sh"
export GRIDSCRIPT_BASE

 
# setup gdb gdb1   5   gdb gdb1   4 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
#   
# setup gdb gdb1   5   gdb gdb1   6 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb2   6   gdb gdb2   5 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
  
# setup gdb gdb2   6   gdb gdb2   7 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 3 30`
# 
# setup gdb gdb3   5   gdb gdb3   4 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: 12 13 14 15
 
# setup gdb gdb3   5   gdb gdb3   6 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 3 30`

# setup gdb gdb4   4   gdb gdb4   3
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
# 
# setup gdb gdb4   4   gdb gdb4   5
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
# 
setup gdb gdb5   6   gdb gdb5   5
./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 20 30`
 
# setup gdb gdb5   6   gdb gdb5   7
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb6   5   gdb gdb6   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
  
# setup gdb gdb6   5   gdb gdb6   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
# 
# setup gdb gdb7   5   gdb gdb7   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
#  
# setup gdb gdb7   5   gdb gdb7   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb8   10  gdb gdb8   11
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb8   10  gdb gdb8   9
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb9   10  gdb gdb9   9 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb9   10  gdb gdb9   11
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb10   4   gdb gdb10   3
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb10   4   gdb gdb10   5
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

#setup gdb gdb12   7   gdb gdb12   6
#./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
# 
#setup gdb gdb12   7   gdb gdb12   8
#./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
#
#setup gdb gdb13   6   gdb gdb13  5 
#./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb13   6   gdb gdb13  7 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb14   5   gdb gdb14   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb14   5   gdb gdb14   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb15   4   gdb gdb15   3
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb15   4   gdb gdb15   5
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
  
# setup gdb gdb16   5   gdb gdb16   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb16   5   gdb gdb16   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb17   5   gdb gdb17   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb17   5   gdb gdb17   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb18   5   gdb gdb18   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
#  
# setup gdb gdb18   5   gdb gdb18   6
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb19  3    gdb gdb19   2 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb19  3    gdb gdb19   4
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb20   4   gdb gdb20   3
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb20   4   gdb gdb20   5
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb21  6   gdb gdb21  5 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb21  6   gdb gdb21  7 
# ./parallel/parallel --results /local/tmp/para_results -j$NUM_THREADS ./parallel_script.sh ::: `seq 30`

# setup gdb gdb23  10  gdb gdb23  9
# ./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`
 
# setup gdb gdb23  10  gdb gdb23  11
# ./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

# setup val val9C  5   val val9C  6
# ./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

# setup val val9C  5   val val9C  4
# ./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val9D  10  val val9D  9
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val9D  10  val val9D  11
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val10C 5   val val10C 4
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val10C 5   val val10C 6
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val10D 10   val val10D 9
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

#setup val val10D 10   val val10D 11
#./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

# setup val val9A 3   val val9A 4
# ./parallel/parallel --results /local/tmp/para_results -j${NUM_THREADS} ./parallel_script.sh ::: `seq 30`

# cd .. 
# rm -f -r para

# setup val val10D 10  val val10D 11

# 
# setup val val10D 10  val val10D 9 
