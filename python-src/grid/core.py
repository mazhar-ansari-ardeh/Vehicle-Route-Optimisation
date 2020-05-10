import re 
import os
from pathlib import Path
import pandas as pd

def should_process(alg, inclusion_filter, exclusion_filter):
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

def sort_algorithms(algorithm, *, base_line='WithoutKnowledge'):
    algorithm = sorted(algorithm, key=lambda s: s.lower())
    for i in range(len(algorithm)):
        if base_line in algorithm[i]:
            temp = algorithm.pop(i)
            algorithm.insert(0, temp)
            break
       
    return algorithm

def rename_alg(algorithm, name_map):
    algorithm = name_map.get(algorithm, algorithm)
    # for name_from, name_to in name_map.items():
    #     if algorithm == name_from:
    #         algorithm = name_to
    return algorithm

def rename_alg_zzz(algorithm):
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

def get_test_fitness(experiment_path, inclusion_filter, exclusion_filter, *, num_generations):
    # A multi-dimensional dictionary that contains all the test fitness values of all the algorithms 
    # for all the runs:
    # test_fitness['FrequentSub'][1][2] will return the test fitness on run=2 of gen=1 of the algorithm='FreqSub'
    # The data is read from the first CSV file that is found in the 'test' directory. 
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
                    return False
                test_fitness[algorithm][gen][int(run)] = float(csv.iloc[gen]['TestFitness'])
            return True
        except Exception as exp:
            print(exp)
            print(file)
            # print(algorithm, run)
            return False
            # raise exp

    experiment_path = Path(experiment_path)
    (_, algorithms, _) = next(os.walk(experiment_path))
    for algorithm in algorithms:
        if not should_process(algorithm, inclusion_filter, exclusion_filter):
            continue
        (_, runs, _) = next(os.walk(experiment_path / algorithm))
        if len(runs) < 30:
            print('Warning: number of runs is less than 30 for ', algorithm, f': ({len(runs)}).')
        for run in runs:
            if int(run) > 30: # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not be considered at all
                continue
            test_dir = experiment_path / algorithm / run / 'test'
            if not (test_dir).exists():
                print('Warning: the test folder does not exist on: ', experiment_path/algorithm/run)
                continue
            (_, _, test_files) = next(os.walk(test_dir))
            for test_file in test_files:
                if not test_files:
                    print("Warning: Test file does not exist in: ", test_dir)
                if not test_file.endswith('.csv'):
                    continue
                if not update_test_fitness(test_dir / test_file, algorithm, run):
                    print("Warning: Something is wrong with the test file: ", test_dir)
                    # continue

    return test_fitness

def get_train_mean(experiment_path, inclusion_filter, exclusion_filter, *, num_generations=50):
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
            for gen in range(num_generations):
                if not gen in mean_train_fitness[algorithm]:
                    mean_train_fitness[algorithm][gen] = {}
                mean_train_fitness[algorithm][gen][int(run)] = float(csv.iloc[gen]['FitMean'])
            return True
        except Exception as exp:
            print(exp)
            print(file)
            return False
            # print(algorithm, run)
            # raise exp

    experiment_path = Path(experiment_path)
    (_, algorithms, _) = next(os.walk(experiment_path))
    for algorithm in algorithms:
        if not should_process(algorithm, inclusion_filter, exclusion_filter):
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
            if not update_mean_train_fitness(stat_file, algorithm, run):
                print("Warning: Something is wrong with the file: ", str(stat_file))

    return mean_train_fitness

def find_failed(basedir, experiments, to_find, num_generations = 50):
    """
    Finds the failed runs of experiments given in the list argument 'experiments'. The function 
    considers only the given algorithm and nothing else. The parameter 'algorithm' refers to a single
    algorithm and and is not expected to be a regex. 
    This function can have an edge over 'find_all_failed' in the regard that this function can detect
    the algorithm is missing entirely while the other function will miss this situation. 
    Usage: find_failed(dirbase, experiments, 'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset')
    """
    for exp in experiments:
        experiment_path = Path(basedir) / exp
        (_, algorithms, _) = next(os.walk(experiment_path))
        if not to_find in algorithms:
            print(exp, to_find, 'is missing')
            continue
        (_, runs, _) = next(os.walk(experiment_path / to_find))
        for i in range(1, 31):
            if not str(i) in runs:
                print(exp, to_find, str(i), 'is missing')
                continue
            test_dir = experiment_path / to_find / str(i) / 'test'
            if not (test_dir).is_dir():
                print(exp, to_find, str(i), 'test folder is missing')
                continue
            (_, _, test_files) = next(os.walk(test_dir))
            if not test_files:
                    print(exp, to_find, str(i), 'test folder is empty')
            csv_found = False
            for test_file in test_files:
                if not test_file.endswith('.csv'):
                    continue
                csv_found = True
                csv = pd.read_csv(test_dir / test_file)
                if csv.shape[0] - 1 < num_generations : # -1 is for the header
                    print(exp, to_find, str(i), 'test file does not contain', num_generations, 
                        'generation', csv.shape[0])
            if not csv_found:
                print(exp, to_find, str(i), 'test file does not contain any CSVs')
                continue

def find_all_failed(basedir, experiments, inclusion_filter, exclusion_filter, num_generations = 50):
    """
    Finds the failed runs of experiments given in the list argument 'experiments'. The function 
    considers the set of algorithms given by the parameter 'algorithm' which can be a regex. 
    The 'find_failed' function can have an edge over this one in the regard that that function can 
    detect if an algorithm is missing entirely while this function will miss this situation. 
    Usage: find_all_failed(dirbase, experiments, inclusion_filter, exclusion_filter)
    """
    for exp in experiments:
        experiment_path = Path(basedir) / exp
        (_, algorithms, _) = next(os.walk(experiment_path))
        for algorithm in algorithms:
            if not should_process(algorithm, inclusion_filter, exclusion_filter):
                continue
            (_, runs, _) = next(os.walk(experiment_path / algorithm))
            for i in range(1, 31):
                if not str(i) in runs:
                    print(exp, algorithm, str(i), 'is missing')
                    continue
                test_dir = experiment_path / algorithm / str(i) / 'test'
                if not (test_dir).is_dir():
                    print(exp, algorithm, str(i), 'test folder is missing')
                    continue
                (_, _, test_files) = next(os.walk(test_dir))
                if not test_files:
                        print(exp, algorithm, str(i), 'test folder is empty')
                csv_found = False
                for test_file in test_files:
                    if not test_file.endswith('.csv'):
                        continue
                    csv_found = True
                    if (test_dir / test_file).stat().st_size == 0:
                        print(exp, algorithm, str(i), 'test file is empty:', test_file)
                        continue
                    csv = pd.read_csv(test_dir / test_file)
                    if csv.shape[0] - 1 < num_generations : # -1 is for the header
                        print(exp, algorithm, str(i), 'test file does not contain', num_generations, 
                            'generation', csv.shape[0])
                if not csv_found:
                    print(exp, algorithm, str(i), 'test file does not contain any CSVs')
                    continue
