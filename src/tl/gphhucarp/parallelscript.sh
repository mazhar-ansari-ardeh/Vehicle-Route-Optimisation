
#!/bin/sh
#
# Force Bourne Shell if not Sun Grid Engine default shell (you never know!)
#
#$ -S /bin/sh
#
# I know I have a directory here so I'll use it as my initial working directory
#
#$ -wd /vol/grid-solar/sgeusers/mazhar
#

# Setting variables that config the experiment.
NUM_VEHICLES_SOURCE=6; export NUM_VEHICLES_SOURCE
NUM_VEHICLES_TARGET=7; export NUM_VEHICLES_TARGET
DATASET_CATEGORY="gdb"; export DATASET_CATEGORY
DATASET="gdb21"; export DATASET

# The directory that will contain all the results from grid.
RESULT_DIR="$DATASET-v$NUM_VEHICLES_SOURCE-to$NUM_VEHICLES_TARGET"



# cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
# cp /vol/grid-solar/sgeusers/mazhar/*.* .
# echo "All required files copied. Directory content: "
# ls -la


SGE_TASK_ID=$1
export SGE_TASK_ID

JOB_ID=$2
export JOB_ID

chmod 777 ./gridscript_base.sh
./gridscript_base.sh


#
# ls -la
#
# Now we move the output to a place to pick it up from later
#  noting that we need to distinguish between the TASKS
#  (really should check that directory exists too, but this is just a test)
#
# mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR
# mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID
# cp -r stats  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
# cp -r stat  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
# cp *.* /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
#
# echo "Ran through OK"
