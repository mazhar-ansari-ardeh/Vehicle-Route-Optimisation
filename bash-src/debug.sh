DATASET_CATEGORY_SOURCE='gdb'; export DATASET_CATEGORY_SOURCE


DATASET_SOURCE='gdb1'; export DATASET_SOURCE

NUM_VEHICLES_SOURCE=5; export NUM_VEHICLES_SOURCE

DATASET_CATEGORY_TARGET='gdb'; export DATASET_CATEGORY_TARGET

DATASET_TARGET='gdb1'; export DATASET_TARGET

NUM_VEHICLES_TARGET=6; export NUM_VEHICLES_TARGET

SGE_TASK_ID=2; export SGE_TASK_ID

if test -z "$1"
then
    SCRIPT_TO_TEST=""
    exit
else
    SCRIPT_TO_TEST="$1"
fi
echo "Running $SCRIPT_TO_TEST"


exec 2>&1

if [ -d $HOME/$SCRIPT_TO_TEST ]; then
    rm -r "$HOME/$SCRIPT_TO_TEST"
fi

mkdir "$HOME/$SCRIPT_TO_TEST"
cd "$HOME/$SCRIPT_TO_TEST" || exit


cp -r /vol/grid-solar/sgeusers/mazhar/data/ .
cp -v /vol/grid-solar/sgeusers/mazhar/*.jar .
cp -v /vol/grid-solar/sgeusers/mazhar/*.sh .
cp -v /vol/grid-solar/sgeusers/mazhar/*.param .

echo "All required files copied. Directory content: "

pwd
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

   chmod 777 ./$SCRIPT_TO_TEST
   ./$SCRIPT_TO_TEST > $SCRIPT_TO_TEST.out
fi