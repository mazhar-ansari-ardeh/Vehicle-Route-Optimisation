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

# Naming convention: 
#   Source-target separator: '.'
#   Algorithm parameter name separator: ':'
#   Algorithm parameter value separator: '_'


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
if test -z $1
then
    echo 'Arg 1 for source dataset category is empty. Sctript terminated.'
    exit
else
    DATASET_CATEGORY_SOURCE=$1; export DATASET_CATEGORY_SOURCE
fi

if test -z $2
then
    echo 'Arg 2 is empty. Sctript terminated.'
    exit
else
    DATASET_SOURCE=$2; export DATASET_SOURCE
fi
if test -z $3
then
    echo 'Arg 3 is empty. Sctript terminated.'
    exit
else
    NUM_VEHICLES_SOURCE=$3; export NUM_VEHICLES_SOURCE
fi

if test -z $4 
then 
    echo 'Arg 4 is empty. Script terminated.'
    exit
else
    DATASET_CATEGORY_TARGET=$4; export DATASET_CATEGORY_TARGET
fi

if test -z $5 
then 
    echo 'Arg 5 is empty. Script terminated.'
    exit
else
    DATASET_TARGET=$5; export DATASET_TARGET
fi

if test -z $6
then
    echo 'Arg 6 is empty. Sctript terminated.'
    exit
else
    NUM_VEHICLES_TARGET=$6; export NUM_VEHICLES_TARGET
fi

if test -z $7
then 
    echo "Base grid script not provided. Using the default script 'egl_base.sh'"
    GRIDSCRIPT_BASE="new_base.sh"
else
    GRIDSCRIPT_BASE=$7
    echo "Running $GRIDSCRIPT_BASE"
fi

exec 2>&1

# The directory that will contain all the results from grid.
# RESULT_DIR="$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE.$DATASET_TARGET.vt$NUM_VEHICLES_TARGET"
RESULT_DIR="$DATASET_SOURCE.vs$NUM_VEHICLES_SOURCE:gen_200"
export RESULT_DIR


cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
cp /vol/grid-solar/sgeusers/mazhar/*.jar .
cp /vol/grid-solar/sgeusers/mazhar/*.sh .
cp /vol/grid-solar/sgeusers/mazhar/*.param .

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

   chmod 777 ./$GRIDSCRIPT_BASE
   ./$GRIDSCRIPT_BASE
fi

#
# ls -la
#
# Now we move the output to a place to pick it up from later
#  noting that we need to distinguish between the TASKS
#

# We do not want the executable program files back
# However, .param files are kept because they can keep record of the experiment parameters. 
rm tl.jar
rm *.sh
rm -r -f data

# mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR
# cp -r .  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/
# cp *.* /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/

# printf "$(date)\t $SGE_TASK_ID \n" >> /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/FinishedTasks.txt

#
echo "Ran through OK"
