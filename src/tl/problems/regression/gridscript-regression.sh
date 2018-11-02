
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
# End of the setup directives
#
# Now let's do something useful, but first change into the job-specific
# directory that should have been created for us
#
# Check we have somewhere to work now and if we don't, exit nicely.
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
#
# Now we are in the job-specific directory so now can do something useful
#
# Stdout from programs and shell echos will go into the file
#    scriptname.o$JOB_ID.$SGE_TASK_ID
#
#
# Copy the input file to the local directory
#
# cp /vol/grid-solar/sgeusers/mazhar/*.jar .

# Setting variables that config the experiment.
PROBLEM_SOURCE="Poly0"
PROBLEM_TARGET="Poly2"
TOURNAMENT_SIZE=20
FILTER_SIZE=100
KNOWLEDGE_PROBABILITY=0.5
STAT_ROOT="stats"
TARGET_PROBLEM_CLASS="tl.problems.regression.one_d.Poly2"
SOURCE_PROBLEM_CLASS="tl.problems.regression.one_d.Poly0"

# Helper variables that are created based on setting variables.
# Do not modify these variables.

# Write knowledge
WRITE_KNOW_STAT_DIR="./$STAT_ROOT/$PROBLEM_SOURCE-writeknow"

# Extract knowledge
KNOWLEDGE_FILE="$PROBLEM_SOURCE.cf"

# Without knowledge
WITHOUT_KNOW_STAT_DIR="./$STAT_ROOT/$PROBLEM_TARGET-wok"

# With knowledge
WITH_KNOW_STAT_DIR="./$STAT_ROOT/$PROBLEM_TARGET-wk-$KNOWLEDGE_PROBABILITY-$FILTER_SIZE-$TOURNAMENT_SIZE"

# The directory that will contain all the results from grid.
RESULT_DIR="$PROBLEM_SOURCE-to$PROBLEM_TARGET"



# cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
cp /vol/grid-solar/sgeusers/mazhar/*.param .
cp /vol/grid-solar/sgeusers/mazhar/tl.jar .
# cp /vol/grid-solar/sgeusers/mazhar/*.bin .
# mkdir -p stat

echo "All required files copied. Directory content: "

ls -la
#
# Note that we need the full path to this utility, as it is not on the PATH
#
#/usr/pkg/bin/convert krb_tkt_flow.JPG krb_tkt_flow.png

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

   echo "Begining to write knowledge"
   java -cp .:tl.jar ec.Evolve          -file train-base.param \
                                        -p stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
                                        -p stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
                                        -p eval.problem=$SOURCE_PROBLEM_CLASS \
                                        -p seed.0=$SGE_TASK_ID

   echo "Finished writing knowledge"

   echo "Begining to test the previous results"
   java -cp .:tl.jar tl.problems.regression.RegTest -file test-base.param \
                                         -p train-path=$WRITE_KNOW_STAT_DIR/ \
                                         -p eval.problem=$SOURCE_PROBLEM_CLASS
   echo "Finished performing test on results of the previous operation"

   echo "Begining to extract knowledge"
   # Except seed, I think most of the other params are not required
   java -cp .:tl.jar tl.gp.CFExtractor test-base.param  $WRITE_KNOW_STAT_DIR/population.gen.99.bin all $KNOWLEDGE_FILE \
                                            stat.file="\$$WRITE_KNOW_STAT_DIR/job.0.out.stat" \
                                            stat.gen-pop-file=$WRITE_KNOW_STAT_DIR/population.gen \
                                            seed.0=$SGE_TASK_ID
   echo "Finished extracting knowledge"

   echo "Running experiment on target problem without knowledge"
   java -cp .:tl.jar ec.Evolve          -file train-base.param \
                                        -p stat.file="\$$WITHOUT_KNOW_STAT_DIR/job.0.out.stat" \
                                        -p eval.problem=$TARGET_PROBLEM_CLASS \
                                        -p stat.gen-pop-file="$WITHOUT_KNOW_STAT_DIR/population.gen" \
                                        -p seed.0=$SGE_TASK_ID

   echo "Finished running experiment on target without knowledge"

   echo "Running tests on target problem without knowledge"
   java -cp .:tl.jar tl.problems.regression.RegTest -file test-base.param \
                                         -p train-path=$WITHOUT_KNOW_STAT_DIR/ \
                                         -p eval.problem=$TARGET_PROBLEM_CLASS \
   echo "Finished running tests on target without knowledge"

   echo "Running experiment on target problem with knowledge"
   # Warning: In the following command, the parameter 'knowledge-extraction' specifies the extraction method. If this value is 'exactcodefragment',
   # the algorithm will assume that that the file contains code fragments and will only load the fragments.
   java -cp .:tl.jar ec.Evolve          -file train-base.param \
                                        -p stat.file="\$$WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                        -p stat.gen-pop-file="$WITH_KNOW_STAT_DIR/population.gen" \
										-p eval.problem=$TARGET_PROBLEM_CLASS \
                                        -p pop.subpop.0.species.pipe.source.0=tl.gp.FilteredKnowledgeCrossoverPipeline \
                                        -p gp.tc.0.init=tl.gp.FilteredFittedCodeFragmentBuilder \
                                        -p gp.tc.0.init.knowledge-log-file="$WITH_KNOW_STAT_DIR/filterknowinit" \
                                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
                                        -p gp.tc.0.init.knowledge-probability=$KNOWLEDGE_PROBABILITY \
                                        -p gp.tc.0.init.knowledge-tournament-size=$TOURNAMENT_SIZE \
                                        -p gp.tc.0.init.knowledge-extraction=exactcodefragment \
                                        -p gp.tc.0.init.knowledge-filter-size=$FILTER_SIZE \
                                        -p pop.subpop.0.species.pipe.source.0.knowledge-log-file="$WITH_KNOW_STAT_DIR/filterknowxover" \
                                        -p seed.0=$SGE_TASK_ID

   echo "Finished running experiment on target with knowledge"

   echo "Running tests on target problem with knowledge"
   java -cp .:tl.jar tl.problems.regression.RegTest -file test-base.param \
                                         -p train-path=$WITH_KNOW_STAT_DIR/ \
                                         -p eval.problem=$TARGET_PROBLEM_CLASS \
   echo "Finished running tests on target with knowledge"


   echo "Running experiment on target problem with fulltree knowledge"
   # Warning: In the following command, the parameter 'knowledge-extraction' specifies the extraction method. If this value is 'exactcodefragment',
   # the algorithm will assume that that the file contains code fragments and will only load the fragments.
   java -cp .:tl.jar ec.Evolve          -file train-base.param \
                                        -p stat.file="\$$WITH_KNOW_STAT_DIR/job.0.out.stat" \
                                        -p stat.gen-pop-file="$WITH_KNOW_STAT_DIR/population.gen" \
										-p eval.problem=$TARGET_PROBLEM_CLASS \
                                        -p gp.tc.0.init=tl.gp.SimpleCodeFragmentBuilder \
                                        -p gp.tc.0.init.knowledge-log-file="$WITH_KNOW_STAT_DIR/filterknowinit" \
                                        -p gp.tc.0.init.knowledge-file=$KNOWLEDGE_FILE \
                                        -p gp.tc.0.init.knowledge-probability=$KNOWLEDGE_PROBABILITY \
                                        -p gp.tc.0.init.knowledge-tournament-size=$TOURNAMENT_SIZE \
                                        -p gp.tc.0.init.knowledge-extraction=exactcodefragment \
                                        -p gp.tc.0.init.knowledge-filter-size=$FILTER_SIZE \
                                        -p pop.subpop.0.species.pipe.source.0.knowledge-log-file="$WITH_KNOW_STAT_DIR/filterknowxover" \
                                        -p seed.0=$SGE_TASK_ID

   echo "Finished running experiment on target with fulltree knowledge"

   echo "Running tests on target problem with knowledge"
   java -cp .:tl.jar tl.problems.regression.RegTest -file test-base.param \
                                         -p train-path=$WITH_KNOW_STAT_DIR/ \
                                         -p eval.problem=$TARGET_PROBLEM_CLASS \
   echo "Finished running tests on target with knowledge"
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
