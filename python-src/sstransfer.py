from grid.core import *
from grid.plotting import plot_grid_output
from grid.plotting import plot_surr
from grid.management import *
from grid.management import delete_alg
from grid.core import find_failed
from grid.core import find_all_failed
from grid.management import delete_exp
from grid.stats import *
from scipy.stats import wilcoxon

from grid.pandas.core import get_test_stats

experiments = [
                            'gdb1.vs5.gdb1.vt4:gen_50',
                            'gdb1.vs5.gdb1.vt6:gen_50',
                            'gdb2.vs6.gdb2.vt5:gen_50',
                            'gdb2.vs6.gdb2.vt7:gen_50',
                            'gdb3.vs5.gdb3.vt4:gen_50', 'gdb3.vs5.gdb3.vt6:gen_50',
                            'gdb4.vs4.gdb4.vt3:gen_50',
                            'gdb4.vs4.gdb4.vt5:gen_50',
                            'gdb5.vs6.gdb5.vt5:gen_50', 'gdb5.vs6.gdb5.vt7:gen_50',
                            'gdb6.vs5.gdb6.vt4:gen_50',
                            'gdb6.vs5.gdb6.vt6:gen_50',
                            'gdb7.vs5.gdb7.vt4:gen_50', 'gdb7.vs5.gdb7.vt6:gen_50',
                            # 'gdb8.vs10.gdb8.vt9:gen_50', 'gdb8.vs10.gdb8.vt11:gen_50',
                            'gdb21.vs6.gdb21.vt5:gen_50',
                            'gdb21.vs6.gdb21.vt7:gen_50',
                            'gdb23.vs10.gdb23.vt9:gen_50',
                            'gdb23.vs10.gdb23.vt11:gen_50',
                            # 'val10D.vs10.val10D.vt11:gen_50',
                            # 'val10D.vs10.val10D.vt9:gen_50',

                            'gdb1.vs5.gdb1.vt3:gen_50',
                            'gdb1.vs5.gdb1.vt7:gen_50',
                            'gdb2.vs6.gdb2.vt4:gen_50',
                            'gdb2.vs6.gdb2.vt8:gen_50',
                            'gdb3.vs5.gdb3.vt3:gen_50', 'gdb3.vs5.gdb3.vt7:gen_50',
                            'gdb4.vs4.gdb4.vt2:gen_50',
                            'gdb4.vs4.gdb4.vt6:gen_50',
                            'gdb5.vs6.gdb5.vt4:gen_50', 'gdb5.vs6.gdb5.vt8:gen_50',
                            'gdb6.vs5.gdb6.vt3:gen_50', 'gdb6.vs5.gdb6.vt7:gen_50',
                            'gdb7.vs5.gdb7.vt3:gen_50', 'gdb7.vs5.gdb7.vt7:gen_50',
                            # # 'gdb8.vs10.gdb8.vt8:gen_50', 'gdb8.vs10.gdb8.vt12:gen_50',
                            'gdb21.vs6.gdb21.vt4:gen_50',
                            'gdb21.vs6.gdb21.vt8:gen_50',
                            'gdb23.vs10.gdb23.vt8:gen_50',
                            'gdb23.vs10.gdb23.vt12:gen_50',
                            # 'val10D.vs10.val10D.vt12:gen_50', 'val10D.vs10.val10D.vt8:gen_50',
                            ]
dirbase = Path('/vol/grid-solar/sgeusers/mazhar')
# dirbase = Path('/local/scratch')

# Number of GP generations
GENERATIONS = 50

output_folder = Path('/home/mazhar/Desktop/Results/SSTransfer/Existing-SST-0-FullTree-10')

inclusion_filter = [
    r'WithoutKnowledge$',
    r'^sstransfer',
    # r'Full'
    # r'random_sstransfer',
    # 'SurEvalFullTree:tp_1:metric_hamming:gen_0_49:knnpoolrad_0:knnpoolcap_1:interimrad_0:interimcap_1:interimmag_10:dms_20$',
    # r'FullTree:tp_50:dup_true:clear_false$',
    # r'FullTree:tp_50:dup_true:clear_false:fitness_norm',
    # r'FullTree:tp_50:dup_true:clear_false',
    # r'FullTree:tp_100:dup_false:clear_false$',
    r'FullTree:tp_10:dup_false:clear_false$',
    # r'SurEvalFullTree:.*',
    # r'Subtree:perc_10:clear_false',
    # 'RandPoolFullTree:tp_1:metric_hamming:nrad_0:ncap_1:interimmag_10:dms_20',
    # r'RandPoolFullTree:tp_1:.*',
    # r'FrequentSub.*:clear_false',
    # r'Subtree:perc_50:clear_false',
    r'Subtree:perc_100:clear_false',
]

# Ignore, exclude and do not show the following items
exclusion_filter = [
    ':xotry_3',
    ':histup_true',
    # ':tp_0:',
    ':tp_0.08:',
    ':tp_0.12:',
    ':tp_0.01',
    ':tp_0.05',
    ':tp_0.1:',
    ':tp_0.15',
    ':tp_0.20',
    ':tp_0.5',
]

rename_map = {
    'random_sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.1:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'RandSST-10',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.5:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-50',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.5:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-S-50',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.5:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-50',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.20:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-20',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.20:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-S-20',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.20:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-20',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.15:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-15',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.15:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SST-15',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.15:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-15',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.1:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-10',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.1:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SST-10',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.1:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-10',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-0',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-S-0',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-0',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.05:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-5',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.05:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-S-5',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.05:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-5',

    'sstransfer:tl_true:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.01:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-ST-1',
    'sstransfer:tl_true:histup_false:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.01:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-S-1',
    'sstransfer:tl_false:histup_true:tclrrad_0:tclrcap_1:histsimthresh_0:metric_hamming:dms_20:tp_0.01:bldsim_0:xosim_0:xotry_10:mutsim_0:muttry_10:mutprb_0.5':'SS-T-1',

    'FullTree:tp_50:dup_true:clear_false': 'FullTree-50',
    'FullTree:tp_100:dup_true:clear_false': 'FullTree',
    'FullTree:tp_100:dup_false:clear_false': 'FullTree',
    'FullTree:tp_10:dup_false:clear_false' : 'FullTree-10',
    # 'FullTree:tp_50:dup_true:clear_false:fitness_norm': 'NorFullTree-50',


    # 'Subtree:perc_10:clear_false': 'SubTree-10',
    'Subtree:perc_50:clear_false': 'SubTree',
    'Subtree:perc_100:clear_false': 'SubTree',
    # 'Subtree:perc_100:clear_false':'SubTree',

    # 'SubTree_10':'SubTree-10',
    # 'SubTree_50':'SubTree-50',
}


def failed():
    find_all_failed(dirbase, experiments, inclusion_filter, exclusion_filter)


def report():
    # base_line = 'WithoutKnowledge'
    base_line = ''
    summary_table, test_fitness, fried_table = summary(dirbase, experiments, inclusion_filter, exclusion_filter,
                                                       rename_map, baseline_alg=base_line, num_generations=GENERATIONS)

    save_stat2(summary_table, output_folder, rename_map, fried_table)
    save_summary(summary_table, output_folder, rename_map)
    base_line = 'WithoutKnowledge'
    improvements(test_fitness, rename_map, dump_file=output_folder / 'improvements', num_generations=GENERATIONS, base_line=base_line)
    # with open(output_folder/'fried.csv', 'w') as file:
    #     writer = csv.writer(file)
    #     for key, value in fried.items():
    #         key = key.replace('gdb', 'GDB').replace('.vs', '-').replace('.GDB', '').replace('.vt', '').replace(':gen_50', '')
    #         value = round(value, 2)
    #         value = "{:.2f}".format(value)
    #         writer.writerow([key, value])
    wdl2(test_fitness, rename_map=rename_map, dump_file=output_folder/'wdl',base_line=base_line)
    for exp in test_fitness:
        exp_name = exp.replace('.', '-').replace(':gen_50', '')
        save_stats(test_fitness[exp], exp_name, output_folder, rename_map=rename_map, round_results=True, baseline_alg=base_line, gen=GENERATIONS-1)
        plot_grid_output(test_fitness[exp], exp_name, inclusion_filter, exclusion_filter,
                         output_folder, rename_map=rename_map, boxplots=False, lfontsize=110, linewidth=19)


def restore():
    experiments = [
                            # 'gdb1.vs5.gdb1.vt4:gen_50', 
                            # 'gdb1.vs5.gdb1.vt6:gen_50',
                            # 'gdb2.vs6.gdb2.vt5:gen_50', 'gdb2.vs6.gdb2.vt7:gen_50',
                            # 'gdb3.vs5.gdb3.vt4:gen_50', 'gdb3.vs5.gdb3.vt6:gen_50',
                            # 'gdb4.vs4.gdb4.vt3:gen_50', 'gdb4.vs4.gdb4.vt5:gen_50',
                            # 'gdb5.vs6.gdb5.vt5:gen_50', 'gdb5.vs6.gdb5.vt7:gen_50',
                            # 'gdb6.vs5.gdb6.vt4:gen_50', 'gdb6.vs5.gdb6.vt6:gen_50',
                            # 'gdb7.vs5.gdb7.vt4:gen_50', 'gdb7.vs5.gdb7.vt6:gen_50',
                            # 'gdb8.vs10.gdb8.vt9:gen_50', 'gdb8.vs10.gdb8.vt11:gen_50',
                            # 'gdb9.vs10.gdb9.vt9:gen_50', 'gdb9.vs10.gdb9.vt11:gen_50',
                            # 'gdb10.vs4.gdb10.vt3:gen_50', 'gdb10.vs4.gdb10.vt5:gen_50',
                            # 'gdb12.vs7.gdb12.vt6:gen_50', 'gdb12.vs7.gdb12.vt8:gen_50',
                            # 'gdb13.vs6.gdb13.vt5:gen_50', 'gdb13.vs6.gdb13.vt7:gen_50',
                            # 'gdb14.vs5.gdb14.vt4:gen_50', 'gdb14.vs5.gdb14.vt6:gen_50',
                            # 'gdb15.vs4.gdb15.vt3:gen_50', 'gdb15.vs4.gdb15.vt5:gen_50', 
                            # 'gdb16.vs5.gdb16.vt4:gen_50', 'gdb16.vs5.gdb16.vt6:gen_50',
                            # 'gdb17.vs5.gdb17.vt4:gen_50', 'gdb17.vs5.gdb17.vt6:gen_50',
                            # 'gdb19.vs3.gdb19.vt2:gen_50', 'gdb19.vs3.gdb19.vt4:gen_50',
                            # 'gdb20.vs4.gdb20.vt3:gen_50', 'gdb20.vs4.gdb20.vt5:gen_50',
                            # 'gdb21.vs6.gdb21.vt5:gen_50', 'gdb21.vs6.gdb21.vt7:gen_50',
                            # 'gdb23.vs10.gdb23.vt11:gen_50', 'gdb23.vs10.gdb23.vt9:gen_50',
                            # 'val9C.vs5.val9C.vt4:gen_50', 'val9C.vs5.val9C.vt6:gen_50',
                            # 'val9D.vs10.val9D.vt9:gen_50', 'val9D.vs10.val9D.vt11:gen_50',
                            # 'val10C.vs5.val10C.vt4:gen_50', 'val10C.vs5.val10C.vt6:gen_50',
                            # 'val10D.vs10.val10D.vt11:gen_50',
                            # 'val10D.vs10.val10D.vt9:gen_50',
                            # 'gdb18.vs5.gdb18.vt4:gen_50', 'gdb18.vs5.gdb18.vt6:gen_50',

                            # 'gdb1.vs5.gdb1.vt3:gen_50', 'gdb1.vs5.gdb1.vt7:gen_50',
                            # 'gdb2.vs6.gdb2.vt4:gen_50', 'gdb2.vs6.gdb2.vt8:gen_50',
                            # 'gdb3.vs5.gdb3.vt3:gen_50', 'gdb3.vs5.gdb3.vt7:gen_50',
                            # 'gdb4.vs4.gdb4.vt2:gen_50', 'gdb4.vs4.gdb4.vt6:gen_50',
                            # 'gdb5.vs6.gdb5.vt4:gen_50', 'gdb5.vs6.gdb5.vt8:gen_50',
                            # 'gdb6.vs5.gdb6.vt3:gen_50', 'gdb6.vs5.gdb6.vt7:gen_50',
                            # 'gdb7.vs5.gdb7.vt3:gen_50', 'gdb7.vs5.gdb7.vt7:gen_50',
                            # 'gdb8.vs10.gdb8.vt8:gen_50', 'gdb8.vs10.gdb8.vt12:gen_50',
                            # 'gdb9.vs10.gdb9.vt8:gen_50', 'gdb9.vs10.gdb9.vt12:gen_50',
                            # 'gdb10.vs4.gdb10.vt2:gen_50', 'gdb10.vs4.gdb10.vt6:gen_50',
                            # 'gdb12.vs7.gdb12.vt5:gen_50', 'gdb12.vs7.gdb12.vt9:gen_50',
                            # 'gdb13.vs6.gdb13.vt4:gen_50', 'gdb13.vs6.gdb13.vt8:gen_50',
                            # 'gdb14.vs5.gdb14.vt3:gen_50', 'gdb14.vs5.gdb14.vt7:gen_50',
                            # 'gdb15.vs4.gdb15.vt2:gen_50', 'gdb15.vs4.gdb15.vt6:gen_50', 
                            # 'gdb16.vs5.gdb16.vt3:gen_50', 'gdb16.vs5.gdb16.vt7:gen_50',
                            # 'gdb17.vs5.gdb17.vt4:gen_50', 'gdb17.vs5.gdb17.vt5:gen_50',
                            # 'gdb19.vs3.gdb19.vt1:gen_50', 'gdb19.vs3.gdb19.vt5:gen_50',
                            # 'gdb20.vs4.gdb20.vt2:gen_50', 'gdb20.vs4.gdb20.vt6:gen_50',
                            # 'gdb21.vs6.gdb21.vt4:gen_50', 'gdb21.vs6.gdb21.vt8:gen_50',
                            # 'gdb23.vs10.gdb23.vt8:gen_50', 'gdb23.vs10.gdb23.vt12:gen_50', 
                            # 'val9C.vs5.val9C.vt3:gen_50', 'val9C.vs5.val9C.vt7:gen_50',
                            # 'val9D.vs10.val9D.vt8:gen_50', 'val9D.vs10.val9D.vt12:gen_50',
                            # 'val10C.vs5.val10C.vt3:gen_50', 'val10C.vs5.val10C.vt7:gen_50',
                            # 'val10D.vs10.val10D.vt12:gen_50', 'val10D.vs10.val10D.vt8:gen_50',
]

    algs = [
       'WithoutKnowledge'
    ]
    for alg in algs: 
        unarchive(experiments, alg, '/local/scratch', '/home/mazhar/grid')


def report_time():
    def reduce_runs(run_stats):
        run_summaries = {}
        for run in sorted(run_stats):
            if 'Time' not in run_summaries:
                run_summaries['Time'] = [None] * 30  # len(run_stats)
            if 'ProgSize' not in run_summaries:
                run_summaries['ProgSize'] = [None] * 30  # len(run_stats)

            summed = run_stats[run].sum()
            run_summaries['Time'][run-1] = summed['Time']
            run_summaries['ProgSize'][run-1] = summed['ProgSizeMean'] / 50

        run_summaries['Time'] = list(filter(lambda x: x is not None, run_summaries['Time']))
        run_summaries['ProgSize'] = list(filter(lambda x: x is not None, run_summaries['ProgSize']))
        return run_summaries

    exp_summary = {}
    for exp in experiments:
        ren_exp = rename_exp(exp)
        print('Time: processing', dirbase / exp)
        train_stats = get_train_stat(dirbase / exp, inclusion_filter, exclusion_filter, num_generations=50)
        exp_summary[ren_exp] = {}
        for algorithm in train_stats:
            alg = rename_alg(algorithm, rename_map)
            run_summary = reduce_runs(train_stats[algorithm])
            exp_summary[ren_exp][alg] = run_summary

    def save_sum(ex_summary, aspect):
        average = {}
        std = {}
        for ex in ex_summary:
            average[ex] = {}
            std[ex] = {}
            for al in ex_summary[ex]:
                average[ex][al] = round(statistics.mean(ex_summary[ex][al][aspect]), 2)
                std[ex][al] = round(statistics.stdev(ex_summary[ex][al][aspect]), 2)

        ave_df = pd.DataFrame(average)
        std_df = pd.DataFrame(std)
        fried = friedman_test2(ave_df.T.to_dict('list'), [])
        ave_df['Rank'] = pd.Series({alg[0]: alg[1] for alg in fried[1]})
        posthoc_pvals = pd.DataFrame({'P-Value': dict(fried[2] + [('Friedman', fried[0])])})

        latex_df = ave_df.astype(str).add(r"$\pm$" + std_df.astype(str), fill_value="")
        csv_df = ave_df.astype(str).add(r"(" + std_df.astype(str) + r")", fill_value="")
        if not output_folder.exists():
            output_folder.mkdir(parents=True)
        latex_df.T.to_latex(output_folder / f"{aspect}.tex", escape=False)
        csv_df.T.to_csv(output_folder / f"{aspect}.csv")
        posthoc_pvals.to_latex(output_folder / f"{aspect}_posthoc.tex", escape=False)
        posthoc_pvals.to_csv(output_folder / f"{aspect}_posthoc.csv")

    save_sum(exp_summary, 'Time')
    save_sum(exp_summary, 'ProgSize')

    # progsize_average = {}
    # progsize_std = {}
    # for exp in exp_summary:
    #     time_average[exp] = {}
    #     time_std[exp] = {}
    #     progsize_average[exp] = {}
    #     progsize_std[exp] = {}
    #     for alg in exp_summary[exp]:
    #         time_average[exp][alg] = round(statistics.mean(exp_summary[exp][alg]['Time']), 2)
    #         time_std[exp][alg] = round(statistics.stdev(exp_summary[exp][alg]['Time']), 2)
    #         progsize_average[exp][alg] = round(statistics.mean(exp_summary[exp][alg]['ProgSize']), 2)
    #         progsize_std[exp][alg] = round(statistics.stdev(exp_summary[exp][alg]['ProgSize']), 2)
    #
    # time_ave_df = pd.DataFrame(time_average)
    # time_std_df = pd.DataFrame(time_std)
    # fried = friedman_test2(time_ave_df.T.to_dict('list'), [])
    # time_ave_df['Rank'] = pd.Series({alg[0]: alg[1] for alg in fried[1]})
    # posthoc_pvals = pd.DataFrame({'P-Value': dict(fried[2] + [('Friedman', fried[0])])})
    #
    # latex_df = time_ave_df.astype(str).add(r"$\pm$" + time_std_df.astype(str), fill_value="")
    # csv_df = time_ave_df.astype(str).add(r"(" + time_std_df.astype(str) + ", " + r")", fill_value="")
    # latex_df.T.to_latex(output_folder / "Time.tex")
    # csv_df.T.to_csv(output_folder / "Time.csv")
    # posthoc_pvals.to_latex(output_folder / "Time_posthoc.tex")
    # posthoc_pvals.to_csv(output_folder / "Time_posthoc.cdv")


if __name__ == '__main__':    
    report()
    report_time()
    # failed()
    # restore()

    ...
