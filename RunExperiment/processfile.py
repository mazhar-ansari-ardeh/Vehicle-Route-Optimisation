import os
import re
import statistics
from scipy import stats
from pathlib import Path
import subprocess
import matplotlib.pyplot as plt

# dirpath = '/vol/grid-solar/sgeusers/mazhar/val9C-v5-to6/'
experiments = ['gdb1-v5-to4', 'gdb1-v5-to6', 'gdb2-v6-to5', 'gdb2-v6-to7'
                            , 'gdb9-v10-to11'
                            , 'gdb21-v6-to5'
                            , 'gdb21-v6-to7'
                            , 'gdb8-v10-to9', 'gdb8-v10-to11', 'gdb9-v10-to9'
                            , 'gdb23-v10-to9', 'gdb23-v10-to11', 'val9C-v5-to4', 'val9C-v5-to6', 'val9D-v10-to9', 'val9D-v10-to11'
                            , 'val10C-v5-to4'
                            , 'val10C-v5-to6'
                            , 'val10D-v10-to9', 'val10D-v10-to11'
                            ]
dirbase = Path('/home/mazhar/scratch/CEC/')
dirpath = dirbase / 'val10D-v10-to9'
generations = 50

def bin_to_txt_pop(path_to_folder, print_output = True):
    '''Convert binary population to text population'''

    tmp_path = str(path_to_folder)
    subprocess.call(['java', '-jar', 'tl.jar', tmp_path])

def process_csv(file, train_fitness_ind = 6, test_fitness_ind = 7):
    file = open(file)

    train_fitness = []
    test_fitness = []
    for line in file:
        nums = line.split(',')
        if not nums[0].isnumeric():
            continue
        train_fitness.append(float(nums[train_fitness_ind].replace(' ', '')))
        test_fitness.append(float(nums[test_fitness_ind].replace(' ', '')))

    # print(train_fitness)
    # print(test_fitness)
    file.close()

    return train_fitness[-1], test_fitness[-1]

def process_grid_output(experiment_path):
    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))

    experiment_tr_fitnesses = {}
    experiment_ts_fitnesses = {}

    for run in runs:
        if run.isnumeric() == False:
            continue
        
        (_, experiments, _) = next(os.walk(experiment_path / run / 'stats'))
        for algorithm in experiments:
            # inside the directory: experiment_path/run/stats/algorithm/ 

            # Convert binary population to text population
            # bin_to_txt_pop(experiment_path / run / 'stats' / algorithm)

            if not algorithm in experiment_tr_fitnesses:
                experiment_tr_fitnesses[algorithm] = {}  
            if not algorithm in experiment_ts_fitnesses: 
                experiment_ts_fitnesses[algorithm] = {}
 
            if not (experiment_path / run / 'stats' / algorithm / 'test').exists():
                continue 
            (_, _, files) = next(os.walk(experiment_path / run / 'stats' / algorithm / 'test'))
            for file in files:
                # inside the directory: experiment_path/run/stats/algorithm/test


                if not file.endswith('.csv'):
                    continue

                tr_fit, ts_fit = process_csv(experiment_path / run / 'stats' / algorithm / 'test' / file)
                experiment_tr_fitnesses[algorithm][run] = tr_fit
                experiment_ts_fitnesses[algorithm][run] = ts_fit
                break

    wok_exp = ''
    for algorithm in experiment_ts_fitnesses:
        if 'wok' in algorithm:
            wok_exp = algorithm
            break

    for algorithm in experiment_ts_fitnesses:
        print(algorithm + ':')
        run_values = experiment_ts_fitnesses[algorithm].values()
        min_val = min(run_values)
        for ind in experiment_ts_fitnesses[algorithm].keys():
            if min_val == experiment_ts_fitnesses[algorithm][ind]:
                min_ind = ind
                break
        print('\tmin:\t ', min_ind, ':', min(experiment_ts_fitnesses[algorithm].values()))
        print('\tmean:\t ', statistics.mean(experiment_ts_fitnesses[algorithm].values()))
        print('\tstdev:\t ', statistics.stdev(experiment_ts_fitnesses[algorithm].values()))
        if algorithm == wok_exp:
            continue
        print("\tWilcoxon:", end='')
        print(stats.wilcoxon(list(experiment_ts_fitnesses[algorithm].values()), list(experiment_ts_fitnesses[wok_exp].values()))[1])




def load_csv(file, comma=','):
    
    csv_matrix = []
    for line in open(file):
        line = line.strip('\n')
        items = line.split(comma)
        items = map(lambda item: item.strip('\t').strip(' '), items)
        csv_matrix.append(list(items))

    return csv_matrix

def plot_grid_output(experiment_path):

    # Folder structure is 'experiment_path/run/stats/algorithm/test'

    # experiment_path is the top-level folder that contains all the runs: 
    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))

    gen_mean_ts = {}

    def update_gen_mean_ts(file):
        nonlocal gen_mean_ts
        csv = load_csv(file)
        if not algorithm in gen_mean_ts:
            gen_mean_ts[algorithm] = {}
        for gen in range(generations):
            if not gen in gen_mean_ts[algorithm]:
                gen_mean_ts[algorithm][gen] = {}
            gen_mean_ts[algorithm][gen][int(run)] = float(csv[gen + 1][7])
            # if 'wok' in algorithm and gen == 7:
            #     print(run, ':', gen_mean_ts[algorithm][gen][int(run)])

    def plot_mean_of_genbest_tr(gen_mean):
        markers = (marker for marker in ['.', ',', 'o', 'v', '^', '<', '>'
                                            , '1', '2', '3', '4', 's', 'p', '*', 'h', 'H', '+', 'x'
                                            , 'D', 'd', '|', '_'])
        line_styles = ['--', '-.', ':']
        sc = 0
        
        # figsize=(width, height)
        fig_all = plt.figure(figsize=(60, 32))
        ax_all = fig_all.add_subplot(111)
        ax_all.set_xlabel('Generation', fontdict={'fontsize':45 })
        ax_all.set_ylabel('Fitness', fontdict={'fontsize':45 })
        
        for algorithm in sorted(gen_mean):
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
            # ax.boxplot(box_data)

            ax = fig.add_subplot(312)
            ax.set_title(algorithm + ': average of 30 runs per generation')
            ax.set_xlabel('Generation')
            ax.set_ylabel('Average fitness')
            # ax.plot(range(1, len(mean_data) + 1), mean_data)
            if not 'writeknow' in algorithm:
                label = algorithm.replace('Knowledge', '')\
                                    .replace('depthed_frequent', 'FrequentSub')\
                                    .replace('Root', 'root')\
                                    .replace('wok', 'Without Transfer')\
                                    .split('-wk-')[-1]
                label = label[0].upper() + label[1:]

                if 'wok' in algorithm:
                    ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth = 5,  markersize=12, label="Without Transfer", color='k')
                else:
                    sc += 1 
                    sc %= 3 

                    ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth = 5
                                       , linestyle=line_styles[sc], marker=next(markers), markersize=12)

            ax = fig.add_subplot(313)
            ax.set_title(algorithm + ': combo')
            ax.set_xlabel('Generation')
            ax.set_ylabel('Fitness')
            # ax.boxplot(box_data)
            # ax.plot(range(1, len(mean_data) + 1), mean_data)
            if not Path(experiment_path.name).exists(): 
                Path(experiment_path.name).mkdir()
            # fig.savefig(f'{experiment_path.name + "/" + algorithm}.jpg')
            print('Saved', f'{experiment_path.name + "/" + algorithm}.jpg')
        output_folder = Path('/home/mazhar/MyPhD/MyPapers/Generalizability of GPTL/')
        leg = ax_all.legend(fontsize=72, ncol=2, markerscale=2)
        # for line in leg.get_lines():
        #     line.set_linewidth(10)
        
        for handle in leg.legendHandles:
            handle._markersize = [30]
        # fig_all.savefig(experiment_path.name + "/" + algorithm.split('-')[0] + '-all.jpg')
        fig_all.savefig(output_folder / (experiment_path.name + '-all.jpg'))
        

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
    print('Finished')


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

        if not Path(experiment_path.name).exists(): 
            Path(experiment_path.name).mkdir()

        csv_file = open(f'{experiment_path.name + "/" + "stats"}.csv', 'w')
        latex_file = open(f'{experiment_path.name + "/" + "stats"}.tex', 'w')

        csv_file.write('Algorithm, Best, Mean, Stdev, WilcoxPVal\n')
        latex_file.write(r'\begin{table}[]' + '\n'
                       + r'\resizebox{\columnwidth}{!}{%'  + '\n'
                       + r'\begin{tabular}{lllll} \hline'  + '\n'
                       + r'Algorithm & Best               & Mean               & Stdev              & WilcoxPVal           \\ \hline' + '\n')

        for algorithm in sorted(experiment_ts_fitnesses):
            miin = min(experiment_ts_fitnesses[algorithm].values())
            mean = statistics.mean(experiment_ts_fitnesses[algorithm].values())
            stdev = statistics.stdev(experiment_ts_fitnesses[algorithm].values())
            if algorithm != wok_exp:
                pval = stats.wilcoxon(list(experiment_ts_fitnesses[algorithm].values()), list(experiment_ts_fitnesses[wok_exp].values()))[1]
            else:
                pval = '--'
            # if 'writeknow' in algorithm:
            #     continue

            if 'wok' in algorithm:
                algorithm = 'Without Transfer'
            elif 'writeknow' in algorithm:
                continue
            else:
                algorithm = algorithm.split('-wk-')[-1]
                algorithm = algorithm[0].upper() + algorithm[1:]

            csv_file.write(f'{algorithm}, {miin}, {mean}, {stdev}, {pval}\n')
            algorithm.replace('_', '\\_')
            latex_file.write(f'{algorithm} & {miin} & {mean} & {stdev} & {pval}\\\\\n')

        csv_file.close()
        latex_file.write(r'\end{tabular}' + '\n'
                       + r'}' + '\n'
                       + r'\caption{caption}' + '\n'
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
            # Now inside 'experiment_path/run/stats/algorithm'

            if (experiment_path / run / 'stats' / algorithm / 'test').exists():
                # Now inside 'experiment_path/run/stats/algorithm/test'

                (_, _, test_files) = next(os.walk(experiment_path / run / 'stats' / algorithm / 'test'))
                for test_file in test_files:
                    if not test_file.endswith('.csv'):
                        continue
                    get_exp_stats()


    save_exp_stats()                


def plot_best_of_algorithms(experiment_path):
    experiment_path = Path(experiment_path)
    (_, runs, _) = next(os.walk(experiment_path))

    best_run_ind = runs[0]
    best_run_fit = float('inf')

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


                csv_matrix = load_csv(experiment_path / run / 'stats' / algorithm / 'test' / test_file)
                ts_fit = float(csv_matrix[-1][7])


if __name__ == '__main__':
    # process_grid_output(dirpath)
    # for exp in experiments:
    #     # plot_grid_output(dirbase / exp)
    #     get_experiment_stats(dirbase / exp)

    get_experiment_stats('/home/mazhar/grid/gdb1-v5-to4/')
