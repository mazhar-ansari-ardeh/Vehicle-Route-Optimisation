import os
import subprocess
import re
import statistics
from pathlib import Path
from scipy import stats
import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats
import shutil
import json

from grid.core import *
from grid.plotting import plot_grid_output
from grid.plotting import plot_surr
from grid.management import rename_alg_folder
from grid.management import delete_alg
from grid.core import find_failed
from grid.core import find_all_failed
from grid.management import delete_exp
from grid.stats import *


experiments = [
                            # 'egl-e1-A.vs5:gen_200',
                            # 'egl-e1-B.vs7:gen_200',
                            'gdb1.vs5.gdb1.vt4:gen_50',
                            'gdb1.vs5.gdb1.vt6:gen_50',
                            'gdb2.vs6.gdb2.vt5:gen_50',
                            'gdb2.vs6.gdb2.vt7:gen_50',
                            'gdb3.vs5.gdb3.vt4:gen_50',
                            'gdb3.vs5.gdb3.vt6:gen_50',
                            'gdb4.vs4.gdb4.vt3:gen_50',
                            'gdb4.vs4.gdb4.vt5:gen_50',
                            'gdb5.vs6.gdb5.vt5:gen_50',
                            'gdb5.vs6.gdb5.vt7:gen_50',
                            'gdb6.vs5.gdb6.vt4:gen_50',
                            'gdb6.vs5.gdb6.vt6:gen_50',
                            'gdb7.vs5.gdb7.vt4:gen_50',
                            'gdb7.vs5.gdb7.vt6:gen_50',
                            # 'gdb8.vs10.gdb8.vt9:gen_50',
                            # 'gdb8.vs10.gdb8.vt11:gen_50',
                            # 'gdb12.vs7.gdb12.vt6:gen_50',
                            # 'gdb12.vs7.gdb12.vt8:gen_50',
                            # 'gdb13.vs6.gdb13.vt5:gen_50',
                            # 'gdb13.vs6.gdb13.vt7:gen_50',
                            # 'gdb14.vs5.gdb14.vt4:gen_50',
                            # 'gdb14.vs5.gdb14.vt6:gen_50',
                            # 'gdb15.vs4.gdb15.vt3:gen_50',
                            # 'gdb15.vs4.gdb15.vt5:gen_50', 
                            # 'gdb16.vs5.gdb16.vt4:gen_50',
                            # 'gdb16.vs5.gdb16.vt6:gen_50',
                            # 'gdb17.vs5.gdb17.vt4:gen_50',
                            # 'gdb17.vs5.gdb17.vt6:gen_50',
                            # 'gdb19.vs3.gdb19.vt2:gen_50',
                            # 'gdb19.vs3.gdb19.vt4:gen_50',
                            # 'gdb20.vs4.gdb20.vt3:gen_50',
                            # 'gdb20.vs4.gdb20.vt5:gen_50',

                            # 'gdb18.vs5.gdb18.vt4:gen_50',
                            # 'gdb18.vs5.gdb18.vt6:gen_50',

                            # 'gdb21.vs6.gdb21.vt5:gen_50',
                            # 'gdb21.vs6.gdb21.vt7:gen_50',

                            # 'gdb9.vs10.gdb9.vt9:gen_50',
                            # 'gdb9.vs10.gdb9.vt11:gen_50',

                            # 'gdb23.vs10.gdb23.vt11:gen_50',
                            # 'gdb23.vs10.gdb23.vt9:gen_50',
                            # 'val9C.vs5.val9C.vt4:gen_50',
                            # 'val9C.vs5.val9C.vt6:gen_50',
                            # 'val9D.vs10.val9D.vt9:gen_50',
                            # 'val9D.vs10.val9D.vt11:gen_50',
                            # 'val10C.vs5.val10C.vt4:gen_50',
                            # 'val10C.vs5.val10C.vt6:gen_50',
                            # 'val10D.vs10.val10D.vt11:gen_50',
                            # 'val10D.vs10.val10D.vt9:gen_50',

                            # 'gdb1.vs5:gen_50',
                            # 'gdb2.vs6:gen_50',
                            # 'gdb4.vs4:gen_50',
                            # 'gdb5.vs6:gen_50',
                            # 'gdb6.vs5:gen_50',
                            # 'gdb6.vs5:gen_50',
                            # 'gdb12.vs7:gen_50',
                            # 'gdb13.vs6:gen_50',
                            # 'gdb14.vs5:gen_50',
                            # 'gdb15.vs4:gen_50',
                            # 'gdb16.vs5:gen_50',
                            # 'gdb17.vs5:gen_50',
                            # 'gdb18.vs5:gen_50',
                            # 'gdb19.vs3:gen_50',
                            # 'gdb20.vs4:gen_50',
                            # 'gdb22.vs8:gen_50',
                            # 'gdb3.vs6:gen_50',
                            # 'gdb8.vs10:gen_50',
                            # 'gdb9.vs10:gen_50',
                            # 'gdb21.vs6:gen_50',
                            # 'gdb23.vs10:gen_50',
                            # 'val9A.vs3:gen_50',
                            # 'val9C.vs5:gen_50',
                            # 'val9D.vs10:gen_50',
                            # 'val10C.vs5:gen_50',
                            # 'val10D.vs10:gen_50',

                            # 'gdb1.vs4:gen_200',
                            # 'gdb1.vs5:gen_200',
                            # 'gdb1.vs6:gen_200',
                            # 'gdb2.vs5:gen_200',
                            # 'gdb2.vs6:gen_200',
                            # 'gdb2.vs7:gen_200',
                            # 'gdb8.vs9:gen_200',
                            # 'gdb8.vs10:gen_200',
                            # 'gdb8.vs11:gen_200',
                            # 'gdb9.vs9:gen_200',
                            # 'gdb9.vs10:gen_200',
                            # 'gdb9.vs11:gen_200',
                            # 'gdb21.vs5:gen_200',
                            # 'gdb21.vs6:gen_200',
                            # 'gdb21.vs7:gen_200',
                            # 'gdb23.vs9:gen_200',
                            # 'gdb23.vs10:gen_200',
                            # 'gdb23.vs11:gen_200',
                            # 'val9C.vs4:gen_200',
                            # 'val9C.vs5:gen_200',
                            # 'val9C.vs6:gen_200',
                            # 'val9D.vs9:gen_200',
                            # 'val9D.vs10:gen_200',
                            # 'val9D.vs11:gen_200',
                            # 'val10C.vs4:gen_200',
                            # 'val10C.vs5:gen_200',
                            # 'val10C.vs6:gen_200',
                            # 'val10D.vs9:gen_200',
                            # 'val10D.vs10:gen_200',
                            # 'val10D.vs11:gen_200',

                            # 'gdb1.vs6:gen_200',
                            # 'gdb2.vs7:gen_200',
                            # 'gdb8.vs11:gen_200',
                            # 'gdb9.vs11:gen_200',
                            # 'gdb21.vs7:gen_200',
                            # 'gdb23.vs11:gen_200',
                            # 'val9C.vs6:gen_200',
                            # 'val9D.vs11:gen_200',
                            # 'val10C.vs6:gen_200',
                            # 'val10D.vs11:gen_200',

                            # 'gdb8.vs10.gdb8.vt12:gen_50',
                            ]

dirbase = Path('/home/mazhar/grid/')
# dirbase = Path('/local/scratch')

# Number of GP generations
GENERATIONS = 50

output_folder = Path('/home/mazhar/Desktop/plots-SurEval')

inclusion_filter = [
    # r'WithoutKnowledge:clear_false',
    r'WithoutKnowledge$',
    # r'Surrogate:initsurpool_true:tp_0:surupol_FIFONoDupPhenotypic',
    # r'Surrogate:initsurpool_true:tp_0.1:surupol_FIFONoDupPhenotypic',
    # r'Surrogate:.*:dms_30.*AddOnce',
    # r'Surrogate:.*:dms_30.*Reset',
    # r'Surrogate:initsurpool_true:tp_0:knndistmetr_hamming:avefitdup_true:dms_30:surupol_AddOncePhenotypic',
    # r'Surrogate:.*Entropy',
    # r'Surrogate:.*Unbounded.*',
    # r'Surrogate:initsurpool_false:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset',
    # r'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset',
    # r'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic',
    # r'Surrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic',
    # r'EnsembleSurrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:dms_30',
    # r'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:dms_30',
    # r'EnsembleSurrogate:initsurpool_true:tp_0.5:knndistmetr_corrphenotypic:dms_30',
    # r'Ensemble.*',
    # r'Surrogate:.*:dms_30.*Reset',
    # r'Surrogate:initsurpool_true:tp_0.1:surupol_AddOnce',
    # r'Surrogate:initsurpool_false:tp_0:surupol_Reset',
    # r'Surrogate:initsurpool_true:tp_0:surupol_Entropy',
    # r'Surrogate:initsurpool_true:tp_0.1:surupol_Entropy',
    # r'Surrogate:initsurpool_false:tp_0:surupol_Entropy',
    # r'Cleared*',
    r'SurEval',
    r'FullTree:tp_10:dup_true:clear_false',
    r'FullTree:tp_50:dup_true:clear_false',
    r'FullTree:tp_50:dup_true:clear_true',
    r'Subtree:perc_10:clear_false',
    r'Subtree:perc_50:clear_false',]

# Ignore, exclude and do not show the following items
exclusion_filter = [
    # r'_hamming',
    # r'tp_0:',
    r':tp_0.1',
    r':tp_0.5',
    # r'EnsembleSurrogate:initsurpool_true:tp_0.5:knndistmetr_corrphenotypic:dms_30'
]

rename_map = {
    # 'Surrogate:initsurpool_true:tp_0:surupol_FIFONoDupPhenotypic' : 'Surrogate(true, 0, FIFONoDupPhenotypic)',
    # 'Surrogate:initsurpool_true:tp_0.1:surupol_FIFONoDupPhenotypic' : 'Surrogate(true, 0.1, FIFONoDupPhenotypic)',
    # 'Surrogate:initsurpool_true:tp_0:surupol_AddOncePhenotypic': 'Surrogate(true, 0, AddOnce)',
    # 'Surrogate:initsurpool_true:tp_0.1:surupol_AddOncePhenotypic': 'Surrogate(true, 0.1, AddOnce)',
    # 'Surrogate:initsurpool_true:tp_0:surupol_Entropy':'Surrogate(true, 0, Entropy)',
    # 'Surrogate:initsurpool_true:tp_0.1:surupol_Entropy':'Surrogate(true, 0.1, Entropy)',
    # 'Surrogate:initsurpool_false:tp_0:surupol_Entropy':'Surrogate(false, 0, Entropy)',    
    # 'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:dms_30':'KESGPHH-0.1',
    # 'EnsembleSurrogate:initsurpool_true:tp_0.5:knndistmetr_corrphenotypic:dms_30':'KESGPHH-0.5',
    # 'EnsembleSurrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:dms_30':'KESGPHH-0.0',
    # 'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_hamming:dms_30':'KESGPHHham-0.1',
    # 'EnsembleSurrogate:initsurpool_true:tp_0:knndistmetr_hamming:dms_30':'KESGPHHham-0.0',
    'ClearedFullTree:tp_0.5:metric_hamming:gen_26_49:nrad_0:dms_20':'ClearedFullTree(0.5, 26, 49, 0)',
    'ClearedFullTree:tp_0.5:metric_hamming:gen_26_49:nrad_5:dms_20':'ClearedFullTree(0.5, 26, 29, 5)',
    'ClearedFullTree:tp_0.5:metric_hamming:gen_49_49:nrad_0:dms_20':'ClearedFullTree(0.5, 49, 49, 0)',
    'ClearedFullTree:tp_1:metric_hamming:gen_1_49:nrad_7:dms_20':'ClearedFullTree(1.0, 1, 49, 7)',
    'ClearedFullTree:tp_0.1:metric_hamming:gen_49_49:nrad_0:dms_20':'ClearedFullTree(0.1, 49, 49, 0)',
    'SurEvalFullTree:tp_0.1:metric_hamming:gen_49_49:nrad_0:dms_20':'SurEvalFullTree(0.1, 49, 49, 0)',
    'SurEvalFullTree:tp_0.5:metric_hamming:gen_49_49:nrad_0:dms_20':'SurEvalFullTree(0.5, 49, 49, 0)',
    'SurEvalFullTree:tp_0.5:metric_hamming:gen_26_49:nrad_0:dms_20':'SurEvalFullTree(0.5, 26, 49, 0)',
    'SurEvalFullTree:tp_0.5:metric_hamming:gen_26_49:nrad_5:dms_20':'SurEvalFullTree(0.5, 26, 49, 5)',
    'SurEvalFullTree:tp_1:metric_hamming:gen_1_49:nrad_7:dms_20':'SurEvalFullTree(1.0, 1, 49, 7)',
    'FullTree:tp_10:dup_true:clear_true': 'FullTree(10, dup, clear)',
    'FullTree:tp_10:dup_true:clear_false': 'FullTree-10',
    'FullTree:tp_50:dup_true:clear_false': 'FullTree-50',

    'Subtree:perc_10:clear_false': 'SubTree-10',
    'Subtree:perc_50:clear_false': 'SubTree-50',

    # 'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset':'KESGPHH-0-Reset',
    # 'Surrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset':'KESGPHH-0.1-Reset',
    # 'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic':'KESGPHH-0-Fixed',
    # 'Surrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic':'KESGPHH-0.1-Fixed',
    # 'Surrogate:initsurpool_false:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset':'ESGPHH',
    # 'Surrogate:initsurpool_true:tp_0:knndistmetr_phenotypic:avefitdup_true:surupol_Reset':'ESGPHH-Reset-phen20',
    # 'Surrogate:initsurpool_true:tp_0.1:knndistmetr_hamming:avefitdup_true:dms_30:surupol_Reset':'ESGPHH-Reset-ham30',
    # 'FrequentSub':
    # 'SubTree_10':'SubTree-10',
    # 'SubTree_50':'SubTree-50',
    # 'Surrogate:initsurpool_true:tp_0:surupol_Reset' : 'Surrogate(true, 0, Reset)', 
    # 'Surrogate:initsurpool_true:tp_0.1:surupol_Reset' : 'Surrogate(true, 0.1, Reset)', 
    # 'Surrogate:initsurpool_false:tp_0:surupol_Reset' : 'Surrogate-No Transfer', 
    }


def report():
    summary_table, test_fitness = summary(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map)
    save_summary(summary_table, output_folder, rename_map)
    improvements(test_fitness, rename_map, dump_file=output_folder / 'improvements')
    wdl2(test_fitness, rename_map=rename_map, dump_file=output_folder/'wdl')
    for exp in test_fitness: 
        exp_name = exp.replace('.', '-').replace(':gen_50', '')
        plot_grid_output(test_fitness[exp], exp_name, inclusion_filter, exclusion_filter, 
                         output_folder, rename_map=rename_map, boxplots=False, lfontsize=65)
        save_stats(test_fitness[exp], exp_name, output_folder, rename_map=rename_map, round_results=True)

if __name__ == '__main__':    
    report()
    ...
