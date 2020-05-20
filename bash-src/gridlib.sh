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

EXPERIMENT_DESCRIPTION='Set the generation to 200 to see the behaviour of the vanilla GPHH on longer generations. No transfer learning applied.

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
    KNOWLEDGE_SOURCE_NAME="KnowledgeSource:clear_$CLEAR"
    echo "Clearing on source is set up"
else
    KNOWLEDGE_SOURCE_NAME="KnowledgeSource"
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

function test_similarity()
{
    local L_EXP_NAME="TestSimilarity"
	GENS=$@
    cp -r -v /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/population.gen.49.bin .
	# mkdir -p $GPHH_REPOSITORY_SOURCE/$SLURM_ARRAY_TASK_ID
	# cd $GPHH_REPOSITORY_SOURCE/$SLURM_ARRAY_TASK_ID

	java -cp .:tl.jar tl.gp.TestSimilarity carp_param_base.param population.gen.49.bin simtest \
					    eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                                            stat.file="\$EvaluateOnTest.job.0.out.stat" \
                                            eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE \
                                            eval.problem.eval-model.instances.0.samples=500 \
                                            stat.gen-pop-file=eval.population.gen \
                                            generations=$GENERATIONS \
                                            seed.0=$TEST_SEED

    SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE:gen_$GENERATIONS/"$L_EXP_NAME/$SGE_TASK_ID/"
    mkdir -p $SAVE_TO
    cp -r -v *.* $SAVE_TO/
}

# This is done on the target domain. This experiment does not use transfer learning.
function do_non_knowledge_experiment()
{
    echo "Running experiment on target problem without knowledge $CLEAR"
    if [ "$CLEAR" == "true" ]; then
        L_EXP_NAME="WithoutKnowledge:clear_$CLEAR"
    else
        L_EXP_NAME="WithoutKnowledge"
    fi
    WITHOUT_KNOW_STAT_DIR="./$L_EXP_NAME/$SGE_TASK_ID"
    java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                    -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                    -p generations=$GENERATIONS_ON_TARGET \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p clear=$CLEAR \
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
    L_EXPERIMENT_NAME=$1
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

# This function performs the Surrogate-EvaluatedFulltree experiment. This method gets a path to a directory or file that
# contains knowledge, loads the population from it, performs a clearing on it and forms a surrogate pool from the
# cleared population. The experiment creates an intermediate population of 10 times the size of the originial
# population, evaluates the intermediate population with the surrogate, removes duplicates with a clearing method and
# then, initialise a percentage of target domains from the top individuals of the cleared population. This function has
# the foloowing parameters:
# 1. Percentage of the target population to initialise,
# 2. Similarity metric:
#       2.1. phenotypic
#       2.2. corrphenotypic
#       2.3. hamming
# 3. Generation of source domain from which to start loading populations (inclusive)
# 4. Generation of source domain until which which to start loading populations (inclusive)
# 5. Niche radius.
function SurEvalFullTree()
{
  local L_EXP_NAME="SurEvalFullTree:tp_$1:metric_$2:gen_$3_$4:nrad_$5:dms_20"
  local L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"
  do_knowledge_experiment "$L_EXP_NAME" \
                          -p state=tl.gphhucarp.dms.DMSSavingGPHHState \
                          -p gp.tc.0.init=tl.knowledge.surrogate.SurEvalBuilder \
                          -p gp.tc.0.init.knowledge-path=$KNOWLEDGE_SOURCE_DIR/ \
                          -p gp.tc.0.init.num-generations=$GENERATIONS \
                          -p gp.tc.0.init.surr-log-path=$L_EXPERIMENT_DIR/ \
                          -p gp.tc.0.init.transfer-percent=$1 \
                          -p gp.tc.0.init.distance-metric=$2 \
                          -p gp.tc.0.init.from-generation=$3 \
                          -p gp.tc.0.init.to-generation=$4 \
                          -p gp.tc.0.init.niche-radius=$5

#gp.tc.0.init.surr-log-path=./stats/target-wk/SurEvalFullTree/
}

# This function performs the Surrogate-EvaluatedFulltree experiment without surrogate evaluation. newly-created
#	 rees will not be evaluated with the surrogate method and a fitness value of -1 will be
#	 assigned to them. This feature acts as a control measure for testing to see if the results come from the
#	 surrogate or not. This function has the foloowing parameters:
# 1. Percentage of the target population to initialise,
# 2. Similarity metric:
#       2.1. phenotypic
#       2.2. corrphenotypic
#       2.3. hamming
# 3. Generation of source domain from which to start loading populations (inclusive)
# 4. Generation of source domain until which which to start loading populations (inclusive)
# 5. Niche radius.
# 6. Niche capacity
function RandSurEvalFullTree()
{
  local L_EXP_NAME="SurEvalFullTree:tp_$1:metric_$2:gen_$3_$4:nrad_$5:ncap_$6:dms_20"
  local L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"
  do_knowledge_experiment "$L_EXP_NAME" \
                          -p state=tl.gphhucarp.dms.DMSSavingGPHHState \
                          -p gp.tc.0.init=tl.knowledge.surrogate.SurEvalBuilder \
                          -p gp.tc.0.init.knowledge-path=$KNOWLEDGE_SOURCE_DIR/ \
                          -p gp.tc.0.init.num-generations=$GENERATIONS \
                          -p gp.tc.0.init.surr-log-path=$L_EXPERIMENT_DIR/ \
                          -p gp.tc.0.init.transfer-percent=$1 \
                          -p gp.tc.0.init.distance-metric=$2 \
                          -p gp.tc.0.init.from-generation=$3 \
                          -p gp.tc.0.init.to-generation=$4 \
                          -p gp.tc.0.init.disable-sur-eval=true \
                          -p gp.tc.0.init.niche-radius=$5 \
                          -p gp.tc.0.init.niche-radius=$6

#gp.tc.0.init.surr-log-path=./stats/target-wk/SurEvalFullTree/
}

# This function performs the cleared fulltree experiment. This method gets a path to a directory or file that contains
# knowledge, loads the population from it, performs a clearing on it and forms a pool from the cleared population to
# initialise a percentage of target domains. This function has the foloowing parameters:
# 1. Percentage of the target population to initialise,
# 2. Similarity metric:
#       2.1. phenotypic
#       2.2. corrphenotypic
#       2.3. hamming
# 3. Generation of source domain from which to start loading populations (inclusive)
# 4. Generation of source domain until which which to start loading populations (inclusive)
# 5. Niche radius.
# 6. Niche capacity
function ClearedFullTree()
{
  local L_EXP_NAME="ClearedFullTree:tp_$1:metric_$2:gen_$3_$4:nrad_$5:ncap_$6:dms_20"
  do_knowledge_experiment "$L_EXP_NAME" \
                          -p state=tl.gphhucarp.dms.DMSSavingGPHHState \
                          -p gp.tc.0.init=tl.gp.ClearedFullTreeBuilder \
                          -p gp.tc.0.init.knowledge-path="$KNOWLEDGE_SOURCE_DIR"/ \
                          -p gp.tc.0.init.transfer-percent="$1" \
                          -p gp.tc.0.init.distance-metric="$2" \
                          -p gp.tc.0.init.num-generations=$GENERATIONS \
                          -p gp.tc.0.init.from-generation="$3" \
                          -p gp.tc.0.init.to-generation="$4" \
                          -p gp.tc.0.init.niche-radius="$5" \
                          -p gp.tc.0.init.niche-radius="$6"
}

# This function performs the surrogate-assisted transfer learning.
# The function takes the following parameters:
#   1. Init the surrogate pool in the builder. If "false" (the default), the surrogate pool will not be initialised and
#   hence transferred from the source domain. Don't forget the quotation marks.
#   2. Transfer percent (in [0, 1]), the percent of the initial population that is transferred exactly from the source
#   domain.
#   3. KNN distance metric:
#       3.1. phenotypic
#       3.2. corrphenotypic
#       3.3. hamming
#   4. evaluate surrogate pool on initialisatoin: "true" or "false". This parameter is not reflected in experiment names.
#   5. Average fitness for duplicate individuals: "true", "false"
#   6. surupol: Surrogate update policy. Acceptable values are:
#       6.1. FIFOPhenotypic
#       6.2. AddOncePhenotypic
#       6.3. FIFONoDupPhenotypic
#       6.4. Reset
#       6.5. UnboundedPhenotypic
#       6.6. Unbounded
#       6.7. Entropy
#       6.8. CorrEntropy
#   7. If surrogate updade policy is "CorrEntropy", then this represents eps.
#   8. If surrogate updade policy is "CorrEntropy", then this represents mininum cluster size.
function Surrogate()
{
#L_SURUPOL=""
if [ "$6" == "corrphenotypic" ]
then
  L_SURUPOL="$6_eps_$7_minpt_$8"
else
  L_SURUPOL="$6"
fi

local L_EXP_NAME="Surrogate:initsurpool_$1:tp_$2:knndistmetr_$3:avefitdup_$5:dms_30:surupol_$L_SURUPOL"
local L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"
do_knowledge_experiment $L_EXP_NAME \
                        -p state=gphhucarp.gp.SurrogatedGPHHEState \
                        -p pop.subpop.0.species.ind=tl.knowledge.surrogate.SuGPIndividual \
                        -p surrogate-state.surr-log-path=$L_EXPERIMENT_DIR/ \
                        -p surrogate-state.surrogate-updpool-policy=$6 \
                        -p surrogate-state.eval-surpool-on-init=$4 \
                        -p gp.tc.0.init=ec.SurrogateBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.knowledge-log-file=$L_EXPERIMENT_DIR/SurrogateBuilderLog \
                        -p gp.tc.0.init.init-surrogate-pool=$1 \
                        -p gp.tc.0.init.transfer-percent=$2 \
                        -p surrogate-state.surrogate-average-dup-fitness=$5 \
                        -p surrogate-state.knn-distance-metric=$3 \
                        -p surrogate-state.corr-entropy.eps=$7 \
                        -p surrogate-state.corr-entropy.min-cluster-size=$8
}

# This function performs the ensemble-surrogate-assisted transfer learning.
# The function takes the following parameters:
#   1. Init the surrogate pool in the builder. If "false" (the default), the surrogate pool will not be initialised and
#   hence transferred from the source domain. Don't forget the quotation marks.
#   2. Transfer percent (in [0, 1]), the percent of the initial population that is transferred exactly from the source
#   domain.
#   3. KNN distance metric:
#       3.1. phenotypic
#       3.2. corrphenotypic
#   4. evaluate surrogate pool on initialisatoin: "true" or "false". This parameter is not reflected in experiment names.
function EnsembleSurrogate()
{
local L_EXP_NAME="EnsembleSurrogate:initsurpool_$1:tp_$2:knndistmetr_$3:dms_30"
local L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"
do_knowledge_experiment $L_EXP_NAME \
                        -p state=gphhucarp.gp.EnsembleSurrogatedGPHHState \
                        -p pop.subpop.0.species.ind=tl.knowledge.surrogate.SuGPIndividual \
                        -p ensemble-surrogate-state.surr-log-path=$L_EXPERIMENT_DIR/ \
                        -p ensemble-surrogate-state.eval-surpool-on-init=$4 \
                        -p gp.tc.0.init=tl.gp.EnsembleSurrogateBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.knowledge-log-file=$L_EXPERIMENT_DIR/EnsembleSurrogateBuilderLog \
                        -p gp.tc.0.init.init-surrogate-pool=$1 \
                        -p gp.tc.0.init.transfer-percent=$2 \
                        -p ensemble-surrogate-state.knn-distance-metric=$3
}

function PPTEvolutionState()
{
ecj_experiment "PPTEvolutionState:lr_$1:radius_$2:cap_$3" $DATASET_CATEGORY_SOURCE $DATASET_SOURCE $NUM_VEHICLES_SOURCE \
                -p state=tl.gp.PPTEvolutionState \
                -p ppt-state.lr=$1 \
                -p ppt-state.sample-size=100 \
                -p ppt-state.tournament-size=20 \
                -p ppt-state.knowledge-log-file=PPTEvolutionStateLog \
                -p ppt-state.niche-radius=$2 \
                -p ppt-state.niche-capacity=$3
}


# This function performs the experiment in which a new GP operator is introduced that creates a portion of
# the population from a PPT that it also adapts during the GP run.
# The function has the following input parameters:
# 1. Probability of PPT breeding
# 2. Probability of crossover breeding
# 3. Probability of mutation breeding
# 4. Probability of reproduction breeding
# 5. Learning rate
# 6. Sample size
# 7. Tournament size
# 8. Transfer percent
# 9. From source generation to learn initial PPT
# 10. To source generation to learn initial PPT
# 11. Niching radius to learn initial PPT
# 12. Niching capacity to learn initial PPT
# 13. Minimum probability threshold of PPT items
# 14. The probability of using the PPT complement in the PPT breeding pipeline
# 15. Clear: Using clearing on target domain
function PPTBreeding()
{
L_EXP_NAME="PPTBreeding:ppt_$1:cmpppt_${14}:xover_$2:mut_$3:repro_$4:lr_$5:ss_$6:ts:_$7:initperc_$8:igen_$9_${10}:inrad_${11}:incap_${12}:mnThr_${13}:clear_${15}"
printf "Begining the experiment: $L_EXP_NAME.\n"

L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"

java -cp .:tl.jar tl.knowledge.extraction.ExtractPPT carp_param_base.param $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                        eval.problem.eval-model.instances.0.samples=500 \
                        seed.0=$TEST_SEED \
                        extract-ppt.knowledge-log-file=$L_EXPERIMENT_DIR/PPTExtractionLog \
                        extract-ppt.num-generations=$GENERATIONS \
                        extract-ppt.from-generation=$9 \
                        extract-ppt.to-generation=${10} \
                        extract-ppt.fitness-niche-radius=${11} \
                        extract-ppt.fitness-niche-capacity=${12} \
                        extract-ppt.lr=0 \
                        extract-ppt.learner='frequency' \
                        extract-ppt.sample-size=$6 \
                        extract-ppt.tournament-size=$7 \
                        extract-ppt.min-prob-thresh=${13} \
                        generation=$GENERATIONS

do_knowledge_experiment $L_EXP_NAME \
                        -p pop.subpop.0.species.pipe.num-sources=4 \
                        -p pop.subpop.0.species.pipe.source.0=ec.gp.koza.CrossoverPipeline \
                        -p pop.subpop.0.species.pipe.source.0.prob=$2 \
                        -p pop.subpop.0.species.pipe.source.1=ec.gp.koza.MutationPipeline \
                        -p pop.subpop.0.species.pipe.source.1.prob=$3 \
                        -p pop.subpop.0.species.pipe.source.2=ec.breed.ReproductionPipeline \
                        -p pop.subpop.0.species.pipe.source.2.prob=$4 \
                        -p pop.subpop.0.species.pipe.source.3=tl.knowledge.ppt.gp.PPTBreedingPipeline \
                        -p pop.subpop.0.species.pipe.source.3.prob=$1 \
                        -p pop.subpop.0.species.pipe.source.3.complement-probability=${14} \
                        -p pop.subpop.0.species.pipe.source.3.knowledge-log-file=$L_EXPERIMENT_DIR/PPTBreedLog \
                        -p state=tl.gp.PPTEvolutionState \
                        -p ppt-state.lr=$5 \
                        -p ppt-state.sample-size=$6 \
                        -p ppt-state.tournament-size=$7 \
                        -p ppt-state.ppt-stat-log=$L_EXPERIMENT_DIR/PPTStatLog \
                        -p ppt-state.ppt-log=$L_EXPERIMENT_DIR/PPTLog \
                        -p gp.tc.0.init=tl.gp.PPTBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        -p clear=${15} \
                        -p gp.tc.0.init.transfer-percent=$8

cp -p -v $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt $SAVE_TO/$SGE_TASK_ID
}


function TournamentFullTreeExp()
{
do_knowledge_experiment TournamentFullTreeTree:tp_$1:tournament_$2 \
                        -p gp.tc.0.init=tl.gp.TournamentFullTreeBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.knowledge-extraction=root \
                        -p gp.tc.0.init.tournament-size=$2
}

# This function performs the transfer learning experiment 'MutatingSubtree'.
# This function takes 7 input parameters:
#   1. the transfer percent (in (0, 100])
#   2. extraction method: acceptable values are:
#       + rootsubtree
#       + all
#       + root
#   3. number of mutation (must be greater than zero)
#   4. simplify: true or false
#   5. target percent: in (0, 1]
#   6. niche radius: negative values disable niching
#   7. niche capacity
function MutatingSubtree()
{
do_knowledge_experiment MutatingSubtree:tp_$1:ext_$2:mut_$3:sim_$4:tgtp_$5:rad_$6:cap_$7 \
                        -p gp.tc.0.init=tl.gp.MutatingSubtreeBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.knowledge-extraction=$2 \
                        -p gp.tc.0.init.ns.0=ec.gp.koza.KozaNodeSelector \
                        -p gp.tc.0.init.num-mutated=$3 \
                        -p gp.tc.0.init.simplify=$4 \
                        -p gp.tc.0.init.target-percent=$5 \
                        -p gp.tc.0.init.niche-radius=$6 \
                        -p gp.tc.0.init.niche-capacity=$7
}

# This function performs the transfer learning experiment 'SubTree'.
# This function takes one input parameter: the transfer percent.
function SubTree()
{
ls

L_EXP_NAME="Subtree:perc_$1:clear_$CLEAR"

do_knowledge_experiment $L_EXP_NAME \
                        -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.knowledge-extraction=rootsubtree
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

# This function performs the transfer learning experiment 'BestGen'.
# This function takes one input parameter: the number of individuals to take from the population of each generation.
function BestGen()
{
do_knowledge_experiment BestGen:k_$1 \
                        -p gp.tc.0.init=tl.gp.BestGenKnowledgeBuilder \
                        -p gp.tc.0.init.k=$1 \
                        -p gp.tc.0.init.knowledge-folder=$KNOWLEDGE_SOURCE_DIR/
}

# This function performs the transfer learning experiment 'GTLKnow'.
# This function does not takes any input parameter.
function GTLKnow()
{
do_knowledge_experiment GTLKnowlege  \
                        -p gp.tc.0.init=tl.gp.GTLKnowlegeBuilder \
                        -p gp.tc.0.init.knowledge-folder=$KNOWLEDGE_SOURCE_DIR/
}

# This function performs the transfer learning experiment "FrequentSub'.
# The function takes three input arguments:
# 1. the extraction method. Acceptable values are:
#   - rootsubtree
#   - all
#   - root
# 2. extraction percent: the percent of initial population to consider for extraction, must be in [0, 1].
# 3. transfer percent: the percent of initial population of target domain to create from the extracted subtrees, must be in [0, 1].
function FrequentSub()
{
L_EXP_NAME="FrequentSub:extract_$1:extperc_$2:tranperc_$3:clear_$CLEAR"
do_knowledge_experiment $L_EXP_NAME \
                        -p gp.tc.0.init=tl.gp.FrequentCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-directory=$KNOWLEDGE_SOURCE_DIR/ \
                        -p gp.tc.0.init.knowledge-extraction=$1 \
                        -p gp.tc.0.init.extract-percent=$2 \
                        -p gp.tc.0.init.transfer-percent=$3 \
                        -p gp.tc.0.init.min-cf-depth=2 \
                        -p gp.tc.0.init.max-cf-depth=8
}

# This function performs the experiment in which a new GP operator is introduced that creates a portion of
# the population from a PPT that it also adapts during the GP run. This operator also performs a mutation on
# the created individual and if the probability of the mutated one is less than that of the original one and
# its fitness better, then the mutated one will be used and otherwise, the original one.
# The function has the following input parameters:
# 1. Probability of PPT breeding
# 2. Probability of crossover breeding
# 3. Probability of mutation breeding
# 4. Probability of reproduction breeding
# 5. Learning rate
# 6. Sample size
# 7. Tournament size
# 8. Transfer percent
# 9. From source generation to learn initial PPT
# 10. To source generation to learn initial PPT
# 11. Niching radius to learn initial PPT
# 12. Niching capacity to learn initial PPT
# 13. Minimum probability threshold of PPT items
function PPTMutIndBreeding()
{
L_EXP_NAME="PPTMutIndBreeding:ppt_$1:xover_$2:mut_$3:repro_$4:lr_$5:ss_$6:ts:_$7:initperc_$8:igen_$9_${10}:inrad_${11}:incap_${12}:mnThr_${13}"
printf "Begining the experiment: $L_EXP_NAME.\n"

L_EXPERIMENT_DIR="$L_EXP_NAME/$SGE_TASK_ID"

java -cp .:tl.jar tl.knowledge.extraction.ExtractPPT carp_param_base.param $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                        eval.problem.eval-model.instances.0.samples=500 \
                        seed.0=$TEST_SEED \
                        extract-ppt.knowledge-log-file=$L_EXPERIMENT_DIR/PPTExtractionLog \
                        extract-ppt.num-generations=$GENERATIONS \
                        extract-ppt.from-generation=$9 \
                        extract-ppt.to-generation=${10} \
                        extract-ppt.fitness-niche-radius=${11} \
                        extract-ppt.fitness-niche-capacity=${12} \
                        extract-ppt.lr=0 \
                        extract-ppt.learner='frequency' \
                        extract-ppt.sample-size=$6 \
                        extract-ppt.tournament-size=$7 \
                        extract-ppt.min-prob-thresh=${13} \
                        generation=$GENERATIONS

do_knowledge_experiment $L_EXP_NAME \
                        -p pop.subpop.0.species.pipe.num-sources=4 \
                        -p pop.subpop.0.species.pipe.source.0=ec.gp.koza.CrossoverPipeline \
                        -p pop.subpop.0.species.pipe.source.0.prob=$2 \
                        -p pop.subpop.0.species.pipe.source.1=ec.gp.koza.MutationPipeline \
                        -p pop.subpop.0.species.pipe.source.1.prob=$3 \
                        -p pop.subpop.0.species.pipe.source.2=ec.breed.ReproductionPipeline \
                        -p pop.subpop.0.species.pipe.source.2.prob=$4 \
                        -p pop.subpop.0.species.pipe.source.3=tl.knowledge.ppt.gp.PPTMutIndBreedingPipeline \
                        -p pop.subpop.0.species.pipe.source.3.prob=$1 \
                        -p pop.subpop.0.species.pipe.source.3.mut-builder=ec.gp.koza.GrowBuilder \
                        -p pop.subpop.0.species.pipe.source.3.mut-selector=ec.gp.koza.KozaNodeSelector \
                        -p pop.subpop.0.species.pipe.source.3.knowledge-log-file=$L_EXPERIMENT_DIR/PPTBreedLog \
                        -p state=tl.gp.PPTEvolutionState \
                        -p ppt-state.lr=$5 \
                        -p ppt-state.sample-size=$6 \
                        -p ppt-state.tournament-size=$7 \
                        -p ppt-state.ppt-stat-log=$L_EXPERIMENT_DIR/PPTStatLog \
                        -p ppt-state.ppt-log=$L_EXPERIMENT_DIR/PPTLog \
                        -p gp.tc.0.init=tl.gp.PPTBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        -p gp.tc.0.init.transfer-percent=$8

cp -p -v $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt $SAVE_TO/$SGE_TASK_ID
}

# This function performs the transfer learning experiment 'PPTLearning'.
# The function, at the moment, takes 8 parameters: 
#   1. percent of initial population on target domain to create with this TL method
#   2. niche radius when learning the PPT.
#   3. niche capacity.
#   4. from generation 
#   5. to generation
#   6. learning rate
#   7. sample size (originally used 100)
#   8. tournament size (originally used 20)
#   9. simplify: boolean value. 
function ExtractPPTFreq()
{

L_EXP_NAME=PPTFreqLearning:percent_$1:nrad_$2:ncap_$3:gens_$4_$5:lr_$6:ss_$7:ts_$8 #:sim_$9
printf "Begining to extract the PPT: $L_EXP_NAME.\n" 

java -cp .:tl.jar tl.knowledge.extraction.ExtractPPT carp_param_base.param $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                        eval.problem.eval-model.instances.0.samples=500 \
                        seed.0=$TEST_SEED \
                        extract-ppt.knowledge-log-file=$KNOWLEDGE_SOURCE_DIR/PPTExtractionLog \
                        extract-ppt.num-generations=$GENERATIONS \
                        extract-ppt.from-generation=$4 \
                        extract-ppt.to-generation=$5 \
                        extract-ppt.fitness-niche-radius=$2 \
                        extract-ppt.fitness-niche-capacity=$3 \
                        extract-ppt.lr=$6 \
                        extract-ppt.learner='frequency' \
                        extract-ppt.sample-size=$7 \
                        extract-ppt.tournament-size=$8 \
                        generation=$GENERATIONS

#                         extract-ppt.simplify=$9 \

do_knowledge_experiment $L_EXP_NAME \
                        -p gp.tc.0.init=tl.gp.PPTBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        -p gp.tc.0.init.transfer-percent=$1 
                        
cp -p -v $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt $SAVE_TO/$SGE_TASK_ID
}


# This function performs the transfer learning experiment 'PPTLearning'.
# The function, at the moment, takes 8 parameters: 
#   1. percent of initial population on target domain to create with this TL method
#   2. niche radius when learning the PPT (not used yet).
#   3. niche capacity (not used yet).
#   4. from generation 
#   5. to generation
#   6. learning rate 
#   7. epsilon (originally used 0.1)
#   8. clr (originally used 0.2)
#   9. simplify: boolean value. 
function ExtractPPTPipe()
{

L_EXP_NAME=PPTPipeLearning:percent_$1:nrad_$2:ncap_$3:gens_$4_$5:lr_$6:eps_$7:clr_$8 #:sim_$9
printf "Begining to extract the PPT: $L_EXP_NAME.\n" 

java -cp .:tl.jar tl.knowledge.extraction.ExtractPPT carp_param_base.param $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                        eval.problem.eval-model.instances.0.samples=500 \
                        seed.0=$TEST_SEED \
                        extract-ppt.knowledge-log-file=$KNOWLEDGE_SOURCE_DIR/PPTExtractionLog \
                        extract-ppt.num-generations=$GENERATIONS \
                        extract-ppt.from-generation=$4 \
                        extract-ppt.to-generation=$5 \
                        extract-ppt.fitness-niche-radius=$2 \
                        extract-ppt.fitness-niche-capacity=$3 \
                        extract-ppt.lr=$6 \
                        extract-ppt.learner='pipe' \
                        extract-ppt.epsilon=$7 \
                        extract-ppt.clr=$8 \
                        generation=$GENERATIONS

#                         extract-ppt.simplify=$9 \
                        
do_knowledge_experiment $L_EXP_NAME \
                        -p gp.tc.0.init=tl.gp.PPTBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt \
                        -p gp.tc.0.init.transfer-percent=$1 
                        
cp -p -v $KNOWLEDGE_SOURCE_DIR/$L_EXP_NAME.ppt $SAVE_TO/$SGE_TASK_ID
}
