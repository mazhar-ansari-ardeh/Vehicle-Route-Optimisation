#!/bin/bash
#SBATCH -a 1-1
#SBATCH --cpus-per-task=2
#SBATCH --mem-per-cpu=8G
#SBATCH --time=01:00:00
#SBATCH --partition=quicktest
#SBATCH -o /nfs/home/ansarima/out/%x-%A-%a.o.txt
#SBATCH -e /nfs/home/ansarima/out/%x-%A-%a.e.txt

# Print the task id.
echo "My SLURM_ARRAY_TASK_ID:  $SLURM_ARRAY_TASK_ID $SLURM_ARRAY_JOB_ID $SLURM_JOB_NAME "
module load java/jdk/1.8.0_121
module list

# Naming convention:
#   Source-target separator: '.'
#   Algorithm parameter name separator: ':'
#   Algorithm parameter value separator: '_'

DATASET_CATEGORY_0='gdb'; export DATASET_CATEGORY_0

DATASET_0='gdb1'; export DATASET_0

NUM_VEHICLES_0=4; export NUM_VEHICLES_0

DATASET_CATEGORY_1='gdb'; export DATASET_CATEGORY_1

DATASET_1='gdb1'; export DATASET_1

NUM_VEHICLES_1=5; export NUM_VEHICLES_1

DATASET_CATEGORY_2='gdb'; export DATASET_CATEGORY_2

DATASET_2='gdb1'; export DATASET_2

NUM_VEHICLES_2=6; export NUM_VEHICLES_2


GENERATIONS=50

. ./multipoplib_r_3.sh 

# The seed value that will be used for testing models found. This value must be unique
TEST_SEED=25556

EMTET_experiment 200 1 0.5
