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
                            'gdb6.vs5.gdb6.vt4:gen_50',
                            'gdb6.vs5.gdb6.vt6:gen_50',
                            'gdb7.vs5.gdb7.vt4:gen_50',
                            'gdb7.vs5.gdb7.vt6:gen_50',
                            
                            'gdb12.vs7.gdb12.vt6:gen_50',
                            'gdb12.vs7.gdb12.vt8:gen_50',
                            'gdb13.vs6.gdb13.vt5:gen_50',
                            'gdb13.vs6.gdb13.vt7:gen_50',
                            'gdb14.vs5.gdb14.vt4:gen_50',
                            'gdb14.vs5.gdb14.vt6:gen_50',
                            'gdb15.vs4.gdb15.vt3:gen_50',
                            'gdb15.vs4.gdb15.vt5:gen_50', 
                            'gdb16.vs5.gdb16.vt4:gen_50',
                            'gdb16.vs5.gdb16.vt6:gen_50',
                            'gdb17.vs5.gdb17.vt4:gen_50',
                            'gdb17.vs5.gdb17.vt6:gen_50',
                            'gdb19.vs3.gdb19.vt2:gen_50',
                            'gdb19.vs3.gdb19.vt4:gen_50',
                            'gdb20.vs4.gdb20.vt3:gen_50',
                            'gdb20.vs4.gdb20.vt5:gen_50',

                            # 'gdb18.vs5.gdb18.vt4:gen_50',
                            # 'gdb18.vs5.gdb18.vt6:gen_50',

                            'gdb21.vs6.gdb21.vt5:gen_50',
                            'gdb21.vs6.gdb21.vt7:gen_50',

                            # 'gdb8.vs10.gdb8.vt9:gen_50',
                            # 'gdb8.vs10.gdb8.vt11:gen_50',
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
                            # 'gdb3.vs6:gen_50',
                            # 'gdb8.vs10:gen_50',
                            # 'gdb9.vs10:gen_50',
                            # 'gdb21.vs6:gen_50',
                            # 'gdb23.vs10:gen_50',
                            # 'val9C.vs5:gen_50',
                            # 'val9D.vs10:gen_50',mean
                            # 'val10C.vs5:gen_50',
                            # 'val10D.vs10:gen_50',

                            # 'gdb1.vs4:gen_200',
                            # 'gdb1.vs5:gen_200',
                            # 'gdb1.vs6:gen_200',
                            # 'gdb2.vs5:gen_200',
                            # 'gdb2.vs6:gen_200',
                            # 'gdb2.vs7:gen_200',
                            # 'gdb8.vs9:gen_200',
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

# Algorithms in this list will be ignored and not processed
filter = [
          ':incap_1$',
          ':mnThr_0.03$',
          ':mnThr_0.01$',
          'BestGen:k_1', 
          'BestGen:k_2', 
          'GTLKnow',
          'FullTree_10',
          'FullTree_25',
          r'TournamentFull',
        #   r'Subtree_50',
        #   r'Frequent*',
        #   'WithoutKnowledge',
          r'sim_',
          r':percent_0.1',
          r':tp_20', # Transfer percent
          r'tp_25',
        #   r'tp_50',
          r'gens_25_49', 
          r'PPTPipeLearning*',
          r'PPTFreqLearning*',
        #   r'lr_0.9',
          r'lr_0.7',
        #   r'ss_512',
          r'ss_1000',
        #   r'ss_100',
          r'ss_80',
        #   r'ts_7',
        #   r'ts_20',
        #   r'ts_-1',
          r':ts_50',
        #   r'nrad_-1',
        #   r'nrad_0.0',
        #   r'nrad_0.1',
        #   r'ncap_3',
        #   r'PPTFreqLearning*',
          r'MutatingSubtree*',
          r'PPTLearn*',
          r'mnThr_0\.03$',
          r'mnThr_0\.01$',
          r'Thr_0$',
          r'incap_1$',
        #   r'WithoutKnowledge:clear_true',
          r'^WithoutKnowledge$',
          r'ppt_0.4', 
          r'ppt_0.2', 
          r'ppt_0.325', 
        #   r'mnThr_0\.03$',
]

dirbase = Path('/home/mazhar/grid/')
# dirbase = Path('/local/scratch')

# Number of GP generations
GENERATIONS = 50

output_folder = Path('/home/mazhar/Desktop/plots')

inclusion_filter = [
    # r'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_false',
    r'PPTBreeding:ppt_0:cmpppt_0:xover_0.8:mut_0.15:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    r'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    r'PPTBreeding:ppt_0.325:cmpppt_0:xover_0.325:mut_0.3:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    
    r'PPTBreeding:ppt_0:cmpppt_0:xover_0.8:mut_0.15:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    r'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    r'PPTBreeding:ppt_0.325:cmpppt_0:xover_0.325:mut_0.3:repro_0.05:lr_.*:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',

    # r'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    r'FullTree:tp_10:dup_true:clear_true',
    r'FullTree:tp_50:dup_false:clear_true',
    # r'Subtree:perc_10:clear_true',
    r'Subtree:perc_50:clear_true',

    r'^FrequentSub.*:clear_true',
    r'^FrequentSub.*:clear_false',
    # r'^FullTree:tp.*:clear_true',
    # r'^PPTBreeding.*:clear_true',
    # r'^PPTBreeding.*:clear_false',
    # r'^Subtree:.*:clear_true',
    # r'PPTFreqLearning.*ts_20',

    # r'PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_20',
    # r'PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.7:ss_100:ts_20',
    # r'PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.9:ss_100:ts_20',
    r'^FullTree:',
    # r'^Subtree:',
    r'WithoutKnowledge:clear_true'
]

# Ignore, exclude and do not show the following items
exclusion_filter = [
    # PPTBreeding
    # r'ppt_0.1', 
    # r'ppt_0.2', 
    # r'ppt_0.325',
    r'ppt_0.4', 
    r'ppt_0.6',
    r'ss_500',
    # r'lr_0.1',
    r'lr_0.8',
    # r'lr_0.9',
    # r'initperc_0.1',
    # r'initperc_0.5',

    # r'percent_0.1',
    # r'ss_80',
    # r'ss_500',
    # r'ss_1000'

    # FullTree
    # r'FullTree:tp_10',
    r'FullTree:tp_25',
    r'FullTree:tp_20',
    # r'FullTree:tp_50',

    # Subtree
    # r'Subtree:perc_10',
    r'Subtree:perc_20',
    # r'Subtree:perc_50',

    # r'^FrequentSub:extperc_0\.1',
    # r'^FrequentSub:extperc_0\.2',
    # r'^FrequentSub:extperc_0\.5',

    # FrequentSub
    # r'tranperc_0\.1', # FrequentSub
    r'tranperc_0\.2', # FrequentSub
    # r'tranperc_0\.5', # FrequentSub

    # r'WithoutKnowledge$',
]

def should_process(alg, inclusion_filter = inclusion_filter, exclusion_filter = exclusion_filter):
    included = False
    for f in inclusion_filter:
        if not re.search(f, alg):
            # print("", alg, "for", f)
            continue
        else:
            included = True
            break
    if included:
        for f in exclusion_filter: 
            if re.search(f, alg):
                included = False
                # print("Excluded", alg, 'for', f)
                break
        
    return included

def get_test_fitness(experiment_path, num_generations = GENERATIONS, *test_fitness):
    # A multi-dimensional dictionary that contains all the test fitness values of all the algorithms 
    # for all the runs:
    # test_fitness['FrequentSub'][1][2] will return the test fitness on run=2 of gen=1 of the algorithm='FreqSub'
    test_fitness = {}

    def update_test_fitness(file, algorithm, run):
        nonlocal test_fitness
        try:
            csv = pd.read_csv(file)
            if not algorithm in test_fitness:
                test_fitness[algorithm] = {}
            for gen in range(num_generations):
                if not gen in test_fitness[algorithm]:
                    test_fitness[algorithm][gen] = {}
                if csv.shape[0] - 1 <= gen : # -1 is for the header
                    print("Warning: The csv file does not contain generation:", gen)
                    return
                test_fitness[algorithm][gen][int(run)] = float(csv.iloc[gen]['TestFitness'])
        except Exception as exp:
            print(exp)
            print(file)
            print(algorithm, gen, run)
            raise exp

    experiment_path = Path(experiment_path)
    (_, algorithms, _) = next(os.walk(experiment_path))
    for algorithm in algorithms:
        if not should_process(algorithm):
            continue
        (_, runs, _) = next(os.walk(experiment_path / algorithm))
        for run in runs:
            if int(run) > 30: # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not be considered at all
                continue
            test_dir = experiment_path / algorithm / run / 'test'
            if not (test_dir).exists():
                print('Warning: the test folder does not exist on: ', experiment_path/algorithm/run)
                continue
            (_, _, test_files) = next(os.walk(test_dir))
            for test_file in test_files:
                if not test_file.endswith('.csv'):
                    continue
                update_test_fitness(test_dir / test_file, algorithm, run)

    return test_fitness

def get_train_mean(experiment_path, *mean_train_fitness):
    """
    Reads the 'jobs.0.stat.csv' file of experiments and collects the population mean of each generation.
    """
    # A multi-dimensional dictionary that contains all the test fitness values of all the algorithms 
    # for all the runs:
    # test_fitness['FrequentSub'][1][2] will return the test fitness on run=2 of gen=1 of the algorithm='FreqSub'
    mean_train_fitness = {}

    def update_mean_train_fitness(file, algorithm, run):
        nonlocal mean_train_fitness
        try:
            csv = pd.read_csv(file)
            if not algorithm in mean_train_fitness:
                mean_train_fitness[algorithm] = {}
            for gen in range(GENERATIONS):
                if not gen in mean_train_fitness[algorithm]:
                    mean_train_fitness[algorithm][gen] = {}
                mean_train_fitness[algorithm][gen][int(run)] = float(csv.iloc[gen]['FitMean'])
        except Exception as exp:
            print(exp)
            print(file)
            print(algorithm, gen, run)
            raise exp

    experiment_path = Path(experiment_path)
    (_, algorithms, _) = next(os.walk(experiment_path))
    for algorithm in algorithms:
        if not should_process(algorithm):
            continue
        (_, runs, _) = next(os.walk(experiment_path / algorithm))
        for run in runs:
            if int(run) > 30: # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not be considered at all
                continue
            train_dir = experiment_path / algorithm / run 
            if not (train_dir).exists():
                print('Warning: the train folder does not exist on: ', experiment_path/algorithm/run)
                continue
            stat_file = train_dir / 'job.0.stat.csv'

            if not (stat_file).exists():
                print('Warning: the stat file does not exist: ', stat_file)
                continue
            update_mean_train_fitness(stat_file, algorithm, run)

    return mean_train_fitness

def rename_alg(algorithm):
    algorithm =    algorithm.replace('WithoutKnowledge:clear_true', 'Without Transfer')\
                            .replace('Knowledge', 'Transfer')\
                            .replace('FullTree:tp_10:dup_true:clear_true', 'FullTree-10') \
                            .replace('FullTree:tp_20:dup_true:clear_true', 'FullTree-20') \
                            .replace('FullTree:tp_50:dup_false:clear_true', 'FullTree-50') \
                            .replace('FrequentSub:extract_all:extperc_0.1:tranperc_0.1:clear_true', 'FreqSub-10') \
                            .replace('FrequentSub:extract_all:extperc_0.2:tranperc_0.2:clear_true', 'FreqSub-20') \
                            .replace('FrequentSub:extract_all:extperc_0.5:tranperc_0.5:clear_true', 'FreqSub-50') \
                            .replace('FrequentSub:extract_all:extperc_0.1:tranperc_0.5:clear_true', 'FreqSub-50') \
                            .replace('Subtree:perc_10:clear_true', 'SubTree-10')\
                            .replace('Subtree:perc_10:clear_true', 'SubTree-10')\
                            .replace('Subtree:perc_50:clear_true', 'SubTree-50')\
                            .replace('PPTBreeding:', 'PKGPHH(') \
                            .replace(':cmpppt_0', '') \
                            .replace(':ss_100', '') \
                            .replace(':ts:_7', '') \
                            .replace(':inrad_-1:incap_1', '') \
                            .replace(':mnThr_0', '') \
                            .replace(':igen_49_49', '') \
                            .replace(':repro_0.05', '') \
                            .replace('ppt_', '') \
                            .replace('xover_', '') \
                            .replace('mut_', '') \
                            .replace('lr_', '') \
                            .replace('initperc_', '') \
                            .replace(':clear_true', ')') \
                            .replace(':clear_false', ') - not cleared') \
                            .replace(':', ', ')
                            # .replace(':clear_true', ')') \
                            # .replace('ppt_0.2', '0.2') \
                            # .replace('ppt_0.4', '0.4') \
                            # .replace(':clear_true', '') \
                            # .replace(':ss_100', '') \
                            # .replace(':ss_80', '80') \
                            # .replace(':ss_-1', ' --') \
                            # .replace(':ts_20', ', 20)') \
                            # .replace(':ts_7', ', ') \
                            # .replace(':nrad_-1:ncap_2', '(') \
                            # .replace(':nrad_0.0:ncap_2', '(') \
                            # .replace(':nrad_0.0:ncap_1', '(') \
                            # .replace(':nrad_0.1:ncap_2', '(') \
                            # .replace(':nrad_0.1:ncap_1', '(') \
                            # .replace(':ss_512', '512') \
                            # .replace(':ts_-1', ', --)') \
                            # .replace('Root', 'root')\
                            # .replace(':percent_0.5', '') \
                            # .replace('TLGPCriptor', 'GPHH-TT-TLGPCriptor')\
                            # .replace(':lr_0.8', '') \
                            # .replace('FullTree:tp_50:dup_false', 'FullTree-50') \
                            # .replace(':ts', '') \
    return algorithm

def sort_algorithms(algorithm):
    algorithm = sorted(algorithm, key=lambda s: s.lower())
    for i in range(len(algorithm)):
        if 'WithoutKnowledge' in algorithm[i]:
            temp = algorithm.pop(i)
            algorithm.insert(0, temp)
            break
            # algorithm[0] = algorithm[i]
            # algorithm[i] = temp
        # if 'TLGPC' in algorithm[i]:
        #     temp = algorithm[i]
        #     algorithm.remove(algorithm[i])
        #     algorithm.append(temp)
        # if 'all' in algorithm[i]:
        #     for j in range(i, len(algorithm)):
        #         if 'first' in algorithm[j]:
        #             temp = algorithm[i]
        #             algorithm[i] = algorithm[j]
        #             algorithm[j] = temp
        
    return algorithm

def plot_grid_output(test_fitness, experiment_path, boxplots=False, lcols=1, lfontsize=85, linewidth=19):

    markers = ["", ',', 'o', 'v', '^', '<', '>', 's', 'p', '*', 'h', 'H', '+', 'x', 'D', 'd', '|', '_']
    line_styles = ['--', '-.', ':']
    sc = 0 # line style counter
    mc = 0 # marker counter
    
    fig_all = plt.figure(figsize=(60, 32))
    ax_all = fig_all.add_subplot(111)
    ax_all.set_xlabel('Generation', fontdict={'fontsize':120 })
    ax_all.set_ylabel('Fitness', fontdict={'fontsize':120 })
    ax_all.tick_params(axis='both', which='major', labelsize=100)

    # Algorithms in this list will be ignored and filtered out
    
    for algorithm in sort_algorithms(test_fitness):
    # for algorithm in (test_fitness):
        if not should_process(algorithm):
            continue

        box_data = []
        mean_data = []
        for gen in test_fitness[algorithm]:
            box_data.append((list(test_fitness[algorithm][gen].values())))
            mean_data.append(statistics.mean(list(test_fitness[algorithm][gen].values())))
            # print(algorithm, ', ', gen, ': ', statistics.mean(list(gen_mean[algorithm][gen].values())))

        if not Path(output_folder / experiment_path.name).exists(): 
            Path(output_folder / experiment_path.name).mkdir()

        # if not Path(output_folder ).exists(): 
        #     Path(output_folder ).mkdir()

        if boxplots:
            # box_fig = plt.figure(figsize=(12,14))
            box_fig = plt.figure(figsize=(60,32))
            # box_ax = box_fig.add_subplot(311)
            # box_ax = box_fig.add_subplot(111)
            # box_ax.set_title(rename_alg(algorithm))
            # box_ax.set_xlabel('Generation', fontdict={'fontsize':120 })
            # box_ax.set_ylabel('Fitness', fontdict={'fontsize':120 })
            # box_ax.tick_params(axis='both', which='major', labelsize=20)
            # box_ax.boxplot(box_data)

            # box_ax = box_fig.add_subplot(312)
            # box_ax.set_title(rename_alg(algorithm) + ': average of 30 runs per generation')
            # box_ax.set_xlabel('Generation')
            # box_ax.set_ylabel('Average fitness')
            # box_ax.plot(range(1, len(mean_data) + 1), mean_data)
            box_ax = box_fig.add_subplot(111)
            box_ax.set_title(rename_alg(algorithm), fontdict={'fontsize':100 })
            box_ax.set_xlabel('Generation', fontdict={'fontsize':120 })
            box_ax.set_ylabel('Fitness', fontdict={'fontsize':120 })

            flierprops = dict(marker='o', markerfacecolor='r', markersize=30, linestyle='none')
            boxprops = dict(linestyle='-', linewidth=6, color='black')
            whiskerprops = dict(linewidth=6)
            medianprops = dict(linewidth=6)

            box_ax.boxplot(box_data, flierprops=flierprops, boxprops=boxprops, whiskerprops=whiskerprops,medianprops=medianprops)
            box_ax.tick_params(axis='both', which='major', labelsize=45)
            box_ax.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth/4)
            box_fig.savefig(output_folder / f'{experiment_path.name + "/" + algorithm}.jpg', bbox_inches='tight', pad_inches=0)
            plt.close(box_fig)
            print('Saved', output_folder / f'{experiment_path.name + "/" + algorithm}.jpg')

        if not 'writeknow' in algorithm:
            label = rename_alg(algorithm)
            # label = algorithm
            # label = label[0].upper() + label[1:]

            if algorithm == "WithoutKnowledge:clear_true":
                ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth,  markersize=15, label="Without Transfer", color='k')
            else:
                sc += 1 
                sc %= len(line_styles) 
                mc += 1
                mc %= len(markers)

                ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth = linewidth
                                    , linestyle=line_styles[sc], marker=markers[mc], markersize=30)

    leg = ax_all.legend(fontsize=lfontsize, ncol=lcols, markerscale=1)
    for line in leg.get_lines():
        line.set_linewidth(20)
    
    fig_all.savefig(output_folder / experiment_path.name / f'{experiment_path.name}-all.jpg', bbox_inches='tight', pad_inches=0)
    # fig_all.savefig(Path("/home/mazhar/Desktop/EuroGP 2020") / f'{experiment_path.name}-all.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig_all)
    # print(Path("/home/mazhar/Desktop/EuroGP 2020") / f'{experiment_path.name}-all.jpg')

    # fig_all.savefig(output_folder / f'{experiment_path.name}-all.eps', bbox_inches='tight', pad_inches=0)
    # print(output_folder / f'{experiment_path.name}-all.eps')

def improv_hist(imp_table):
    box_fig = plt.figure(figsize=(12,14))
    box_ax = box_fig.add_subplot(111)
    box_ax.set_title('algorithm')
    box_ax.set_xlabel('Generation')
    box_ax.set_ylabel('Fitness')

    labels, data = imp_table.keys(), imp_table.values()

    ren_labels = []
    for label in labels:
        label = rename_alg(label)
        ren_labels.append(label)
    box_ax.boxplot(data)
    # box_ax.set_xticks(range(1, len(labels) + 1), ren_labels)
    box_ax.set_xticklabels(ren_labels, rotation=90)
    box_fig.savefig('improvements.jpg')
    

def save_stats(test_fitness, experiment_path, round_results = False, baseline_alg = 'WithoutKnowledge'):
    if not Path(output_folder / experiment_path.name).exists(): 
        Path(output_folder / experiment_path.name).mkdir()

    csv_file = open(output_folder / f'{experiment_path.name + "/" + "train_stats"}.csv', 'w')
    latex_file = open(output_folder / f'{experiment_path.name + "/" + "train_stats"}.tex', 'w')

    experiment = experiment_path.name
    experiment = r'\textbf{' + experiment.replace('gen_50', '').replace('.', ' ') + '}'
    experiment = r'\multicolumn{3}{c}{' + experiment + r'}'
    csv_file.write('Algorithm, Mean, Stdev, Min, Max, median, p_wi\n')
    latex_file.write(r'\begin{table}[]' + '\n'
                       + r'\resizebox{\columnwidth}{!}{%'  + '\n'
                       + r'\begin{tabular}{llll} \hline'  + '\n'
                       + f'Scenario: & {experiment}' + r'\\ \hline' + '\n'
                       + r'Algorithm & Mean(Stdev) & Min & Max & $p_{WT}$          \\ \hline' + '\n')

    for alg in sort_algorithms(test_fitness):
        if alg == 'FullTree_25' or alg == 'FullTree_50':
            continue
        # if len(list(test_fitness[alg][0].values())) < 30:
        #     print("Imbalanced data: ", alg)
        #     continue

        mini = min(list(test_fitness[alg][49].values()))
        maxi = max(list(test_fitness[alg][49].values()))
        mean = statistics.mean(list(test_fitness[alg][49].values()))
        std =statistics.stdev(list(test_fitness[alg][49].values()))
        median = statistics.median(list(test_fitness[alg][49].values()))
        # if alg == 'FullTree:tp_50:dup_false':
        #     pval_full = '--'
        # else:
        #     pval_full = stats.wilcoxon(list(test_fitness['FullTree:tp_50:dup_false'][0].values()), list(test_fitness[alg][0].values()))[1]
        
        if alg == baseline_alg:
            pval_wo_wil = '--'
            # pval_wo_t = '--'
        else: 
            # pval_wo = stats.mannwhitneyu(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]
            if len(list(test_fitness[baseline_alg][49].values())) != len(list(test_fitness[alg][49].values())):
                print("Len of ", alg, "(", len(list(test_fitness[alg][49].values())), ") is not 30.")
                pval_wo_wil = -1
                # pval_wo_t = -1
            else:
                pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]
                # pval_wo_t = stats.ttest_rel(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]

        if round_results:
            mini = round(mini, 2)
            maxi = round(maxi, 2)
            mean = round(mean, 2)
            std = round(std, 2)
            median = round(median, 2)
            if isinstance(pval_wo_wil, float):
                pval_wo_wil = round(pval_wo_wil, 2)
            # if isinstance(pval_wo_t, float):
            #     pval_wo_wil = round(pval_wo_t, 2)

        # alg = rename_alg(alg)
        latex_file.write(f'{alg} & {mean}({std}) & {mini} & {maxi} & {pval_wo_wil} \\\\\n')
        csv_file.write(f'{alg}, {mean}, {std}, {mini}, {maxi}, {median}, {pval_wo_wil} \n')
        # print(alg, mean, std, pval_full, pval_wo)
    
    csv_file.close()
    latex_file.write(r'\hline \end{tabular}' + '\n'
                    + r'}' + '\n'
                    + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
                    + r'\end{table}')
    latex_file.close()

def compare_train():
    '''
    Compare the train performance of two generations 50 and 200.
    '''
    for exp in experiments:
        alg = 'KnowledgeSource'
        train_fitness = get_train_mean(dirbase / exp)
        mean1 = round(statistics.mean(list(train_fitness[alg][49].values())), 2)
        median1 = round(statistics.median(list(train_fitness[alg][49].values())), 2)

        mean2 = round(statistics.mean(list(train_fitness[alg][199].values())), 2)
        median2 = round(statistics.median(list(train_fitness[alg][199].values())), 2)

        # std =statistics.stdev(list(test_fitness[alg][49].values()))
        pval_wo = stats.wilcoxon(list(train_fitness[alg][49].values()), list(train_fitness[alg][199].values()))[1]
        print(exp, mean1, mean2, median1, median2, round(pval_wo, 2))

def compare_test():
    '''
    Compare the test performance of two generations 50 and 200.
    '''

    out = open('stats.csv', 'w')
    out.write('dataset, average, median, min, max\n')
    for exp in experiments:
        print('\n')
        alg = 'KnowledgeSource'
        test_fitness = get_test_fitness(dirbase / exp, 200)
        fit1 = list(test_fitness[alg][49].values())
        fit2 = list(test_fitness[alg][199].values())

        min1 = round(min(fit1), 2)
        # min1_run = fit1.index(min(fit1))
        min1_run = [i for i in test_fitness[alg][49] if test_fitness[alg][49][i] == min(fit1)][0]
        min2 = round(min(fit2), 2)
        min2_run = [i for i in test_fitness[alg][199] if test_fitness[alg][199][i] == min(fit2)][0]

        max1 = round(max(fit1), 2)
        max2 = round(max(fit2), 2)

        mean1 = round(statistics.mean(fit1), 2)
        mean2 = round(statistics.mean(fit2), 2)

        median1 = round(statistics.median(fit1), 2)
        median2 = round(statistics.median(fit2), 2)

        pval_wo = stats.wilcoxon(fit1, fit2)[1]
        print(exp)
        print("mean:\t", mean1, mean2)
        print("median:\t", median1, median2)
        print("min:\t", f"{min1}({min1_run})", f"{min2}({min2_run})")
        print("max:\t", max1, max2)
        print("pval:\t", round(pval_wo, 2), "\n")

        out.write(f'{exp}\n')
        out.write(f'50, {mean1}, {median1}, {min1}, {max1}\n')
        out.write(f'200, {mean2}, {median2}, {min2}, {max2}\n')
        out.write(f'PVal, {round(pval_wo, 3)}\n')
        out.write('\n')
        out.flush()
    out.close()

def delete_exp_files(experiment_path, alg, file_name):
    """
    Deletes specified files in given experiments to save space. 
    """
    alg = alg.lower()
    file_name = file_name.lower()
    experiment_path = Path(experiment_path)
    (_, algorithms, _) = next(os.walk(experiment_path))
    for algorithm in algorithms:
        if not re.search(alg, algorithm.lower()):
            continue
        (_, runs, _) = next(os.walk(experiment_path / algorithm))
        for run in runs:
            if int(run) in range(1, 5):
                continue
            run_path = Path((experiment_path / algorithm) / run)
            (_, _, files) = next(os.walk(run_path))
            for file in files:
                if re.search(file_name, file.lower()):
                    print(run_path / file)
                    os.remove(run_path / file)
                    
def compress_grid():
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            if (Path('/local/scratch/') / exp / (algorithm + ".tar.bz2")).is_file():
                continue
            shutil.make_archive(Path('/local/scratch/') / exp / algorithm, 'bztar', Path(dirbase) / exp / algorithm)
            print(Path('/local/scratch/') / exp / algorithm, 'finished.')

def delete_exp():
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            if (not re.search(r'KnowledgeSource', algorithm)): # and (not re.search(r'PPTPipe', algorithm)):
                continue
            # if not re.search(r'PPTPipe', algorithm):
            #     continue
            (_, runs, _) = next(os.walk(dirbase / exp / algorithm))
            for run in runs: 
                if (not (Path('/local/scratch/') / exp / algorithm / (run + ".tar.bz2")).is_file()):
                    print(Path(dirbase) / exp / algorithm / run, ' is not backed up. Backing it up now.')
                    shutil.make_archive(Path('/local/scratch/') / exp / algorithm / run, 'bztar', Path(dirbase) / exp / algorithm / run)
                    print(Path(dirbase) / exp / algorithm / run, ' backed up.')
                    
                    shutil.rmtree(Path(dirbase) / exp / algorithm / run)
                    print(Path(dirbase) / exp / algorithm / run, ' deleted.')


def find_improvement(train_fitness, *, base_line='WithoutKnowledge:clear_true', generations=GENERATIONS):

    retval = {} 
    for alg in train_fitness:
        if alg == base_line: 
            continue
        retval[alg] = []
        for i in range(generations):
            # if len(list(train_fitness[base_line][generations-1].values())) !=  len(list(train_fitness[alg][i].values())):
            #     print('Imbalanced len:', alg, 'at generation:', i)
            #     continue

            pval_wo_wil = stats.wilcoxon(list(train_fitness[base_line][generations-1].values())[:len(train_fitness[alg][i].values())], list(train_fitness[alg][i].values()))[1]
            if pval_wo_wil >= 0.05:
                # The performance is similar now. 
                retval[alg].append(0)
            if pval_wo_wil < 0.05:
                mean_alg = statistics.mean(list(train_fitness[alg][i].values()))
                mean_bse = statistics.mean(list(train_fitness[base_line][generations-1].values()))
                if mean_alg > mean_bse:
                    # Oh oh
                    retval[alg].append(-1) # = f'{i} failed.'
                else: 
                    retval[alg].append(1)

    return retval

def update_improvement_percentage(improvements, improve_perc_table, improve_table):
    # retval = {}
    for alg in improvements: 
        # retval[alg] = 0
        if alg not in improve_perc_table:
            improve_perc_table[alg] = 0
            improve_table[alg] = []
        improvement = improvements[alg]
        improved = 0
        for i in range(len(improvement)):
            if improvement[i] >= 0 :
                improved += 1
                if improved == 3: 
                    # retval[alg] = 1 -  (i + 1 - improved)/len(improvement)
                    perc = 1 -  (i + 1 - improved)/len(improvement)
                    improve_perc_table[alg] += perc
                    improve_table[alg].append(perc)
                    break
            else: 
                improved = 0
    # return retval

def wdl(test_data, base_line, wdltable):

    # wins = 0
    # draws = 0
    # loses = 0
    
    bmean = statistics.mean(list(test_data[base_line][49].values()))
    for alg in test_data:
        if alg == base_line:
            continue

        if not alg in wdltable:
            # wins, draws, losses, missing 
            wdltable[alg] = [0, 0, 0, 0]

        try:
            mean = statistics.mean(list(test_data[alg][49].values()))
            # base_mean = statistics.mean(list(test_data[base_line][49].values()))
        except KeyError as e:
            print(alg, e)
            wdltable[alg][3] += 1
            continue
        if len(list(test_data[base_line][49].values())) != len(list(test_data[alg][49].values())):
            print("Len of ", alg, "(", len(list(test_data[alg][49].values())), ") is not 30.")
            # pval_wo_wil = -1
            wdltable[alg][3] += 1
            continue
        if mean < bmean : 
            wdltable[alg][0] += 1
        elif mean == bmean:
            wdltable[alg][1] += 1
        else:
            wdltable[alg][2] += 1
        # pval_wo_wil = stats.wilcoxon(list(test_data[base_line][49].values()), list(test_data[alg][49].values()))[1]

        # if pval_wo_wil < 0.05:
        #     if mean < bmean:
        #         wdltable[alg][0] += 1
        #     else: 
        #         wdltable[alg][2] += 1
        # else:
        #     wdltable[alg][1] += 1

        

# def get_all_test_fitnesses(dirbase=dirbase):
#     """
#     Iterates over all experiments and loads the test fitnesses for all algorithms in them (filters applied).
#     The return value is a dictionary that maps each experiment name to a dictionary that maps algorithms to 
#     fitness values
#     """
#     retval = {}
#     for exp in experiments:
#         print('\n', exp)
#         test_fitness = get_test_fitness(dirbase / exp)
#         # if exp not in retval: 
#         #     retval[exp] = {}
#         retval[exp] = test_fitness
# 
#     return retval

def calc_all_improvements(expbase=dirbase, base_line = 'WithoutKnowledge:clear_true', file_name=''):
    imp_perc_table = {} # Holds the sum of improvements that each algorithm achieves over all experiments
    imp_table = {} # Holds a list of improvements that each algorithm achieves over all experiments
    for exp in experiments:
        print(exp, '\n')
        test_fitness = get_test_fitness(dirbase / exp)
        imps = find_improvement(test_fitness)
        update_improvement_percentage(imps, imp_perc_table, imp_table)
    

    for alg in imp_perc_table: 
        imp_perc_table[alg] = imp_perc_table[alg] / len(experiments)
        print(alg, imp_perc_table[alg], '\n')
    
    if file_name:
        with open(file_name+"-perc") as f:
            json.dump(imp_perc_table, f)
        with open(file_name+"-list") as f:
            json.dump(imp_table, f)

        return imp_perc_table, imp_table

def test_train_scatter(algorithm=[
    'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_false',
    # 'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
    # 'FullTree:tp_10:dup_true:clear_true',
    'FullTree:tp_50:dup_false:clear_true',
    # 'Subtree:perc_10:clear_true',
    'Subtree:perc_50:clear_true',
    ],
    base_line='WithoutKnowledge:clear_true'):

    fig = plt.figure(figsize=(20, 14))
    markers = ['o', 'v', '^', 's', 'p', '*', 'h', '+', 'x', 'D', 'd', '|', '_']
    cnt = 0

    experiments = [
                            'gdb1.vs5.gdb1.vt4:gen_50',
                            # 'gdb1.vs5.gdb1.vt6:gen_50',
                            # 'gdb2.vs6.gdb2.vt5:gen_50',
                            # 'gdb2.vs6.gdb2.vt7:gen_50',
                            # 'gdb3.vs5.gdb3.vt4:gen_50',
                            'gdb3.vs5.gdb3.vt6:gen_50',
                            # 'gdb4.vs4.gdb4.vt3:gen_50',
                            # 'gdb4.vs4.gdb4.vt5:gen_50',
                            # 'gdb5.vs6.gdb5.vt5:gen_50',
                            # 'gdb5.vs6.gdb5.vt7:gen_50',
                            # 'gdb6.vs5.gdb6.vt4:gen_50',
                            # 'gdb6.vs5.gdb6.vt6:gen_50',
                            'gdb7.vs5.gdb7.vt4:gen_50',
                            # 'gdb7.vs5.gdb7.vt6:gen_50',
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
                            'gdb19.vs3.gdb19.vt2:gen_50',
                            # 'gdb19.vs3.gdb19.vt4:gen_50',
                            # 'gdb20.vs4.gdb20.vt3:gen_50',
                            # 'gdb20.vs4.gdb20.vt5:gen_50',
                            # 'gdb18.vs5.gdb18.vt4:gen_50',
                            # 'gdb18.vs5.gdb18.vt6:gen_50',
                            # 'gdb21.vs6.gdb21.vt5:gen_50',
                            # 'gdb21.vs6.gdb21.vt7:gen_50',
]


    fontsize=18
    ncol=1
    markerscale=1
    markersize=12
    labelsize=30

    exp = experiments[0]
    print('\n', exp)
    test_fitness = get_test_fitness(dirbase / exp)
    train_fitness = get_train_mean(dirbase / exp)
    cnt = 0
    for alg in [*algorithm, base_line]:
        x = []
        y = []
        for run in range(1, 31):
            x.append(train_fitness[alg][49][run])
            y.append(test_fitness[alg][49][run])

        # x = list(train_fitness[alg][49].values())
        # y = list(test_fitness[alg][49].values())
        ax = fig.add_subplot(221)
        ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
        # ax.set_title(algorithm + ': combo')
        # ax.set_xlabel('Generation')
        # ax.set_ylabel('Fitness')
        cnt += 1
        # ax.legend()
    leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
    ax.tick_params(axis='both', which='major', labelsize=labelsize)
    for line in leg.get_lines():
        line.set_linewidth(25)

    # plt.show()


    exp = experiments[1]
    print('\n', exp)
    test_fitness = get_test_fitness(dirbase / exp)
    train_fitness = get_train_mean(dirbase / exp)
    cnt = 0
    for alg in [*algorithm, base_line]:
        x = []
        y = []
        for run in range(1, 31):
            x.append(train_fitness[alg][49][run])
            y.append(test_fitness[alg][49][run])

        # x = list(train_fitness[alg][49].values())
        # y = list(test_fitness[alg][49].values())
        ax = fig.add_subplot(222)
        ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt],s=markersize**2)
        # ax.set_title(algorithm + ': combo')
        # ax.set_xlabel('Generation')
        # ax.set_ylabel('Fitness')
        cnt += 1
        # ax.legend()
    leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
    ax.tick_params(axis='both', which='major', labelsize=labelsize)
    for line in leg.get_lines():
        line.set_linewidth(25)
        
    exp = experiments[2]
    print('\n', exp)
    test_fitness = get_test_fitness(dirbase / exp)
    train_fitness = get_train_mean(dirbase / exp)
    cnt = 0
    for alg in [*algorithm, base_line]:
        x = []
        y = []
        for run in range(1, 31):
            x.append(train_fitness[alg][49][run])
            y.append(test_fitness[alg][49][run])

        # x = list(train_fitness[alg][49].values())
        # y = list(test_fitness[alg][49].values())
        ax = fig.add_subplot(223)
        ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
        # ax.set_title(algorithm + ': combo')
        # ax.set_xlabel('Generation')
        # ax.set_ylabel('Fitness')
        cnt += 1
        # ax.legend()
    leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
    ax.tick_params(axis='both', which='major', labelsize=labelsize)
    for line in leg.get_lines():
        line.set_linewidth(25)

    cnt = 0
    exp = experiments[3]
    print('\n', exp)
    test_fitness = get_test_fitness(dirbase / exp)
    train_fitness = get_train_mean(dirbase / exp)
    for alg in [*algorithm, base_line]:
        x = []
        y = []
        for run in range(1, 31):
            x.append(train_fitness[alg][49][run])
            y.append(test_fitness[alg][49][run])

        # x = list(train_fitness[alg][49].values())
        # y = list(test_fitness[alg][49].values())
        ax = fig.add_subplot(224)
        ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
        # ax.set_title('Train Fitness')
        # ax.set_xlabel('Generation')
        # ax.set_ylabel('Fitness')
        cnt += 1
        # ax.legend()

    leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
    ax.tick_params(axis='both', which='major', labelsize=labelsize)
    for line in leg.get_lines():
        line.set_linewidth(25)

    # ax.set_xlabel('Train Fitness')
    # ax.set_ylabel('Test Fitness')
    fig.savefig('scatter.jpg')
    plt.show()
            

if __name__ == '__main__':
    # test_train_scatter()

    # calc_all_improvements(base_line="WithoutKnowledge:clear_true")
    # compare_test()
    # delete_exp()
    # compress_grid()
    # test_plot()
    # all_fit = get_all_test_fitnesses()

    wdltable = {}
    # # imp_perc_table = {}
    # # imp_table = {}
    for exp in experiments:
        print('\n', exp)
        test_fitness = get_test_fitness(dirbase / exp)
        wdl(test_fitness, 'WithoutKnowledge:clear_true', wdltable)
        # plot_grid_output(test_fitness, dirbase / exp, boxplots=True)
    #     save_stats(test_fitness, dirbase /exp, round_results=True, baseline_alg='WithoutKnowledge')

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

    with open('mean_wdl.json', 'w') as f:
        json.dump(wdltable, f)
    for alg in wdltable:
        print(alg, wdltable[alg])
