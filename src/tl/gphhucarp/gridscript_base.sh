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

GENERATIONS=50


# Helper variables that are created based on setting variables.
# Do not modify these variables.

# Path to dataset file
DATASET_FILE="$DATASET_CATEGORY/$DATASET.dat"

# Write knowledge
WRITE_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-writeknow"

# Extract knowledge
KNOWLEDGE_FILE_BASE="$DATASET-v$NUM_VEHICLES_SOURCE"


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



function do_knowledge_experiment()
{
# The L prefix indicates that the varaible is loval
L_EXPERIMENT_NAME=$1
echo "Experiment arguments:" "${@}"
printf "Running experiment on target problem with knowledge, experiment: $L_EXPERIMENT_NAME\n\n"
L_WITH_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-to-$NUM_VEHICLES_TARGET-wk-$L_EXPERIMENT_NAME"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$L_WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file="$L_WITH_KNOW_STAT_DIR/population.gen" \
                                   -p gp.tc.0.init.knowledge-log-file="$L_WITH_KNOW_STAT_DIR/knowinit" \
                                   -p pop.subpop.0.species.pipe.source.1.knowledge-log-file="$L_WITH_KNOW_STAT_DIR/knowmutation" \
                                   -p generations=$GENERATIONS \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                   "${@:2}" \
                                   -p seed.0=$SGE_TASK_ID

echo "Finished running experiment: $L_EXPERIMENT_NAME"

echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                       -p train-path=$L_WITH_KNOW_STAT_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                       -p eval.problem.eval-model.instances.0.samples=500 \
                                       -p generations=$GENERATIONS \
                                       -p seed.0=$TEST_SEED
printf "Finished running tests on target with knowledge, experiment: $L_EXPERIMENT_NAME \n\n\n"
}


function write_knowledge()
{
printf "\nBegining to write knowledge"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
                                   -p generations=$GENERATIONS \
                                   -p stat.save-pop=true \
                                   -p seed.0=$SGE_TASK_ID
echo "Finished writing knowledge"

echo "Begining to test the source problem results"
java -cp .:tl.jar gphhucarp.gp.GPTest  -file carp_param_base.param \
                                       -p train-path=$WRITE_KNOW_STAT_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE\
                                       -p generations=$GENERATIONS \
                                       -p eval.problem.eval-model.instances.0.samples=500 \
                                       -p seed.0=$TEST_SEED
echo "Finished performing test on source problem results"
}



function copy_knowledge()
{

mkdir -p $WRITE_KNOW_STAT_DIR
# RESULT_DIR must be exported by gridscript.sh
if [ -z $RESULT_DIR ]
then
    printf "The variable RESULT_DIR is not set"
    exit
fi
cp -r /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/$STAT_ROOT/$DATASET-v$NUM_VEHICLES_SOURCE-writeknow ./$STAT_ROOT/
ls $WRITE_KNOW_STAT_DIR
}



function evaluate_on_test()
{
echo "Evaluating source population on test settings using EvaluateOnTest"
for i in `seq 0  $(($GENERATIONS-1))`
do
    java -cp .:tl.jar tl.gp.EvaluateOnTest carp_param_base.param  $WRITE_KNOW_STAT_DIR/population.gen.$i.bin \
                                        eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                        stat.file="\$$WRITE_KNOW_STAT_DIR/EvaluateOnTest.job.0.out.stat" \
                                        eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE \
                                        eval.problem.eval-model.instances.0.samples=500 \
                                        stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/eval.population.gen \
                                        generations=$GENERATIONS \
                                        seed.0=$SGE_TASK_ID
    printf "Finished evaluation of generation $i on test dataset\n"
done
printf "Finished evaluating source population on test settings using EvaluateOnTest\n\n\n"
}



function do_non_knowledge_experiment()
{
echo "Running experiment on target problem without knowledge"
WITHOUT_KNOW_STAT_DIR="./$STAT_ROOT/$DATASET-v$NUM_VEHICLES_TARGET-wok"
java -cp .:tl.jar ec.Evolve        -file carp_param_base.param \
                                   -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                   -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                   -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                   -p generations=$GENERATIONS \
                                   -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                   -p seed.0=$SGE_TASK_ID

echo "Finished running experiment on target without knowledge"

echo "Running tests on target problem without knowledge"
java -cp .:tl.jar gphhucarp.gp.GPTest -file carp_param_base.param \
                                       -p train-path=$WITHOUT_KNOW_STAT_DIR/ \
                                       -p eval.problem.eval-model.instances.0.file=$DATASET_FILE \
                                       -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_TARGET \
                                       -p eval.problem.eval-model.instances.0.samples=500 \
                                       -p generations=$GENERATIONS \
                                       -p seed.0=$TEST_SEED
printf "Finished running tests on target without knowledge\n\n\n"
}

######################################################################################################################


write_knowledge


evaluate_on_test


do_non_knowledge_experiment



# do_knowledge_experiment subtree25 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=25 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment subtree50 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=50 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment subtree75 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=75 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment subtree100 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=100 \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
#
# do_knowledge_experiment fulltree25 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=25 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#
# do_knowledge_experiment fulltree50 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=50 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#
# do_knowledge_experiment fulltree75 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=75 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#
# do_knowledge_experiment fulltree100 \
#                         -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
#                         -p gp.tc.0.init.transfer-percent=100 \
#                         -p gp.tc.0.init.knowledge-extraction=root
#
# do_knowledge_experiment BestGenKnowledge1 \
#                         -p gp.tc.0.init=tl.gp.BestGenKnowledgeBuilder \
#                         -p gp.tc.0.init.k=1 \
#                         -p gp.tc.0.init.knowledge-folder=$WRITE_KNOW_STAT_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment BestGenKnowledge2 \
#                         -p gp.tc.0.init=tl.gp.BestGenKnowledgeBuilder \
#                         -p gp.tc.0.init.k=2 \
#                         -p gp.tc.0.init.knowledge-folder=$WRITE_KNOW_STAT_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment GTLKnowlege  \
#                         -p gp.tc.0.init=tl.gp.GTLKnowlegeBuilder \
#                         -p gp.tc.0.init.k=2 \
#                         -p gp.tc.0.init.knowledge-folder=$WRITE_KNOW_STAT_DIR/ \
#                         -p gp.tc.0.init.knowledge-extraction=rootsubtree
#
# do_knowledge_experiment TLGPCriptor \
#                         -p gp.tc.0.init=tl.gp.TLGPCriptorBuilder \
#                         -p gp.tc.0.init.knowledge-probability=0.5 \
#                         -p gp.tc.0.init.knowledge-file=$WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin \
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
# java -cp .:tl.jar tl.knowledge.extraction.AnalyzeTerminals carp_param_base.param  $WRITE_KNOW_STAT_DIR/population.gen.$(($GENERATIONS-1)).bin $KNOWLEDGE_FILE \
#                                                         stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
#                                                         eval.problem.eval-model.instances.0.file=$DATASET_FILE \
#                                                         eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_SOURCE  \
#                                                         eval.problem.eval-model.instances.0.samples=500 \
#                                                         stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
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
