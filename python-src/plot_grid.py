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

from grid.exp_stats import wdl
from grid.core import get_test_fitness
from grid.plotting import plot_grid_output
from grid.exp_stats import save_stats
from grid.plotting import plot_surr

experiments = [
                            # 'egl-e1-A.vs5:gen_200',
                            # 'egl-e1-B.vs7:gen_200'
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
                            # 'gdb6.vs5.gdb6.vt4:gen_50',
                            # 'gdb6.vs5.gdb6.vt6:gen_50',
                            # 'gdb7.vs5.gdb7.vt4:gen_50',
                            # 'gdb7.vs5.gdb7.vt6:gen_50',
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
                            # 'gdb13.vs6:gen_50',
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

output_folder = Path('/home/mazhar/Desktop/plots')

inclusion_filter = [
    r'WithoutKnowledge:clear_false',
    r'WithoutKnowledge$',
    r'Surrogate:initsurpool_true:tp_0:surupol_FIFONoDupPhenotypic',
    r'Surrogate:initsurpool_true:tp_0.1:surupol_FIFONoDupPhenotypic',
    r'Surrogate:initsurpool_true:tp_0:surupol_AddOnce',
    r'Surrogate:initsurpool_true:tp_0.1:surupol_AddOnce',
    r'Surrogate:initsurpool_true:tp_0:surupol_Reset',
    r'Surrogate:initsurpool_true:tp_0.1:surupol_Reset',
    r'Surrogate:initsurpool_false:tp_0:surupol_Reset',
    r'Surrogate:initsurpool_true:tp_0:surupol_Entropy',
    r'Surrogate:initsurpool_true:tp_0.1:surupol_Entropy',
    r'Surrogate:initsurpool_false:tp_0:surupol_Entropy',
    r'FullTree:tp_10:dup_true:clear_true',
]

# Ignore, exclude and do not show the following items
exclusion_filter = [
]

rename_map = {
    'Surrogate:initsurpool_true:tp_0:surupol_FIFONoDupPhenotypic' : 'Surrogate(true, 0, FIFONoDupPhenotypic)',
    'Surrogate:initsurpool_true:tp_0.1:surupol_FIFONoDupPhenotypic' : 'Surrogate(true, 0.1, FIFONoDupPhenotypic)',
    'Surrogate:initsurpool_true:tp_0:surupol_AddOncePhenotypic': 'Surrogate(true, 0, AddOnce)',
    'Surrogate:initsurpool_true:tp_0.1:surupol_AddOncePhenotypic': 'Surrogate(true, 0.1, AddOnce)',
    'Surrogate:initsurpool_true:tp_0:surupol_Entropy':'Surrogate(true, 0, Entropy)',
    'Surrogate:initsurpool_true:tp_0.1:surupol_Entropy':'Surrogate(true, 0.1, Entropy)',
    'Surrogate:initsurpool_false:tp_0:surupol_Entropy':'Surrogate(false, 0, Entropy)',    
    'FullTree:tp_10:dup_true:clear_true': 'FullTree(10, dup, clear)',
    'Surrogate:initsurpool_true:tp_0:surupol_Reset' : 'Surrogate(true, 0, Reset)', 
    'Surrogate:initsurpool_true:tp_0.1:surupol_Reset' : 'Surrogate(true, 0.1, Reset)', 
    'Surrogate:initsurpool_false:tp_0:surupol_Reset' : 'Surrogate-No Transfer', 
    }

if __name__ == '__main__':
    wdl(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map=rename_map)
    # plot_surr()

    # delete_alg(experiments=experiments, algortithm_to_delete='Surrogate:initsurpool_true:tp_0:surupol_UnboundedPhenotypic$', save_to='')

    # test_train_scatter()

    # calc_all_improvements(base_line="WithoutKnowledge:clear_true")
    # compare_test()
    # delete_exp()
    # compress_grid()
    # test_plot()
    # all_fit = get_all_test_fitnesses()

    for exp in experiments:
        print('\n', exp)
        plot_surr(dirbase, exp, inclusion_filter)
        test_fitness = get_test_fitness(dirbase / exp, inclusion_filter, exclusion_filter, num_generations=50)
        plot_grid_output(test_fitness, exp, inclusion_filter, exclusion_filter, output_folder, rename_map=rename_map, boxplots=True)
        save_stats(test_fitness, exp, output_folder, rename_map=rename_map, round_results=True, baseline_alg='WithoutKnowledge')

    #     test_fitness = get_test_fitness(dirbase / exp)
    #     # train_fitness = get_train_mean(dirbase / exp)
    #     # plot_grid_output(train_fitness, dirbase / exp, False)
    #     # imps = find_improvement(train_fitness)
    #     imps = find_improvement(test_fitness, base_line='WithoutKnowledge')
    #     # for alg in imps: 
    #     #     print(alg, imps[alg], '\n')

    #     update_improvement_percentage(imps, imp_perc_table, imp_table)


    # # improv_hist(imp_table)
    # # with open('improvements.json', 'w') as f: 
    # #     # json.dump(imp_perc_table, f)
    # #     json.dump(imp_table, f)

    # for alg in imp_perc_table: 
    #     print(alg, imp_perc_table[alg] / len(experiments), '\n')

    # with open('mean_wdl.json', 'w') as f:
    #     json.dump(wdltable, f)
    # for alg in wdltable:
    #     print(alg, wdltable[alg])
    ...
