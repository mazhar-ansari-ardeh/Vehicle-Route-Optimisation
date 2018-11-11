import os
import re
import statistics
from scipy import stats
from pathlib import Path

dirpath = '/vol/grid-solar/sgeusers/mazhar/gdb21-v6-to7/'
# filepath = '/vol/grid-solar/sgeusers/mazhar/gdb21-v6-to7/gdb21-v6to7.sh.o3465564.30'

generations = 100
def processfile(path):
    fitnesses = []
    with open(path) as file:
        for line in file:
            if not line.startswith('Generation'):
                continue

            fitnesses.append(float(line.split(" = ")[-1]))

    wo = fitnesses[100:200]
    wi = fitnesses[200:300]

    # print(min(wo), min(wi))
    return [wo[-1], min(wo), wi[-1], min(wi)]

def getColumn(resutls, index):
    col = []
    for ind in resutls.keys():
        col.append(resutls[ind][index])

    return col


def processexperiment(path):
    w = os.walk(path)
    files = next(w)[-1]
    files = sorted(files)
    results = {}

    for file in files:
        if re.search(r'e\d+', file):
            continue

        results[int(file.split('.')[-1])] = processfile(path + file)

    out = open(dirpath + 'summary.csv', 'w')
    out.write('run, without - last, without - best, with - last, with - best\n')
    for ind in sorted(results.keys()):
        line = ','.join(map(str, [ind, *results[ind]]))
        out.write(line + '\n')

    wo_last_col = getColumn(results, 0)
    wo_best_col = getColumn(results, 1)
    wi_last_col = getColumn(results, 2)
    wi_best_col = getColumn(results, 3)

    wo_last, wo_best, wi_last, wi_best = map(min, [wo_last_col, wo_best_col, wi_last_col, wi_best_col])
    out.write(','.join(map(str, ['min', wo_last, wo_best, wi_last, wi_best])) + '\n')
    
    wo_last_mean, wo_best_mean, wi_last_mean, wi_best_mean = map(statistics.mean, [wo_last_col, wo_best_col, wi_last_col, wi_best_col])
    out.write(','.join(map(str, ['mean', wo_last_mean, wo_best_mean, wi_last_mean, wi_best_mean])) + '\n')
    
    wo_last_std, wo_best_std, wi_last_std, wi_best_std = map(statistics.stdev, [wo_last_col, wo_best_col, wi_last_col, wi_best_col])
    out.write(','.join(map(str, ['std', wo_last_std, wo_best_std, wi_last_std, wi_best_std ])) + '\n')

    wilcox_last = stats.wilcoxon(wo_last_col, wi_last_col)
    wilcox_best = stats.wilcoxon(wo_best_col, wi_best_col)
    out.write(','.join(map(str, ['wilcox', '',  wilcox_last, '', wilcox_best])) + '\n')

    out.close()

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

def process_grid_output(path):
    path = Path(path)
    (_, folders, _) = next(os.walk(path))

    experiment_tr_fitnesses = {}
    experiment_ts_fitnesses = {}
    for run in folders:
        if run.isnumeric() == False:
            continue
        
        (_, experiments, _) = next(os.walk(path / run / 'stats'))
        for exp in experiments:
            if not exp in experiment_tr_fitnesses:
                experiment_tr_fitnesses[exp] = {}
            if not exp in experiment_ts_fitnesses: 
                experiment_ts_fitnesses[exp] = {}
            if not (path / run / 'stats' / exp / 'test').exists():
                continue 

            (_, _, files) = next(os.walk(path / run / 'stats' / exp / 'test'))
            for file in files: 
                if not file.endswith('.csv'):
                    continue

                tr_fit, ts_fit = process_csv(path / run / 'stats' / exp / 'test' / file)
                experiment_tr_fitnesses[exp][run] = tr_fit
                experiment_ts_fitnesses[exp][run] = ts_fit
                break

    for exp in experiment_ts_fitnesses:
        print(exp + ':')
        run_values = experiment_ts_fitnesses[exp].values()
        min_val = min(run_values)
        for ind in experiment_ts_fitnesses[exp].keys():
            if min_val == experiment_ts_fitnesses[exp][ind]:
                min_ind = ind
                break
        print('\tmin:\t ', min_ind, ':', min(experiment_ts_fitnesses[exp].values()))
        print('\tmean:\t ', statistics.mean(experiment_ts_fitnesses[exp].values()))
        print('\tstdev:\t ', statistics.stdev(experiment_ts_fitnesses[exp].values()))



if __name__ == '__main__':
    process_grid_output(dirpath)