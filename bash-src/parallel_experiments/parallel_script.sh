#!/bin/sh
#
# This is the equivalent of the 'grid_script.sh' file that I developed for performing grid tasks. 
# This script is meant to be used with the GNU Parallel application to run grid tasks locally. 

# Naming convention: 
#   Source-target separator: '.'
#   Algorithm parameter name separator: ':'
#   Algorithm parameter value separator: '_'
SGE_TASK_ID=$1
export SGE_TASK_ID

temp_dir=$(mktemp -d) 
cd $temp_dir


cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
cp /vol/grid-solar/sgeusers/mazhar/*.jar .
cp /vol/grid-solar/sgeusers/mazhar/*.sh .
cp /vol/grid-solar/sgeusers/mazhar/*.param .

echo "All required files copied. Directory content: "

ls -la


chmod 777 ./$GRIDSCRIPT_BASE
./$GRIDSCRIPT_BASE

# We do not want the executable program files back
# However, .param files are kept because they can keep record of the experiment parameters. 
rm tl.jar
rm *.sh
rm -r -f data

# mkdir -p /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR
# cp -r .  /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/
# cp *.* /vol/grid-solar/sgeusers/mazhar/$RESULT_DIR/

echo "Ran through OK"
cd ..
rm -R $temp_dir
