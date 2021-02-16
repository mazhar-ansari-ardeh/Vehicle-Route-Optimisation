GENERATIONS=50

if [ -z "$NUM_VEHICLES_0" ]; then
  NUM_VEHICLES_0=6
fi
echo "Number of source vehicles: $NUM_VEHICLES_0"

if [ -z "$DATASET_CATEGORY_0" ]; then
  DATASET_CATEGORY_0="gdb"
fi

if [ -z "$DATASET_0" ]; then
  DATASET_0="gdb21"
fi

echo "Dataset 0: $DATASET_CATEGORY_0/$DATASET_0"

# Path to dataset file of domain 0
DATASET_FILE_0="$DATASET_CATEGORY_0/$DATASET_0.dat"
echo $DATASET_FILE_0

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


# This experiment does not have any input arguments.
function do_without_mt_experiment() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="without_mt"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="Results/$DATASET_0.v$NUM_VEHICLES_0/$L_EXPERIMENT_NAME:gen_$GENERATIONS/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_1.param \
    -p generations=$GENERATIONS \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/job.0.out.stat" \
    -p multipop-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p multipop-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_1.param \
    -p train-path="$L_EXPERIMENT_DIR/" \
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
}

# This function implements the idea of running GPHH without any knowledge transfer. However, this experiment also implements a
# clearing method to remove some of the duplicates in the population. This is intended to be a control experiment to see
# if the performance of DDMT is coming from the diversity improvements or the transferred knowledge. The algorithm has only
# two arguments:
# 1. Max number of duplicate individuals to remove. 
# 2. Reverse sort before clearing.
function do_clearing_without_mt_experiment() {
  # The L prefix indicates that the varaible is local
  L_EXPERIMENT_NAME="clearing_without_mt:numclr_$1"
  echo "Experiment arguments:" "${@}"
  pwd
  printf "Running experiment on target problem with knowledge, experiment: %s\n\n" "$L_EXPERIMENT_NAME"
  L_EXPERIMENT_DIR="/nfs/scratch/ansarima/GECCO2021/Results/$DATASET_0.v$NUM_VEHICLES_0/$L_EXPERIMENT_NAME:gen_$GENERATIONS/$SLURM_ARRAY_TASK_ID"
  mkdir -p $L_EXPERIMENT_DIR/
  
  echo "Experiment directory: $L_EXPERIMENT_DIR"
  
  java -cp .:tl.jar ec.Evolve -file multipop_base_1.param \
    -p generations=$GENERATIONS \
    -p stat.subpop.0.file="\$$L_EXPERIMENT_DIR/job.0.out.stat" \
    -p state=tl.knowledge.multipop.ClearingMultiPopEvolutionState \
    -p clear-multipop-state.knowledge-log-file="$L_EXPERIMENT_DIR/State/State" \
    -p clear-multipop-state.pop-log-path="$L_EXPERIMENT_DIR/State/" \
    -p clear-multipop-state.num-clear=$1 \
    -p clear-multipop-state.dms-size=20 \
    -p eval.problem.subpop.0.eval-model.instances.0.file=$DATASET_FILE_0 \
    -p eval.problem.subpop.0.eval-model.instances.0.vehicles=$NUM_VEHICLES_0 \
    "${@:2}" \
    -p seed.0=$SLURM_ARRAY_TASK_ID
  echo "Finished running experiment: $L_EXPERIMENT_NAME"

  echo "Dataset file: $DATASET_FILE_0"
  echo "Running tests on target problem with knowledge, experiment: $L_EXPERIMENT_NAME"
  java -cp .:tl.jar gphhucarp.gp.GPTest -file multipop_base_1.param \
    -p train-path="$L_EXPERIMENT_DIR/" \
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
}

