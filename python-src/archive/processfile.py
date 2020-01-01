import os
import re
import statistics
from scipy import stats
from pathlib import Path
import subprocess
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sbn

sbn.set()

# dirpath = '/vol/grid-solar/sgeusers/mazhar/val9C-v5-to6/'
experiments = [
                            # 'gdb1-v5-to4',
                            # 'gdb1-v5-to6', 
                            # 'gdb2-v6-to5', 
                            # 'gdb2-v6-to7', 
                            # 'gdb21-v6-to5', 
                            # 'gdb21-v6-to7', 
                            # 'niche-gdb1-v5-to4',
                            # 'niche-gdb1-v5-to6', 
                            # 'niche-gdb2-v6-to5', 
                            # 'niche-gdb2-v6-to7', 
                            # 'niche-gdb21-v6-to5', 
                            # 'niche-gdb21-v6-to7', 
                            # 'niche-gdb8-v10-to9', 
                            # 'niche-gdb8-v10-to11', 
                            # 'niche-gdb9-v10-to9', 
                            # 'niche-gdb9-v10-to11', 
                            # 'niche-gdb23-v10-to9', 
                            # 'niche-gdb23-v10-to11', 
                            # 'niche-val9C-v5-to4', 
                            # 'niche-val9C-v5-to6', 
                            # 'niche-val9D-v10-to9', 
                            'niche-val9D-v10-to11', 
                            # 'niche-val10C-v5-to4', 
                            # 'niche-val10C-v5-to6', 
                            # 'gdb8-v10-to9', 
                            # 'gdb8-v10-to11', 
                            # 'gdb9-v10-to9', 
                            # 'gdb9-v10-to11', 
                            # 'gdb23-v10-to9', 
                            # 'gdb23-v10-to11', 
                            # 'val9C-v5-to4', 
                            # 'val9C-v5-to6', 
                            # 'val9D-v10-to9', 
                            # 'val9D-v10-to11', 
                            # 'val10C-v5-to4', 
                            # 'val10C-v5-to6', 
                            # 'val10D-v10-to9', 
                            # 'val10D-v10-to11',
                            # 'egl-e1-A-v5-to7',
                            ]
# dirbase = Path('/home/mazhar/grid/')
dirbase = Path('/home/mazhar/scratch/CEC/')

output_folder = Path('/home/mazhar/Desktop/plots')
generations = 50

# Algorithms in this list will be ignored and not processed
filter = [ 'analyze_terminals', 'gen35-49', 'fitall', 'terminal-prevgen', 
          'p0.8-r0.8',
          'cwt',
          'terminal-30runs',
          'rink', 
          'maveric',
          'prevgen35', 
          'terminal-p1', 
          'terminal-p0', 
        #   'Depthed',
          'SubTree25', 
        #   'SubTree50',
          'SubTree75', 
          'SubTree100',
          'FullTree25', 
        #   'FullTree50', 
          'FullTree75', 
          'FullTree100', 
          'GTLKnow', 
        #   'fulltree', 
          'abscontrib', 
        #   'TLGPCriptor',
        #   'subcontrib-gen49-49-all-noniche',
        #   'FrequentSub-root', 
        #   'FrequentSub-all', 
        #   'subcontrib-gen49-49-all',
         'subcontrib-gen49-49-subtree',
         'depthed_frequent-rootsubtree',
        #  'depthed_frequent-all',
        #  'frequent-Root',
        #  'frequent-all',
        'depthed_frequent-Root',
          '-wok-niched',
        # 'subtree', 'fulltree', 'GTLKnowlege', 
          'BestGenKnowledge2', 
        #   'BestGenKnowledge',
        # 'FrequentSub',
]

positive_filter = [
    # 'gen49-49-all-noniche',
    # 'gen49-49-subtree-noniche',
    # 'FrequentSub-rootsubtree', 
    # 'frequent-Rootsubtree',
]

# If true, the resutls in tables will be rounded to two decimal points.
round_results = True

def should_process(alg):
    for f in positive_filter:
        if f.lower() in alg.lower():
            return True
    for f in filter:
        if f.lower() in alg.lower():
            return False
    return True

def load_csv(file, comma=','):
    
    csv_matrix = []
    for line in open(file):
        line = line.strip('\n')
        items = line.split(comma)
        items = map(lambda item: item.strip('\t').strip(' '), items)
        csv_matrix.append(list(items))

    return csv_matrix

def sort_algorithms(algorithm):
    algorithm = sorted(algorithm, key=lambda s: s.lower())
    for i in range(len(algorithm)):
        if 'wok' in algorithm[i]:
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


def get_test_stat(experiment_path):

    gen_mean_ts = {}

    def update_gen_mean_ts(file):
        nonlocal gen_mean_ts
        try:
            csv = load_csv(file)
            if not algorithm in gen_mean_ts:
                gen_mean_ts[algorithm] = {}
            for gen in range(generations):
                if not gen in gen_mean_ts[algorithm]:
                    gen_mean_ts[algorithm][gen] = {}
                gen_mean_ts[algorithm][gen][int(run)] = float(csv[gen + 1][7])
                # if 'wok' in algorithm and gen == 7:
                #     print(run, ':', gen_mean_ts[algorithm][gen][int(run)])
        except Exception as exp:
            print(exp)
            print(file)
            print(algorithm, gen, run)
            raise exp

    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))        

    for run in runs:
        if run.isnumeric() == False:
            continue
        # Now inside 'experiment_path/run'
        
        (_, algorithms, _) = next(os.walk(experiment_path / run / 'stats'))
        for algorithm in algorithms:
            # Now inside 'experiment_path/run/stats/algorithm'

            if (experiment_path / run / 'stats' / algorithm / 'test').exists():
                # Now inside 'experiment_path/run/stats/algorithm/test'

                (_, _, test_files) = next(os.walk(experiment_path / run / 'stats' / algorithm / 'test'))
                for test_file in test_files:
                    if not test_file.endswith('.csv'):
                        continue
                    update_gen_mean_ts(experiment_path / run / 'stats' / algorithm / 'test' / test_file)

    return gen_mean_ts


def plot_grid_output(experiment_path):

    gen_mean_ts = {}

    def update_gen_mean_ts(file):
        nonlocal gen_mean_ts
        try:
            csv = load_csv(file)
            if not algorithm in gen_mean_ts:
                gen_mean_ts[algorithm] = {}
            for gen in range(generations):
                if not gen in gen_mean_ts[algorithm]:
                    gen_mean_ts[algorithm][gen] = {}
                gen_mean_ts[algorithm][gen][int(run)] = float(csv[gen + 1][7])
                # if 'wok' in algorithm and gen == 7:
                #     print(run, ':', gen_mean_ts[algorithm][gen][int(run)])
        except Exception as exp:
            print(exp)
            print(file)
            print(algorithm, gen, run)
            raise exp

    def plot_mean_of_genbest_tr(gen_mean):

        markers = (marker for marker in ['.', ',', 'o', 'v', '^', '<', '>'
                                            , '1', '2', '3', '4', 's', 'p', '*', 'h', 'H', '+', 'x'
                                            , 'D', 'd', '|', '_'])
        line_styles = ['--', '-.', ':']
        sc = 0 # line style counter
        
        # figsize=(width, height)
        fig_all = plt.figure(figsize=(60, 32))
        ax_all = fig_all.add_subplot(111)
        ax_all.set_xlabel('Generation', fontdict={'fontsize':120 })
        ax_all.set_ylabel('Test Performance in Target Domain', fontdict={'fontsize':90 })
        ax_all.tick_params(axis='both', which='major', labelsize=100)

        # Algorithms in this list will be ignored and filtered out
        
        for algorithm in sort_algorithms(gen_mean):
            if not should_process(algorithm):
                continue

            box_data = []
            mean_data = []
            for gen in gen_mean[algorithm]:
                box_data.append((list(gen_mean[algorithm][gen].values())))
                mean_data.append(statistics.mean(list(gen_mean[algorithm][gen].values())))
                # print(algorithm, ', ', gen, ': ', statistics.mean(list(gen_mean[algorithm][gen].values())))

            fig = plt.figure(figsize=(12,14))
            ax = fig.add_subplot(311)
            ax.set_title(algorithm)
            ax.set_xlabel('Generation')
            ax.set_ylabel('Fitness')
            # ax.tick_params(axis='both', which='major', labelsize=100)
            # ax.boxplot(box_data)

            ax = fig.add_subplot(312)
            ax.set_title(algorithm + ': average of 30 runs per generation')
            ax.set_xlabel('Generation')
            ax.set_ylabel('Average fitness')
            ax.plot(range(1, len(mean_data) + 1), mean_data)
            if not 'writeknow' in algorithm:
                if 'v10-to-11' in algorithm:
                    print(algorithm)
                label = algorithm.replace('Knowledge1', '')\
                                    .replace('depthed_frequent', 'GPHH-BST-Freq')\
                                    .replace('Root', 'root')\
                                    .replace('wok', 'Without Transfer')\
                                    .replace('niching-gen49-49-', '')\
                                    .replace('subtree100', 'GPHH-TT-SubTree100')\
                                    .replace('fulltree100', 'GPHH-TT-FullTree100')\
                                    .replace('subcontrib-gen49-49-all-noniche', 'GPHH-BST-Contrib')\
                                    .replace('cwt-p1-r1-all', 'GPHH-FIT')\
                                    .replace('-all', '')\
                                    .split('-wk-')[-1]
                                    # .replace('depthed_frequent', 'FrequentSub')\
                                    # .replace('subcontrib-gen49-49-all-noniche', 'ContribSub-all')\
                                    # .replace('subcontrib-gen49-49-subtree-noniche', 'ContribSub-subtree')\
                                    # .replace('cwt-p0.8-r0.8-first', 'GPHH-FIT(0.8,0)')\
                                    # .replace('cwt-p0.8-r0.8-all', 'GPHH-FIT(0.8,0.$8^g$)')\
                                     # .replace('TLGPCriptor', 'GPHH-TT-TLGPCriptor')\
                label = label[0].upper() + label[1:]


                if 'wok' in algorithm:
                    ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth = 15,  markersize=15, label="Without Transfer", color='k')
                else:
                    sc += 1 
                    sc %= 3 

                    ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth = 14
                                       , linestyle=line_styles[sc], marker=next(markers), markersize=5)

            ax = fig.add_subplot(313)
            ax.set_title(algorithm + ': combo')
            ax.set_xlabel('Generation')
            ax.set_ylabel('Fitness')
            # ax.boxplot(box_data)
            ax.plot(range(1, len(mean_data) + 1), mean_data)
            if not Path(output_folder / experiment_path.name).exists(): 
                Path(output_folder / experiment_path.name).mkdir()
            # fig.savefig(output_folder / f'{experiment_path.name + "/" + algorithm}.jpg')
            print('Saved', output_folder / f'{experiment_path.name + "/" + algorithm}.jpg')
        leg = ax_all.legend(fontsize=120, ncol=1, markerscale=7)
        for line in leg.get_lines():
            line.set_linewidth(12)
        
        fig_all.savefig(output_folder / experiment_path.name / f'{experiment_path.name}-all.jpg', bbox_inches='tight', pad_inches=0)
        print(output_folder / experiment_path.name / f'{experiment_path.name}-all.jpg')

    # Folder structure is 'experiment_path/run/stats/algorithm/test'
    # experiment_path is the top-level folder that contains all the runs: 
    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))        

    for run in runs:
        if run.isnumeric() == False:
            continue
        # Now inside 'experiment_path/run'
        
        (_, algorithms, _) = next(os.walk(experiment_path / run / 'stats'))
        for algorithm in algorithms:
            # Now inside 'experiment_path/run/stats/algorithm'

            if (experiment_path / run / 'stats' / algorithm / 'test').exists():
                # Now inside 'experiment_path/run/stats/algorithm/test'

                (_, _, test_files) = next(os.walk(experiment_path / run / 'stats' / algorithm / 'test'))
                for test_file in test_files:
                    if not test_file.endswith('.csv'):
                        continue
                    update_gen_mean_ts(experiment_path / run / 'stats' / algorithm / 'test' / test_file)

    plot_mean_of_genbest_tr(gen_mean_ts)
    print('Finished plotting')


def get_experiment_stats(experiment_path):

    experiment_ts_fitnesses = {}
    def get_exp_stats():
        nonlocal experiment_ts_fitnesses

        # if not algorithm in experiment_tr_fitnesses:
        #     experiment_tr_fitnesses[algorithm] = {}  
        if not algorithm in experiment_ts_fitnesses: 
            experiment_ts_fitnesses[algorithm] = {}

        if not test_file.endswith('.csv'):
            return

        # tr_fit, ts_fit = process_csv(experiment_path / run / 'stats' / algorithm / 'test' / test_file)
        # experiment_tr_fitnesses[algorithm][run] = tr_fit
        # experiment_ts_fitnesses[algorithm][run] = ts_fit

        csv_matrix = load_csv(experiment_path / run / 'stats' / algorithm / 'test' / test_file)
        ts_fit = float(csv_matrix[-1][7])
        # experiment_tr_fitnesses[algorithm][run] = tr_fit
        experiment_ts_fitnesses[algorithm][run] = ts_fit


    def save_exp_stats():
        wok_exp = ''
        for algorithm in experiment_ts_fitnesses:
            if 'wok' in algorithm:
                wok_exp = algorithm
                break

        if not Path(output_folder / experiment_path.name).exists(): 
            Path(output_folder / experiment_path.name).mkdir()

        csv_file = open(output_folder / f'{experiment_path.name + "/" + "stats"}.csv', 'w')
        latex_file = open(output_folder / f'{experiment_path.name + "/" + "stats"}.tex', 'w')

        csv_file.write('Algorithm, Best (run), Worst (run), Mean, Stdev, WilcoxPVal\n')
        latex_file.write(r'\begin{table}[]' + '\n'
                       + r'\resizebox{\columnwidth}{!}{%'  + '\n'
                       + r'\begin{tabular}{llllll} \hline'  + '\n'
                       + r'Algorithm & Best (run)     & Worst (run)          & Mean               & Stdev              & WilcoxPVal           \\ \hline' + '\n')

        for algorithm in sorted(experiment_ts_fitnesses):
            miin = min(experiment_ts_fitnesses[algorithm].values())
            miin_index = int([i for i in experiment_ts_fitnesses[algorithm] if experiment_ts_fitnesses[algorithm][i] == miin][0])
            worst = max(experiment_ts_fitnesses[algorithm].values())
            worst_index = int([i for i in experiment_ts_fitnesses[algorithm] if experiment_ts_fitnesses[algorithm][i] == worst][0])
            mean = statistics.mean(experiment_ts_fitnesses[algorithm].values())
            stdev = statistics.stdev(experiment_ts_fitnesses[algorithm].values())
            if algorithm != wok_exp:
                # print(algorithm, wok_exp)
                if len(experiment_ts_fitnesses[algorithm].values()) != 30 or len(experiment_ts_fitnesses[wok_exp].values()) != 30:
                    print()
                pval = stats.wilcoxon(list(experiment_ts_fitnesses[algorithm].values()), list(experiment_ts_fitnesses[wok_exp].values()))[1]

            else:
                pval = '--'
            # if 'writeknow' in algorithm:
            #     continue

            if 'wok' in algorithm:
                algorithm = 'Without Transfer'
            elif 'writeknow' in algorithm:
                # continue
                algorithm = algorithm
            else:
                algorithm = algorithm.split('-wk-')[-1]
                algorithm = algorithm[0].upper() + algorithm[1:]

            if round_results:
                miin = round(miin, 2)
                worst = round(worst, 2)
                mean = round(mean, 2)
                stdev = round(stdev, 2)
                if isinstance(pval, float):
                    pval = round(pval, 2)

            csv_file.write(f'{algorithm}, {miin}, {worst}, {mean}, {stdev}, {pval}\n')
            algorithm.replace('_', '\\_')
            latex_file.write(f'{algorithm} & {miin} ({miin_index}) & {worst} ({worst_index}) & {mean} & {stdev} & {pval}\\\\\n')

        csv_file.close()
        latex_file.write(r'\hline \end{tabular}' + '\n'
                       + r'}' + '\n'
                       + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
                       + r'\end{table}')
        latex_file.close()

    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))

    for run in runs:
        if run.isnumeric() == False:
            continue
        # Now inside 'experiment_path/run'

        (_, algorithms, _) = next(os.walk(experiment_path / run / 'stats'))
        for algorithm in algorithms:
            if not should_process(algorithm):
                continue
            # Now inside 'experiment_path/run/stats/algorithm'

            if (experiment_path / run / 'stats' / algorithm / 'test').exists():
                # Now inside 'experiment_path/run/stats/algorithm/test'

                (_, _, test_files) = next(os.walk(experiment_path / run / 'stats' / algorithm / 'test'))
                for test_file in test_files:
                    if not test_file.endswith('.csv'):
                        continue
                    get_exp_stats()


    save_exp_stats()     

def get_num_nodes(tree):
    # tree = '[(CTT1 min (((FULL * (DC max FUT)) min (((FUT / CTD) min (CR + FULL)) min ((CFH / FULL) min (FUT / CTD)))) max FULL)), use:1, dup: 0, o:]'
    nodes = tree.split(" ")

    return len(nodes) - 5

def num_nodes_in_file(file):
    retval = 0
    num_trees = 0
    for line in open(file):
        if len(line.strip()) == 0 or 'null' in line:
            continue

        num_trees += 1
        num_nodes = get_num_nodes(line)
        if num_nodes < 0:
            print(num_nodes)

        retval += num_nodes
    
    if num_trees == 0:
        return 0

    return retval / num_trees

def get_node_count(experiment_path):
    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))     
    node_count = {}
    for run in runs:
        if run.isnumeric() == False:
            continue
        # Now inside 'experiment_path/run'
        
        (_, algorithms, _) = next(os.walk(experiment_path / run / 'stats'))
        for algorithm in algorithms:
            if not should_process(algorithm):
                continue

            # Now inside 'experiment_path/run/stats/algorithm'
            (_, _, files) = next(os.walk(experiment_path / run / 'stats' / algorithm))
            file = [f for f in files if f.endswith('.succ.log')]
            if len(file) == 0:
                continue
            
            alg = algorithm.replace('Knowledge1', '')\
                    .replace('depthed_frequent', 'GPHH-BST-Freq')\
                    .replace('Root', 'root')\
                    .replace('wok', 'Without Transfer')\
                    .replace('niching-gen49-49-', '')\
                    .replace('subtree100', 'GPHH-TT-SubTree100')\
                    .replace('fulltree100', 'GPHH-TT-FullTree100')\
                    .replace('subcontrib-gen49-49-all-noniche', 'GPHH-BST-Contrib')\
                    .replace('cwt-p1-r1-all', 'GPHH-FIT')\
                    .replace('-all', '')\
                    .split('-wk-')[-1]

            num_nodes = num_nodes_in_file(experiment_path / run / 'stats' / algorithm / file[0])
            if num_nodes != 0:
                if not alg in node_count:
                    node_count[alg] = {run:0}
                if not run in node_count[alg]:
                    node_count[alg][run] = 0
                node_count[alg][run] = node_count[alg][run] + num_nodes

    for alg in node_count:
        print(alg + ": ", statistics.mean(node_count[alg].values()))


if __name__ == '__main__':
    for exp in experiments:
        # stats = get_test_stat(dirbase / exp)
        # print(stats)
        # get_experiment_stats(dirbase / exp)
        # plot_grid_output(dirbase / exp)
        print('\n', exp)
        get_node_count(dirbase / exp)