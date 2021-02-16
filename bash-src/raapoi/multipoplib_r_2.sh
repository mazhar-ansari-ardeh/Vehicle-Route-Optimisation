GENERATIONS=50

if [ -z "$NUM_VEHICLES_0" ]; then
  NUM_VEHICLES_0=6
fi
echo "Number of source vehicles: $NUM_VEHICLES_0"

if [ -z "$NUM_VEHICLES_1" ]
then
   NUM_VEHICLES_1=7
fi
echo "Number of target vehicles: $NUM_VEHICLES_1"

if [ -z "$DATASET_CATEGORY_0" ]; then
  DATASET_CATEGORY_0="gdb"
fi

if [ -z "$DATASET_CATEGORY_1" ]
then
   DATASET_CATEGORY_1=$DATASET_CATEGORY_0
fi

if [ -z "$DATASET_0" ]; then
  DATASET_0="gdb21"
fi

if [ -z "$DATASET_1" ]
then
   DATASET_1=$DATASET_0
fi


echo "Dataset 0: $DATASET_CATEGORY_0/$DATASET_0"
echo "Dataset 1: $DATASET_CATEGORY_1/$DATASET_1"
# echo "Dataset 2: $DATASET_CATEGORY_2/$DATASET_2"

# Path to dataset file of domain 0
DATASET_FILE_0="$DATASET_CATEGORY_0/$DATASET_0.dat"
echo $DATASET_FILE_0

# Path to dataset file of domain 1
DATASET_FILE_1="$DATASET_CATEGORY_1/$DATASET_1.dat"
echo "Dataset file 1: $DATASET_FILE_1"

# Path to dataset file of domain 1
# DATASET_FILE_2="$DATASET_CATEGORY_2/$DATASET_2.dat"
# echo "Dataset file 2: $DATASET_FILE_2"

# The seed value that will be used for testing models found. This value must be unique
TEST_SEED=25556

if [ -z "$SLURM_ARRAY_TASK_ID" ]; then
  SLURM_ARRAY_TASK_ID=3
  printf "SLURM_ARRAY_TASK_ID was not set. Set it to 3.\n\n"
fi
echo "SLURM_ARRAY_TASK_ID: $SLURM_ARRAY_TASK_ID"
if [ -z "$SLURM_ARRAY_JOB_ID" ]; then
  SLURM_ARRAY_JOB_ID=4
  printf "SLURM_ARRAY_JOB_ID was not set. Set it to 4. \n\n"
fi
echo "SLURM_ARRAY_JOB_ID: $SLURM_ARRAY_JOB_ID"



# 1. Exchange size: number of immigrants to exchange.
# 2. Number of mutations when trying to find a new and unseen individual.
function do_multipop_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="multipop2:exch_$1:mut_$2"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation="$2" \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p hmt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p hmt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}


# 1. Exchange size: number of immigrants to exchange.
# 2. Number of mutations when trying to find a new and unseen individual.
function do_nohistory_multipop_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="nohistory_multipop2:exch_$1:mut_$2"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation="$2" \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p hmt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p hmt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p exch.subpop.0.select-to-die.enable-history-search=false \
    -p exch.subpop.1.select-to-die.enable-history-search=false \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}

function do_currentpophistory_multipop_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="currentpophistory_multipop2:exch_$1:mut_$2"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch.enable-history=false \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation="$2" \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p hmt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p hmt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p exch.subpop.0.select-to-die.enable-history-search=false \
    -p exch.subpop.1.select-to-die.enable-history-search=false \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}

# This experiment is a modified version of the do_currentpophistory_multipop_experiment in which the algorithm 
# does not check to see if the target population contains the duplicates or not. Instead, the individuals are
# selected with tournament selection. The target population, on the other hand, selects duplicates to be replaced 
# with the transferred individuals.
#  
# 1. Exchange size: number of immigrants to exchange.
function do_currentpophistory_multipop_disable_dup_chck_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="currentpophistory_multipop_disable_dup_chck2:exch_$1"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="RaapoiResults/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch=ec.exchange.InterPopulationExchange \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p hmt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p hmt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p exch.subpop.0.select-to-die.enable-history-search=false \
    -p exch.subpop.1.select-to-die.enable-history-search=false \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"

}


# This is the modified version of 'do_currentpophistory_multipop_experiment' in which the algorithm selects
# individuals for transfer based on the duplicate detection mechanism but selects the individuals to die 
# with a tournament selection mechanism. 
function do_currentpophistory_multipop_disable_seltodie_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="currentpophistory_multipop_disable_seltodie2:exch_$1:mut_$2"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="RaapoiResults/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch.enable-history=false \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation="$2" \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p hmt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p hmt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die=ec.select.TournamentSelection \
    -p exch.subpop.0.select-to-die.pick-worst=true \
    -p exch.subpop.1.select-to-die=ec.select.TournamentSelection \
    -p exch.subpop.1.select-to-die.pick-worst=true \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
  
}

# 1. Exchange size: number of immigrants to exchange.
# 2. Number of mutations when trying to find a new and unseen individual.
function simple_multipop_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="simple_multipop2:exch_$1"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch=ec.exchange.InterPopulationExchange \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p state=tl.knowledge.multipop.MultiPopEvolutionState \
    -p multipop-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die=ec.select.TournamentSelection \
    -p exch.subpop.0.select-to-die.pick-worst=true \
    -p exch.subpop.1.select-to-die=ec.select.TournamentSelection \
    -p exch.subpop.1.select-to-die.pick-worst=true \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}


function mutating_multipop_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="mutating_multipop2:exch_$1:mut_$2"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch=tl.knowledge.multipop.mutating.MutatingBasicExchanger \
    -p state=tl.knowledge.multipop.mutating.PreEvalExchMultiPopEvolutionState \
    -p preval-mt-state.num-evaluations=102400 \
    -p eval=tl.knowledge.multipop.mutating.RangeBasedSimpleEvaluator \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation="$2" \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p preval-mt-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p preval-mt-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}

function EMTET_experiment_2() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="EMTET2:e:exch_$1:dist_$2:mutprob_$3"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0.mp.$DATASET_1.v$NUM_VEHICLES_1:gen_$GENERATIONS/$L_EXPERIMENT_NAME/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/
  mkdir -p $L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_2.param \
    -p generations=$GENERATIONS \
    -p pop.subpops=2 \
    -p exch=tl.knowledge.multipop.mutating.ProbilisticMutatingBasicExchanger \
    -p state=tl.knowledge.multipop.mutating.PositiveTransferAwarePreEvalExchMultiPopEvolutionState \
    -p positran-pre-eval-state.distance-threshold=$2 \
    -p positran-pre-eval-state.num-evaluations=102400 \
    -p positran-pre-eval-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p positran-pre-eval-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p exch.mutation-prob=$3 \
    -p eval=tl.knowledge.multipop.mutating.RangeBasedSimpleEvaluator \
    -p exch.subpop.0.size="$1" \
    -p exch.subpop.1.size="$1" \
    -p exch.num-mutation=1 \
    -p exch.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger" \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/job.0.out.stat" \
    -p stat.subpop.1.file="\$$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/job.0.out.stat" \
    -p exch.subpop.0.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.0" \
    -p exch.subpop.1.select-to-die.knowledge-log-file="$L_EXPERIMENT_DIR/exchanger/std.1" \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.subpop.1.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.subpop.1.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_0.v$NUM_VEHICLES_0/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_0.v$NUM_VEHICLES_0"

  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_2.param \
    -p train-path="$L_EXPERIMENT_DIR/$DATASET_1.v$NUM_VEHICLES_1/" \
    -p eval.problem=tl.gphhucarp.ReactiveGPHHProblem \
    -p eval.problem.eval-model=gphhucarp.gp.evaluation.ReactiveEvaluationModel \
    -p eval.problem.eval-model.instances=1 \
    -p eval.problem.eval-model.instances.0.file=$DATASET_FILE_1 \
    -p eval.problem.eval-model.instances.0.vehicles=$NUM_VEHICLES_1 \
    -p eval.problem.eval-model.instances.0.samples=500 \
    -p eval.problem.eval-model.instances.0.demand-uncertainty-level=0.2 \
    -p eval.problem.eval-model.instances.0.cost-uncertainty-level=0.2 \
    -p eval.problem.eval-model.objectives=1 \
    -p eval.problem.eval-model.objectives.0=total-cost \
    -p eval.problem.pool-filter=gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter \
    -p eval.problem.tie-breaker=gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker \
    -p generations=$GENERATIONS \
    -p seed.0=$TEST_SEED
  printf "Finished running tests on target with knowledge, experiment: %s \n\n\n" "$L_EXPERIMENT_NAME/$DATASET_1.v$NUM_VEHICLES_1"
}
