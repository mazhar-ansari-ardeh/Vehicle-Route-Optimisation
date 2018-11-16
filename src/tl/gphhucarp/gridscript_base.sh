#!/usr/bin/env bash

# Setting variables that config the experiment.

if [ -z "$NUM_VEHICLES_SOURCE" ]
then
    NUM_VEHICLES_SOURCE=6
fi
echo "Number of source vehicles: $NUM_VEHICLES_SOURCE"

if [ -z "$NUM_VEHICLES_TARGET" ]
then
    NUM_VEHICLES_TARGET=7
fi
echo "Number of target vehicles: $NUM_VEHICLES_TARGET"

if [ -z "$DATASET_CATEGORY" ]
then
    DATASET_CATEGORY="gdb"
fi
if [ -z "$DATASET" ]
then
    DATASET="gdb21"
fi
echo "Dataset: $DATASET_CATEGORY/$DATASET"

STAT_ROOT="stats"


# Helper variables that are created based on setting variables.
# Do not modify these variables.

# Path to dataset file
DATASET_FILE="$DATASET_CATEGORY/$DATASET.dat"

# Write knowledge
WRITE_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-writeknow"

# Extract knowledge
KNOWLEDGE_FILE="$DATASET-v$NUM_VEHICLES_SOURCE.cf"


echo "$DATASET_CATEGORY/$DATASET"
echo "$NUM_VEHICLES_SOURCE to $NUM_VEHICLES_TARGET"

# Helper variables that are created based on setting variables.
# Do not modify these variables.

# The seed value that will be used for testing models found. This value must be unique
TEST_SEED=25556

if [ -z "$SGE_TASK_ID" ]
then
    SGE_TASK_ID=3
fi
echo "SGE_TASK_ID: $SGE_TASK_ID"
if [ -z "$JOB_ID" ]
then
    JOB_ID=4
fi
echo "JOB_ID: $JOB_ID"


######################################################################################################################
echo "Begining to write knowledge"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                                   -p seed.0=$SGE_TASK_ID
echo "Finished writing knowledge"
######################################################################################################################


######################################################################################################################
echo "Begining to test the source problem results"
java -cp .:tl.jar gphhucarp.gp.GPTest  -file carp_param_base.param \
                                       -p train-path=$WRITE_KNOW_STAT_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE\
                                       -p seed.0=$TEST_SEED
echo "Finished performing test on source problem results"
######################################################################################################################


######################################################################################################################
#echo "Begining to extract knowledge"
#java -cp .:tl.jar tl.gp.CFExtractor carp_param_base.param  $WRITE_KNOW_STAT_DIR/population.gen.49.bin all $KNOWLEDGE_FILE \
#                                                        stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
#                                                        stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
#                                                        eval.problem.eval-model.instances.0.file=$DATASET_FILE \
#                                                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
#                                                        seed.0=$SGE_TASK_ID
#echo "Finished extracting knowledge"
######################################################################################################################


######################################################################################################################
echo "Running experiment on target problem without knowledge"
WITHOUT_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_TARGET-wok"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                   -p seed.0=$SGE_TASK_ID

echo "Finished running experiment on target without knowledge"

echo "Running tests on target problem without knowledge"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                       -p train-path=$WITHOUT_KNOW_STAT_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                       -p seed.0=$TEST_SEED
printf "Finished running tests on target without knowledge\n\n\n"
######################################################################################################################


######################################################################################################################
printf "Running experiment on target problem with subtree50 knowledge\n\n"
WITH_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-subtree50"
java -cp .:tl.jar ec.Evolve         -file carp_param_base.param \
                                    -p stat.file="\$$WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITH_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE  \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                                    -p gp.tc.0.init.knowledge-log-file="$WITH_KNOW_STAT_DIR/subtree50knowinit" \
                                    -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.49.bin \
                                    -p gp.tc.0.init.transfer-percent=50 \
                                    -p gp.tc.0.init.knowledge-extraction=rootsubtree \
                                    -p seed.0=$SGE_TASK_ID

printf "Finished running experiment on target with subtree50 knowledge\n\n\n"

printf "Running tests on target problem with subtree knowledge\n\n"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$WITH_KNOW_STAT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p seed.0=$TEST_SEED
printf "Finished running tests on target with subtree knowledge\n\n\n"
######################################################################################################################


######################################################################################################################
printf "Running experiment on target problem with subtree75 knowledge\n\n"
WITH_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-subtree75"
java -cp .:tl.jar ec.Evolve         -file carp_param_base.param \
                                    -p stat.file="\$$WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITH_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE  \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                                    -p gp.tc.0.init.knowledge-log-file="$WITH_KNOW_STAT_DIR/subtree75knowinit" \
                                    -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.49.bin \
                                    -p gp.tc.0.init.transfer-percent=75 \
                                    -p gp.tc.0.init.knowledge-extraction=rootsubtree \
                                    -p seed.0=$SGE_TASK_ID

printf "Finished running experiment on target with subtree75 knowledge\n\n\n"

printf "Running tests on target problem with subtree75 knowledge\n\n"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$WITH_KNOW_STAT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p seed.0=$TEST_SEED
printf "Finished running tests on target with subtree75 knowledge\n\n\n"
######################################################################################################################


######################################################################################################################
printf "Running tests on target problem with MY knowledge\n\n"
WITH_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-MY"
java -cp .:tl.jar ec.Evolve         -file carp_param_base.param \
                                    -p stat.file="\$$WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITH_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE  \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p gp.tc.0.init=tl.gp.MySimpleCodeFragmentBuilder \
                                    -p gp.tc.0.init.knowledge-log-file="$WITH_KNOW_STAT_DIR/MYknowinit" \
                                    -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.49.bin \
                                    -p gp.tc.0.init.transfer-percent=75 \
                                    -p gp.tc.0.init.knowledge-extraction=rootsubtree \
                                    -p seed.0=$SGE_TASK_ID

printf "Finished running experiment on target with MY knowledge\n\n\n"

printf "Running tests on target problem with MY knowledge\n\n"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$WITH_KNOW_STAT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p seed.0=$TEST_SEED
printf "Finished running tests on target with MY knowledge\n\n"
######################################################################################################################
