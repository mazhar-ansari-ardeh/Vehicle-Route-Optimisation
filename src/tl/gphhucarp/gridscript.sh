
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

if [ -d /local/tmp/mazhar/$JOB_ID.$SGE_TASK_ID ]; then
        cd /local/tmp/mazhar/$JOB_ID.$SGE_TASK_ID
else
        echo "Uh oh ! There's no job directory to change into "
        echo "Something is broken. I should inform the programmers"
        echo "Save some information that may be of use to them"
        echo "There's no job directory to change into "
        echo "Here's LOCAL TMP "
        ls -la /local/tmp
        echo "AND LOCAL TMP MAZHAR "
        ls -la /local/tmp/mazhar
        echo "Exiting"
        exit 1
fi


# Setting variables that config the experiment.
NUM_VEHICLES_SOURCE=6; export NUM_VEHICLES_SOURCE
NUM_VEHICLES_TARGET=7; export NUM_VEHICLES_TARGET
DATASET_CATEGORY="gdb"; export DATASET_CATEGORY
DATASET="gdb21"; export DATASET

# The directory that will contain all the results from grid.
RESULT_DIR="$DATASET-v$NUM_VEHICLES_SOURCE-to$NUM_VEHICLES_TARGET"



cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
cp /vol/grid-solar/sgeusers/mazhar/*.* .

echo "All required files copied. Directory content: "

ls -la

if [ -z "$SGE_ARCH" ]; then
   echo "Can't determine SGE ARCH"
else
   if [ "$SGE_ARCH" = "lx-amd64" ]; then
       JAVA_HOME="/usr/pkg/java/sun-8"
   fi
fi

if [ -z "$JAVA_HOME" ]; then
   echo "Can't define a JAVA_HOME"
else
   export JAVA_HOME
   PATH="/usr/pkg/java/bin:${JAVA_HOME}/bin:${PATH}"; export PATH

   ./gridscript_base.sh
fi

#
ls -la
#
# Now we move the output to a place to pick it up from later
#  noting that we need to distinguish between the TASKS
#  (really should check that directory exists too, but this is just a test)
#
mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR
mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID
cp -r stats  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
cp -r stat  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
cp *.* /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/$SGE_TASK_ID/
#
echo "Ran through OK"
