
if test -z $1
then
    SCRIPT=multipop2.sh
else
    SCRIPT=$1
fi
 
# sbatch -J G1v5G1v6$SCRIPT rap_script.sh $SCRIPT gdb gdb1 5 gdb gdb1 6
# sbatch -J G1v5G1v4$SCRIPT rap_script.sh $SCRIPT gdb gdb1 5 gdb gdb1 4
# 
# sbatch -J G2v6G2v7$SCRIPT rap_script.sh $SCRIPT gdb gdb2 6 gdb gdb2 7
# sbatch -J G2v6G2v5$SCRIPT rap_script.sh $SCRIPT gdb gdb2 6 gdb gdb2 5
# 
# sbatch -J G3v5G3v6$SCRIPT rap_script.sh $SCRIPT gdb gdb3 5 gdb gdb3 6
# sbatch -J G3v5G3v4$SCRIPT rap_script.sh $SCRIPT gdb gdb3 5 gdb gdb3 4
# 
# sbatch -J G4v4G4v5$SCRIPT rap_script.sh $SCRIPT gdb gdb4 4 gdb gdb4 5
# sbatch -J G4v4G4v3$SCRIPT rap_script.sh $SCRIPT gdb gdb4 4 gdb gdb4 3
# 
# sbatch -J G5v6G5v7$SCRIPT rap_script.sh $SCRIPT gdb gdb5 6 gdb gdb5 7
# sbatch -J G5v6G5v5$SCRIPT rap_script.sh $SCRIPT gdb gdb5 6 gdb gdb5 5
# 
# sbatch -J G6v5G6v6$SCRIPT rap_script.sh $SCRIPT gdb gdb6 5 gdb gdb6 6
# sbatch -J G6v5G6v4$SCRIPT rap_script.sh $SCRIPT gdb gdb6 5 gdb gdb6 4
# 
# sbatch -J G7v5G7v6$SCRIPT rap_script.sh $SCRIPT gdb gdb7 5 gdb gdb7 6
# sbatch -J G7v5G7v4$SCRIPT rap_script.sh $SCRIPT gdb gdb7 5 gdb gdb7 4

sbatch -J G1v5G2v5$SCRIPT rap_script.sh $SCRIPT gdb gdb1 5 gdb gdb2 5

sbatch -J G1v5G2v7$SCRIPT rap_script.sh $SCRIPT gdb gdb1 5 gdb gdb2 7

sbatch -J G2v6G6v6$SCRIPT rap_script.sh $SCRIPT gdb gdb2 6 gdb gdb6 6

sbatch -J G6v5G7v6$SCRIPT rap_script.sh $SCRIPT gdb gdb6 5 gdb gdb7 6

sbatch -J G7v5G1v6$SCRIPT rap_script.sh $SCRIPT gdb gdb7 5 gdb gdb1 6
