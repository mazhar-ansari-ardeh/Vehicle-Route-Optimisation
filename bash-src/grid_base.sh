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

if [ -z "$DATASET_CATEGORY_SOURCE" ]
then
    DATASET_CATEGORY_SOURCE="gdb"
fi

if [ -z "$DATASET_CATEGORY_TARGET" ]
then
    DATASET_CATEGORY_TARGET=$DATASET_CATEGORY_SOURCE
fi

if [ -z "$DATASET_SOURCE" ]
then
    DATASET_SOURCE="gdb21"
fi

if [ -z "$DATASET_TARGET" ]
then 
    DATASET_TARGET=$DATASET_SOURCE
fi

echo "Dataset: $DATASET_CATEGORY_SOURCE/$DATASET_SOURCE"
echo "Dataset target: $DATASET_CATEGORY_TARGET/$DATASET_TARGET"

# STAT_ROOT="stats"

GENERATIONS=50

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
KNOWLEDGE_SOURCE_NAME="KnowledgeSource"
KNOWLEDGE_SOURCE_DIR="./$KNOWLEDGE_SOURCE_NAME/$SGE_TASK_ID"

# Extracted knowledge. This is the name of the file that will contain the results of programms such as 'AnalyzeTerminals' that output their results in an external file.
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
        printf "Finished evaluation of generation $i on test dataset\n"
    done

    cp -r -v $KNOWLEDGE_SOURCE_DIR/TestedPopulation /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/$KNOWLEDGE_SOURCE_DIR/
    printf "Finished evaluating source population on test settings using EvaluateOnTest\n\n\n"
    printf "$(date)\t $SGE_TASK_ID \n" >> /vol/grid-solar/sgeusers/mazhar/$GPHH_REPOSITORY_SOURCE/FinishedTestEvaluations.txt
}


# This is done on the target domain. This experiment does not use transfer learning.
function do_non_knowledge_experiment()
{
    echo "Running experiment on target problem without knowledge"
    WITHOUT_KNOW_STAT_DIR="./WithoutKnowledge/$SGE_TASK_ID"
    java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                    -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                    -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                    -p generations=$GENERATIONS \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    -p seed.0=$SGE_TASK_ID

    echo "Finished running experiment on target without knowledge"

    echo "Running tests on target problem without knowledge"
    java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$WITHOUT_KNOW_STAT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p eval.problem.eval-model.instances.0.samples=500 \
                                        -p generations=$GENERATIONS \
                                        -p seed.0=$TEST_SEED

    SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE.$DATASET_TARGET.vt$NUM_VEHICLES_TARGET:gen_$GENERATIONS/'WithoutKnowledge'
    mkdir -p $SAVE_TO
    cp -r -v $WITHOUT_KNOW_STAT_DIR/ $SAVE_TO/
    printf "$(date)\t $SGE_TASK_ID \n" >> $SAVE_TO/Finished$'WithoutKnowledge'.txt

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
                                    -p generations=$GENERATIONS \
                                    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                    "${@:2}" \
                                    -p seed.0=$SGE_TASK_ID

    echo "Finished running experiment: $L_EXPERIMENT_NAME"

    echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
    java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                        -p train-path=$L_EXPERIMENT_DIR/ \
                                        -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_TARGET \
                                        -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                        -p eval.problem.eval-model.instances.0.samples=500 \
                                        -p generations=$GENERATIONS \
                                        -p seed.0=$TEST_SEED
    printf "Finished running tests on target with knowledge, experiment: $L_EXPERIMENT_NAME \n\n\n"

    SAVE_TO=/vol/grid-solar/sgeusers/mazhar/$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE.$DATASET_TARGET.vt$NUM_VEHICLES_TARGET:gen_$GENERATIONS/$L_EXPERIMENT_NAME
    mkdir -p $SAVE_TO
    cp -r -v $L_EXPERIMENT_DIR/ $SAVE_TO/
    printf "$(date)\t $SGE_TASK_ID \n" >> $SAVE_TO/Finished$L_EXPERIMENT_NAME.txt
}


# This function performs the transfer learning experiment 'FullTree'.
# This function takes one input parameter: the transfer percent.
function FullTreeExp()
{
do_knowledge_experiment FullTree_$1 \
                        -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.knowledge-extraction=root
}

# This function performs the transfer learning experiment 'SubTree'.
# This function takes one input parameter: the transfer percent.
function SubTree()
{
ls
do_knowledge_experiment Subtree_$1 \
                        -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
                        -p gp.tc.0.init.transfer-percent=$1 \
                        -p gp.tc.0.init.knowledge-extraction=rootsubtree
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
# The function takes one input argument: the extraction method. Acceptable values are:
# - rootsubtree
# - all
# - root
function FrequentSub()
{
do_knowledge_experiment FrequentSub:Extract_$1 \
                        -p gp.tc.0.init=tl.gp.FrequentCodeFragmentBuilder \
                        -p gp.tc.0.init.knowledge-directory=$KNOWLEDGE_SOURCE_DIR/ \
                        -p gp.tc.0.init.knowledge-extraction=$1 \
                        -p gp.tc.0.init.transfer-percent=0.50 \
                        -p gp.tc.0.init.extract-percent=0.50 \
                        -p gp.tc.0.init.min-cf-depth=2 \
                        -p gp.tc.0.init.max-cf-depth=5
}

# This function performs the transfer learning experiment 'PPTLearning'.
# The function, at the moment, takes two parameters:
#   1. percent of initial population on target domain to create with this TL method
#   2. niche radius when learning the PPT.
function PPTExp()
{

printf "Begining to extract the PPT tree.\n"

java -cp .:tl.jar tl.knowledge.extraction.ExtractPPT carp_param_base.param $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_SOURCE_DIR/pipe_tree.ppt \
                        eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                        eval.problem.eval-model.instances.0.samples=500 \
                        extract-ppt.percent=0.5 \
                        extract-ppt.num-generations=$GENERATIONS \
                        extract-ppt.from-generation=49 \
                        extract-ppt.to-generation=49 \
                        extract-ppt.fitness-niche-radius=$2 \
                        extract-ppt.knowledge-log-file=$KNOWLEDGE_SOURCE_DIR/PPTExtractionLog \
                        extract-ppt.lr=0.8 \
                        extract-ppt.sample-size=100 \
                        extract-ppt.tournament-size=20 \
                        seed.0=$TEST_SEED \
                        generation=$GENERATIONS


do_knowledge_experiment PPTLearning:percent_$1 \
                        -p gp.tc.0.init=tl.gp.PPTBuilder \
                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/pipe_tree.ppt \
                        -p gp.tc.0.init.transfer-percent=$1

cp -p -v $KNOWLEDGE_SOURCE_DIR/pipe_tree.ppt $SAVE_TO/$SGE_TASK_ID
}


# Does not require any arguments
# function simplify_trees()
# {
#     echo "\nBegining to analyze subtrees knowledge"
#     SIMPLIFICATION_DIR="./Simplification/$SGE_TASK_ID"
#     mkdir -p $SIMPLIFICATION_DIR
#     KNOWLEDGE_FILE="$SIMPLIFICATION_DIR/$KNOWLEDGE_FILE_BASE.bk"
#     java -cp .:tl.jar tl.knowledge.extraction.SimplifyTrees carp_param_base.param  $KNOWLEDGE_SOURCE_DIR/ $KNOWLEDGE_FILE \
#                                                             stat.file="\$$SIMPLIFICATION_DIR/simplify_trees.0.out.stat" \
#                                                             eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
#                                                             eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
#                                                             eval.problem.eval-model.instances.0.samples=500 \
#                                                             simplify-trees.percent=0.50 \
#                                                             simplify-trees.num-generations=$GENERATIONS \
#                                                             simplify-trees.from-generation=$(($GENERATIONS-1)) \
#                                                             simplify-trees.to-generation=$(($GENERATIONS-1)) \
#                                                             simplify-trees.fitness-niche-radius=-1 \
#                                                             simplify-trees.min-st-depth=2 \
#                                                             simplify-trees.max-st-depth=20 \
#                                                             simplify-trees.knowledge-log-file="./$SIMPLIFICATION_DIR/SimplifyTreeLog" \
#                                                             generations=$GENERATIONS \
#                                                             seed.0=$TEST_SEED
#                                                             stat.gen-pop-file=$KNOWLEDGE_SOURCE_DIR/population.gen \
#     printf "Finished simplifying trees\n\n"
# }

######################################################################################################################


# run_source_domain

# evaluate_on_test {0..1}

do_non_knowledge_experiment

copy_knowledge

FullTreeExp 50

SubTree 50

FrequentSub root

PPTExp 50 -1

# GTLKnow
#
# BestGen 1
#
# BestGen 2


# evaluate_on_test 49

# simplify_trees

# do_non_knowledge_experiment

# LO_EXPERIMENT_NAME="SimpleTree:gen49_49"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.SimplifiedTreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"

# analyze_subtrees rootsubtree
 
# LO_EXPERIMENT_NAME="simpletree-gen49-49"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.SimplifiedTreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"
# 
# 
# LO_EXPERIMENT_NAME="subcontrib-gen49-49-subtree-noniche"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.ContribSubtreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"
# 
#                         
# analyze_subtrees all
# 
# 
# LO_EXPERIMENT_NAME="subcontrib-gen49-49-all-noniche"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.ContribSubtreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"


# analyze_subtrees rootsubtree
# 
# 
# LO_EXPERIMENT_NAME="subcontrib-gen49-49-subtree-abscontrib"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.ContribSubtreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"
# 
#                         
# analyze_subtrees all
# 
# 
# LO_EXPERIMENT_NAME="subcontrib-gen49-49-all-abscontrib"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.tc.0.init=tl.gp.ContribSubtreeBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/KnowledgeLog"

# analyze_terminals

# LO_EXPERIMENT_NAME="cwt-fitall-gen1-49-p1-r1-all"
# do_knowledge_experiment $LO_EXPERIMENT_NAME \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribOnlyWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=1 \
#                         -p gp.fs.0.func.0.weight-use-probability=1 \
#                         -p gp.fs.0.func.0.weight-use-policy=all \
#                         -p gp.fs.0.func.0.uniform-erc=true \
#                         -p gp.fs.0.func.0.min-weight=0.01 \
#                         -p gp.fs.0.func.0.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/TerminalERCContribOnlyWeightedLog" \
#                         -p gp.fs.0.func.0.max-weight=0.9
# 
# LO_EXPERIMENT_NAME="cwt-fitall-gen1-49-p0.8-r0.8-all"
# do_knowledge_experiment  $LO_EXPERIMENT_NAME \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribOnlyWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=0.8 \
#                         -p gp.fs.0.func.0.weight-use-probability=0.8 \
#                         -p gp.fs.0.func.0.weight-use-policy=all \
#                         -p gp.fs.0.func.0.uniform-erc=true \
#                         -p gp.fs.0.func.0.min-weight=0.01 \
#                         -p gp.fs.0.func.0.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/TerminalERCContribOnlyWeightedLog" \
#                         -p gp.fs.0.func.0.max-weight=0.9
# 
# LO_EXPERIMENT_NAME="cwt-fitall-gen1-49-p0.8-r0.8-first"
# do_knowledge_experiment $LO_EXPERIMENT_NAME  \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribOnlyWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=0.8 \
#                         -p gp.fs.0.func.0.weight-use-probability=0.8 \
#                         -p gp.fs.0.func.0.weight-use-policy=first \
#                         -p gp.fs.0.func.0.uniform-erc=true \
#                         -p gp.fs.0.func.0.min-weight=0.01 \
#                         -p gp.fs.0.func.0.knowledge-log-file="./$STAT_ROOT/$DATASET_SOURCE-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$LO_EXPERIMENT_NAME/TerminalERCContribOnlyWeightedLog" \
#                         -p gp.fs.0.func.0.max-weight=0.9


# do_knowledge_experiment FrequentSub-rootsubtree \
#                         -p gp.tc.0.init=tl.gp.FrequentCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-directory=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5
# 

# do_knowledge_experiment FrequentSub-all \
#                         -p gp.tc.0.init=tl.gp.FrequentCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-directory=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=all \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5
#                         
# do_knowledge_experiment FrequentSub-root \
#                         -p gp.tc.0.init=tl.gp.FrequentCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-directory=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=root \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5


# 
# do_knowledge_experiment subtree25 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=25 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#    

# do_knowledge_experiment subtree50 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=50 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#                         
# do_knowledge_experiment subtree75 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=75 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree 
# 
# do_knowledge_experiment subtree100 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=100 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree 
#                         
# 
# do_knowledge_experiment fulltree25 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=25 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#                         
# do_knowledge_experiment fulltree50 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=50 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#                         
# do_knowledge_experiment fulltree75 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=75 \
#                         -p gp.tc.0.init.knowledge-extraction=root 
# 

# do_knowledge_experiment fulltree100 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=100 \
#                         -p gp.tc.0.init.knowledge-extraction=root 
#                         
# do_knowledge_experiment BestGenKnowledge1 \
#                         -p gp.tc.0.init=tl.gp.BestGenKnowledgeBuilder \
#                         -p gp.tc.0.init.k=1 \
#                         -p gp.tc.0.init.knowledge-folder=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree 
#                         
# do_knowledge_experiment BestGenKnowledge2 \
#                         -p gp.tc.0.init=tl.gp.BestGenKnowledgeBuilder \
#                         -p gp.tc.0.init.k=2 \
#                         -p gp.tc.0.init.knowledge-folder=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree 
#                         
# do_knowledge_experiment GTLKnowlege  \
#                         -p gp.tc.0.init=tl.gp.GTLKnowlegeBuilder \
#                         -p gp.tc.0.init.k=2 \
#                         -p gp.tc.0.init.knowledge-folder=$KNOWLEDGE_SOURCE_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree 
#                         
# do_knowledge_experiment TLGPCriptor \
#                         -p gp.tc.0.init=tl.gp.TLGPCriptorBuilder \
#                         -p gp.tc.0.init.knowledge-probability=0.5 \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree \
#                         -p pop.subpop.0.species.pipe.source.1 = tl.gp.TLGPCriptorMutation \
#                         -p pop.subpop.0.species.pipe.source.1.knowledge-probability=0.5 \
  

# do_knowledge_experiment  FrequentSub-all \
#                         -p gp.tc.0.init=tl.gp.DepthedFrequentSimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE.pop.bin \
#                         -p gp.tc.0.init.knowledge-extraction=all \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5 \
#                         -p seed.0=$SGE_TASK_ID
#                         
# do_knowledge_experiment   FrequentSub-root \
#                         -p gp.tc.0.init=tl.gp.DepthedFrequentSimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE.pop.bin \
#                         -p gp.tc.0.init.knowledge-extraction=root \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5 \
#                         -p seed.0=$SGE_TASK_ID
#                         
# do_knowledge_experiment  FrequentSub-rootsubtree \
#                         -p gp.tc.0.init=tl.gp.DepthedFrequentSimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE.pop.bin \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree \
#                         -p gp.tc.0.init.transfer-percent=0.50 \
#                         -p gp.tc.0.init.extract-percent=0.50 \
#                         -p gp.tc.0.init.min-cf-depth=2 \
#                         -p gp.tc.0.init.max-cf-depth=5 \
#                         -p seed.0=$SGE_TASK_ID


# # ######################################################################################################################
# echo "\nBegining to analyze knowledge"
# KNOWLEDGE_FILE="$KNOWLEDGE_FILE_BASE.bk"
# java -cp .:tl.jar tl.gp.AnalyzeTerminals carp_param_base.param  $KNOWLEDGE_SOURCE_DIR/population.gen.$(($GENERATIONS-1)).bin $KNOWLEDGE_FILE \
#                                                         stat.file="\$$KNOWLEDGE_SOURCE_DIR/job.0.out.stat" \
#                                                         eval.problem.eval-model.instances.0.file=$DATASET_FILE_SOURCE \
#                                                         eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
#                                                         eval.problem.eval-model.instances.0.samples=500 \
#                                                         stat.gen-pop-file=$KNOWLEDGE_SOURCE_DIR/population.gen \
#                                                         analyze-terminals.percent=0.5 \
#                                                         generations=$GENERATIONS \
#                                                         seed.0=$SGE_TASK_ID
# echo "Finished extracting knowledge"
# # ######################################################################################################################
                        
                        
                        
# do_knowledge_experiment freq-weigh-terminal-p1-r1-all \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=1 \
#                         -p gp.fs.0.func.0.weight-use-probability=1 \
#                         -p gp.fs.0.func.0.weight-use-policy=all \
# 
# 
# do_knowledge_experiment freq-weigh-terminal-p0.8-r0.8-all \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=0.8 \
#                         -p gp.fs.0.func.0.weight-use-probability=0.8 \
#                         -p gp.fs.0.func.0.weight-use-policy=all \
#                         
# do_knowledge_experiment freq-weigh-terminal-p0.8-r0.8-first \
#                         -p gp.fs.0.func.0=tl.gphhucarp.TerminalERCContribWeighted \
#                         -p gp.fs.0.func.0.terminal-file=$KNOWLEDGE_FILE \
#                         -p gp.fs.0.func.0.weight-use-degeneration-rate=0.8 \
#                         -p gp.fs.0.func.0.weight-use-probability=0.8 \
#                         -p gp.fs.0.func.0.weight-use-policy=first \
