#!/bin/bash
#SBATCH -a 1-30
#SBATCH --cpus-per-task=1
#SBATCH --mem-per-cpu=12G
#SBATCH --partition=parallel
#SBATCH --time=1-00:00:00
#SBATCH --mail-type=FAIL
#SBATCH --mail-user=mazhar@ecs.vuw.ac.nz

# Setting variables that config the experiment.

if test -z $1
then 
    echo 'Arg 1 for script name is empty. Script terminated.'
    exit
else 
    RAPSCRIPT=$1
    echo "Running $RAPSCRIPT"
fi

if test -z $2
then
    echo 'Arg 2 for source dataset category is empty. Sctript terminated.'
    exit
else
    DATASET_CATEGORY_0=$2; export DATASET_CATEGORY_0
fi

if test -z $3
then
    echo 'Arg 3 is empty. Sctript terminated.'
    exit
else
    DATASET_0=$3; export DATASET_0
fi

if test -z $4
then
    echo 'Arg 4 is empty. Sctript terminated.'
    exit
else
    NUM_VEHICLES_0=$4; export NUM_VEHICLES_0
fi

if test -z $5 
then 
    echo 'Arg 5 is empty. Script terminated.'
#     exit
else
    DATASET_CATEGORY_1=$5; export DATASET_CATEGORY_1
fi

if test -z $6 
then 
    echo 'Arg 5 is empty. Script terminated.'
#     exit
else
    DATASET_1=$6; export DATASET_1
fi

if test -z $7
then
    echo 'Arg 7 is empty. Sctript terminated.'
#     exit
else
    NUM_VEHICLES_1=$7; export NUM_VEHICLES_1
fi

if test -z $8
then 
    echo 'Arg 8 is empty.'
#     exit
else
    DATASET_CATEGORY_2=$8; export DATASET_CATEGORY_2
fi

if test -z $9
then 
    echo 'Arg 9 is empty.'
#     exit
else
    DATASET_2=$9; export DATASET_2
fi

if test -z ${10}
then
    echo 'Arg 10 is empty.'
#     exit
else
    NUM_VEHICLES_2=${10}; export NUM_VEHICLES_2
fi


chmod 777 ./$RAPSCRIPT
./$RAPSCRIPT


echo "Ran through OK"
