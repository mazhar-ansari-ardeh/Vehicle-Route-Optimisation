if test -z $1
then
    SCRIPT=without_mt.sh
else
    SCRIPT=$1
fi

printf "%s submitted\n" $SCRIPT
 
# sbatch -J G1v4$SCRIPT rap_script.sh $SCRIPT gdb gdb1 4
# 
# sbatch -J G1v5$SCRIPT rap_script.sh $SCRIPT gdb gdb1 5 
# 
# sbatch -J G1v6$SCRIPT rap_script.sh $SCRIPT gdb gdb1 6
# 
# 
# sbatch -J G2v5$SCRIPT rap_script.sh $SCRIPT gdb gdb2 5
# 
# sbatch -J G2v6$SCRIPT rap_script.sh $SCRIPT gdb gdb2 6
# 
# sbatch -J G2v7$SCRIPT rap_script.sh $SCRIPT gdb gdb2 7
# 
# 
# sbatch -J G3v4$SCRIPT rap_script.sh $SCRIPT gdb gdb3 4
# 
# sbatch -J G3v5$SCRIPT rap_script.sh $SCRIPT gdb gdb3 5
# 
# sbatch -J G3v6$SCRIPT rap_script.sh $SCRIPT gdb gdb3 6
# 
# 
# sbatch -J G4v3$SCRIPT rap_script.sh $SCRIPT gdb gdb4 3
# 
# sbatch -J G4v4$SCRIPT rap_script.sh $SCRIPT gdb gdb4 4
# 
# sbatch -J G4v5$SCRIPT rap_script.sh $SCRIPT gdb gdb4 5
# 
# 
# sbatch -J G5v5$SCRIPT rap_script.sh $SCRIPT gdb gdb5 5
# 
# sbatch -J G5v6$SCRIPT rap_script.sh $SCRIPT gdb gdb5 6
# 
# sbatch -J G5v7$SCRIPT rap_script.sh $SCRIPT gdb gdb5 7
# 
# 
# sbatch -J G6v4$SCRIPT rap_script.sh $SCRIPT gdb gdb6 4 
# 
# sbatch -J G6v5$SCRIPT rap_script.sh $SCRIPT gdb gdb6 5
# 
# sbatch -J G6v6$SCRIPT rap_script.sh $SCRIPT gdb gdb6 6
# 
# 
# sbatch -J G7v4$SCRIPT rap_script.sh $SCRIPT gdb gdb7 4 
# 
# sbatch -J G7v5$SCRIPT rap_script.sh $SCRIPT gdb gdb7 5
# 
# sbatch -J G7v6$SCRIPT rap_script.sh $SCRIPT gdb gdb7 6


sbatch -J G21v6$SCRIPT rap_script.sh $SCRIPT gdb gdb21 6

sbatch -J G21v7$SCRIPT rap_script.sh $SCRIPT gdb gdb21 7

sbatch -J G21v5$SCRIPT rap_script.sh $SCRIPT gdb gdb21 5


sbatch -J G23v10$SCRIPT rap_script.sh $SCRIPT gdb gdb23 10

sbatch -J G23v9$SCRIPT rap_script.sh $SCRIPT gdb gdb23 9

sbatch -J G23v11$SCRIPT rap_script.sh $SCRIPT gdb gdb23 11
