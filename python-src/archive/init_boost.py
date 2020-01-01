from pathlib import Path
import os
import statistics
import csv 


def csvtest():
    path = '/home/mazhar/grid/gdb1-v5-to4/1/stats/gdb1-v5-to-4-wk-subcontrib-gen49-49-all/test/total-cost-gdb1-4-0.2-0.2.csv'
    with open(path, mode='r') as csv_file:
        csv_reader = csv.DictReader(csv_file)
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                print(f'Column names are {", ".join(row)}')
                line_count += 1
            print(f'\tGen: {row["Generation"]}: Train: {row["TrainFitness"]}, test: {row["TestFitness"]}.')
            line_count += 1
        print(f'Processed {line_count} lines.')

generations = 50

def load_csv(file, comma=','):
    
    csv_matrix = []
    for line in open(file):
        line = line.strip('\n')
        items = line.split(comma)
        items = map(lambda item: item.strip('\t').strip(' '), items)
        csv_matrix.append(list(items))

    return csv_matrix

def sort_algorithms(algorithm):
    algorithm = sorted(algorithm)
    for i in range(len(algorithm)):
        if 'wok' in algorithm[i]:
            temp = algorithm[0]
            algorithm[0] = algorithm[i]
            algorithm[i] = temp
        if 'TLGPC' in algorithm[i]:
            temp = algorithm[i]
            algorithm.remove(algorithm[i])
            algorithm.append(temp)
        if 'all' in algorithm[i]:
            for j in range(i, len(algorithm)):
                if 'first' in algorithm[j]:
                    temp = algorithm[i]
                    algorithm[i] = algorithm[j]
                    algorithm[j] = temp
        
    return algorithm

# def should_process(alg):
#     for f in filter:
#         if f.lower() in alg.lower():
#             return False
#     return True


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

    wok = 'gdb1-v6-wok'
    improv = {}
    for algorithm in gen_mean_ts:
        if 'subcontrib' not in algorithm:
            continue

        improv[algorithm] = 0
        for gen in gen_mean_ts[algorithm] :
            if statistics.mean(gen_mean_ts[algorithm][gen].values()) <= statistics.mean(gen_mean_ts[wok][gen].values()):
                improv[algorithm] += 1
            else:
                break



    print('Finished plotting')

plot_grid_output('/home/mazhar/grid/gdb1-v5-to6/')