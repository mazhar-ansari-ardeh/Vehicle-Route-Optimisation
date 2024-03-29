#!/usr/bin/env bash

# Setting variables that config the experiment.

if [ -z "$NUM_VEHICLES_SOURCE" ]
then
    NUM_VEHICLES_SOURCE=6
fi
echo "Number of source vehicles: $NUM_VEHICLES_SOURCE"

# if [ -z "$NUM_VEHICLES_TARGET" ]
# then
#     NUM_VEHICLES_TARGET=7
# fi
echo "Number of target vehicles: $NUM_VEHICLES_TARGET"

if [ -z "$DATASET_CATEGORY_SOURCE" ]
then
    DATASET_CATEGORY_SOURCE="gdb"
fi

# if [ -z "$DATASET_CATEGORY_TARGET" ]
# then
#     DATASET_CATEGORY_TARGET=$DATASET_CATEGORY_SOURCE
# fi

if [ -z "$DATASET_SOURCE" ]
then
    DATASET_SOURCE="gdb21"
fi

# if [ -z "$DATASET_TARGET" ]
# then
#     DATASET_TARGET=$DATASET_SOURCE
# fi

echo "Dataset: $DATASET_CATEGORY_SOURCE/$DATASET_SOURCE"
echo "Dataset target: $DATASET_CATEGORY_TARGET/$DATASET_TARGET"

# STAT_ROOT="stats"

GENERATIONS=50

# CLEAR="true"

# Number of generations of the experiment on target domain
GENERATIONS_ON_TARGET=50

EXPERIMENT_DESCRIPTION='

'

printf "**********************************************************************************************************************************\n"
printf "Experiment description:\n"
echo "$EXPERIMENT_DESCRIPTION"
printf "**********************************************************************************************************************************\n"



# Helper variables that are created based on setting variables.
# Do not modify these variables.

# Path to dataset file of source domain
DATASET_FILE_SOURCE="$DATASET_CATEGORY_SOURCE/$DATASET_SOURCE.dat"

# Path to dataset file of target domain
DATASET_FILE_TARGET="$DATASET_CATEGORY_TARGET/$DATASET_TARGET.dat"

GPHH_REPOSITORY_SOURCE="$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE:gen_$GENERATIONS"
# GPHH_REPOSITORY_TARGET="$DATASET_TARGET.vs$NUM_VEHICLES_TARGET:gen_$GENERATIONS"

# The directory that contains the population of GP for solving the source domain.
if [ "$CLEAR" == "true" ]; then
    KNOWLEDGE_SOURCE_NAME="KnowledgeSource:fitness_norm:clear_$CLEAR"
    echo "Clearing on source is set up"
else
    KNOWLEDGE_SOURCE_NAME="KnowledgeSource:fitness_norm"
    CLEAR="false"
fi
KNOWLEDGE_SOURCE_DIR="./$KNOWLEDGE_SOURCE_NAME/$SGE_TASK_ID"

# Extracted knowledge. This is the name of the file that will contain the results of programms such as
# 'AnalyzeTerminals' that output their results in an external file.
# KNOWLEDGE_FILE_BASE="$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE"


echo "$DATASET_CATEGORY_SOURCE/$DATASET_SOURCE"
echo "$NUM_VEHICLES_SOURCE to $NUM_VEHICLES_TARGET"

# Helper variables that are created based on setting variables.
# Do not modify these variables.

# The seed value that will be used for testing models found. This value must be unique
TEST_SEED=25556

if [ -z "$SGE_TASK_ID" ]
then
    SGE_TASK_ID=3
    printf "SGE_TASK_ID was not set. Set it to 3.\n\n"
fi
echo "SGE_TASK_ID: $SGE_TASK_ID"
if [ -z "$JOB_ID" ]
then
    JOB_ID=4
    printf "JOB_ID was not set. Set it to 4. \n\n"
fi
echo "JOB_ID: $JOB_ID"



# This is actually the part that runs the source domain.
function run_source_domain()
{
# Note to self: Modified this slightly to test a different mutation rate. Revert the changes before using this file again normally.

# L_EXPERIMENT_NAME="KnowledgeSource"
# L_EXPERIMENT_DIR="$L_EXPERIMENT_NAME/$SGE_TASK_ID"
printf "\nBegining to run source domain (write knowledge)"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$KNOWLEDGE_SOURCE_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file=$KNOWLEDGE_SOURCE_DIR/population.gen \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                                   -p generations=$GENERATIONS \
                                   -p stat.save-pop=true \
                                   -p clear=$CLEAR \
                                   -p eval.problem=tl.gphhucarp.NormalisedReactiveGPHHProblem \
                                   -p pop.subpop.0.species.fitness=tl.gp.fitness.NormalisedMultiObjectiveFitness \
                                   -p seed.0=$SGE_TASK_ID
echo "Finished writing knowledge"

echo "Begining to test the source problem results"
java -cp .:tl.jar gphhucarp.gp.GPTest  -file carp_param_base.param \
                                       -p train-path=$KNOWLEDGE_SOURCE_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE\
                                       -p generations=$GENERATIONS \
                                       -p eval.problem.eval-model.instances.0.samples=500 \
                                       -p seed.0=$TEST_SEED
echo "Finished performing test on source problem results"

# SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$L_EXPERIMENT_NAME
# mkdir -p $SAVE_TO
# cp -r -v $L_EXPERIMENT_DIR/ $SAVE_TO/


mkdir -p /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_NAME
cp -r -v $KNOWLEDGE_SOURCE_DIR/ /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_NAME
printf "$(date)\t $SGE_TASK_ID \n" >> /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/FinishedTestEvaluations.txt
}



function copy_knowledge()
{
    printf "Copying knowledge \n"
    mkdir -p $KNOWLEDGE_SOURCE_DIR
    cp -r -v /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/TestedPopulation/*.bin ./$KNOWLEDGE_SOURCE_DIR/
    ls $KNOWLEDGE_SOURCE_DIR
    printf "Finished copying knowledge\n\n"
}


# This is different from the test evaluation. This method can evaluate the whole population of the whole generations on test settings
# This function reads the generations to evaluate from its arguments.
# Example usage:
#   evaluate_on_test 1 2 50 49
# This call will evaluate the populations of generations 1, 2, 49 50
# This function copies the files it requires itself, evaluates them and copies the results back again.
function evaluate_on_test()
{
    GENS=$@
    echo "Evaluating source population on test settings using EvaluateOnTest for generations"
    mkdir -p $KNOWLEDGE_SOURCE_DIR
    mkdir -p /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/TestedPopulation/
    for i in $GENS
    do
        cp -r -v /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/population.gen.$i.bin $KNOWLEDGE_SOURCE_DIR/
        java -cp .:tl.jar tl.gp.EvaluateOnTest carp_param_base.param  $KNOWLEDGE_SOURCE_DIR/population.gen.$i.bin $KNOWLEDGE_SOURCE_DIR/TestedPopulation \
                                            eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                                            stat.file="\$$KNOWLEDGE_SOURCE_DIR/EvaluateOnTest.job.0.out.stat" \
                                            eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE \
                                            eval.problem.eval-model.instances.0.samples=500 \
                                            stat.gen-pop-file=$KNOWLEDGE_SOURCE_DIR/eval.population.gen \
                                            generations=$GENERATIONS \
                                            seed.0=$TEST_SEED

        cp -r -v $KNOWLEDGE_SOURCE_DIR/TestedPopulation/population.gen.$i.bin /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/TestedPopulation/
        printf "Finished evaluation of generation $i on test dataset\n"
    done

#     cp -r -v $KNOWLEDGE_SOURCE_DIR/TestedPopulation /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/
    printf "Finished evaluating source population on test settings using EvaluateOnTest\n\n\n"
    printf "$(date)\t $SGE_TASK_ID \n" >> /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/FinishedTestEvaluations.txt
}

# This is done on the target domain. This experiment does not use transfer learning.
function do_non_knowledge_experiment()
{
    echo "Running experiment on target problem without knowledge $CLEAR"
    if [ "$CLEAR" == "true" ]; then
        L_EXP_NAME="WithoutKnowledge:fitness_norm:clear_$CLEAR"
    else
        L_EXP_NAME="WithoutKnowledge:fitness_norm"
    fi
    WITHOUT_KNOW_STAT_DIR="./$L_EXP_NAME/$SGE_TASK_ID"
    java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                    -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                    -p generations=$GENERATIONS_ON_TARGET \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p clear=$CLEAR \
                                    -p eval.problem=tl.gphhucarp.NormalisedReactiveGPHHProblem \
                                    -p pop.subpop.0.species.fitness=tl.gp.fitness.NormalisedMultiObjectiveFitness \
                                    -p seed.0=$SGE_TASK_ID

    echo "Finished running experiment on target $L_EXP_NAME"

    echo "Running tests on target problem $L_EXP_NAME"
    java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$WITHOUT_KNOW_STAT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p eval.problem.eval-model.instances.0.samples=500 \
                                        -p generations=$GENERATIONS_ON_TARGET \
                                        -p seed.0=$TEST_SEED

    SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE.$DATASET_TARGET.vt$NUM_VEHICLES_TARGET:gen_$GENERATIONS/"$L_EXP_NAME"
    mkdir -p $SAVE_TO
    cp -r -v $WITHOUT_KNOW_STAT_DIR/ $SAVE_TO/
    printf "$(date)\t $SGE_TASK_ID \n" >> $SAVE_TO/Finished"$L_EXP_NAME".txt

    printf "Finished running tests on target without knowledge\n\n\n"
}

# This is done on the target domain. This experiment uses transfer learning.
# This function requires one argument: the name of the experiment.
# The function defines a global variable: SAVE_TO which is the name of the directory that it saved the results to.
function do_knowledge_experiment()
{
    # The L prefix indicates that the varaible is local
    local L_EXPERIMENT_NAME="$1:fitness_norm"
    echo "Experiment arguments:" "${@}"
    printf "Running experiment on target problem with knowledge, experiment: $L_EXPERIMENT_NAME\n\n"
    L_EXPERIMENT_DIR="$1/$SGE_TASK_ID"
    java -cp .:tl.jar ec.Evolve     -file carp_param_base.param \
                                    -p stat.file="\$$L_EXPERIMENT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$L_EXPERIMENT_DIR/population.gen" \
                                    -p gp.tc.0.init.knowledge-log-file="$L_EXPERIMENT_DIR/knowinit" \
                                    -p pop.subpop.0.species.pipe.source.1.knowledge-log-file="$L_EXPERIMENT_DIR/knowmutation" \
                                    -p generations=$GENERATIONS_ON_TARGET \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p clear=$CLEAR \
                                    -p eval.problem=tl.gphhucarp.NormalisedReactiveGPHHProblem \
                                    -p pop.subpop.0.species.fitness=tl.gp.fitness.NormalisedMultiObjectiveFitness \
                                    "${@:2}" \
                                    -p seed.0=$SGE_TASK_ID

    echo "Finished running experiment: $L_EXPERIMENT_NAME"

    echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
    java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$L_EXPERIMENT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p eval.problem.eval-model.instances.0.samples=500 \
                                        -p generations=$GENERATIONS_ON_TARGET \
                                        -p seed.0=$TEST_SEED
    printf "Finished running tests on target with knowledge, experiment: $L_EXPERIMENT_NAME \n\n\n"

    SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE.$DATASET_TARGET.vt$NUM_VEHICLES_TARGET:gen_$GENERATIONS/$L_EXPERIMENT_NAME
    mkdir -p $SAVE_TO
    cp -r -v $L_EXPERIMENT_DIR/ $SAVE_TO/
	# chmod -R 777 $SAVE_TO
    printf "$(date)\t $SGE_TASK_ID \n" >> $SAVE_TO/Finished$L_EXPERIMENT_NAME.txt
}

# This function performs the transfer learning experiment 'FullTree'.
# This function takes two input parameters:
#   1. the transfer percent (in the range [0, 100])
#   2. allow duplicates.
function FullTreeExp()
{
do_knowledge_experiment FullTree:tp_$1:dup_$2:clear_$CLEAR \
                        -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.allow-duplicates=$2 \
                        -p gp.tc.0.init.knowledge-extraction=root
}
