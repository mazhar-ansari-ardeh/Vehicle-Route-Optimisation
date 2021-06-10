import re
import os
from pathlib import Path
import pandas as pd
from glob import glob
from typing import Dict
from typing import List, Union


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
    # algorithm = name_map.get(algorithm, algorithm)
    for name_from, name_to in name_map.items():
        algorithm = algorithm.replace(name_from, name_to)
    return algorithm


def rename_exp(exp):
    exp = re.sub('gdb', 'G', exp)
    exp = re.sub(r'\.vs', '-', exp)
    exp = re.sub(r'\.G', 'vG', exp)
    exp = re.sub(r':gen_50', '', exp)
    exp = re.sub(r'\.vt', '-', exp)
    return exp


def atof(text):
    try:
        retval = float(text)
    except ValueError:
        retval = text
    return retval


def natural_keys(text):
    """
    alist.sort(key=natural_keys) sorts in human order
    http://nedbatchelder.com/blog/200712/human_sorting.html
    (See Toothy's implementation in the comments)
    float regex comes from https://stackoverflow.com/a/12643073/190597
    """
    return [atof(c) for c in re.split(r'[+-]?([0-9]+(?:[.][0-9]*)?|[.][0-9]+)', text)]


def natural_sort(iterable):
    return sorted(iterable, key=natural_keys)


def get_test_fitness(experiment_path, inclusion_filter, exclusion_filter, *, num_generations, rename_map):
    # A multi-dimensional dictionary that contains all the test fitness values of all the algorithms 
    # for all the runs:
    # test_fitness['FrequentSub'][1][2] will return the test fitness on run=2 of gen=1 of the algorithm='FreqSub'
    # The data is read from the first CSV file that is found in the 'test' directory. 
    test_fitness = {}
    best_fitness = {}

    def update_test_fitness(file, algorithm, run):
        nonlocal test_fitness
        nonlocal best_fitness
        ren_alg = rename_alg(algorithm, rename_map)

        try:
            csv = pd.read_csv(file)
            if ren_alg not in test_fitness:
                test_fitness[ren_alg] = {}
            if ren_alg not in best_fitness:
                best_fitness[ren_alg] = {}
                best_fitness[ren_alg][-1] = {}
            best_fitness[ren_alg][-1][run] = csv.TestFitness.min()
            for gen in range(num_generations):
                if gen not in test_fitness[ren_alg]:
                    test_fitness[ren_alg][gen] = {}
                if csv.shape[0] - 1 <= gen and ren_alg != 'EDASLS':  # -1 is for the header
                    print("Warning: The csv file does not contain generation:", gen)
                    return False
                test_fitness[ren_alg][gen][int(run)] = float(csv.iloc[gen]['TestFitness'])
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
            if int(
                    run) > 30:  # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not be considered at all
                continue
            test_dir = experiment_path / algorithm / run / 'test'
            if not (test_dir).exists():
                print('Warning: the test folder does not exist on: ', experiment_path / algorithm / run)
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

    return test_fitness, best_fitness


def get_train_stat(experiment_path, inclusion_filter, exclusion_filter, *, num_generations=50) -> dict:
    """
    Given a path to an experiment, the function will consider all the algorithms inside the path
    that match the filters reads the 'job.0.stat.csv' file of all the runs into a DataFrame object. 
    The function returns a dictionary that maps the algorithm name to another dictionary that maps
    runs of the algorithm to the DataFrame of the experiment results for the run. 
    """
    train_stats = {}

    def update_train_stats(file, algorithm, run):
        nonlocal train_stats
        try:
            csv = pd.read_csv(file)
            if not algorithm in train_stats:
                train_stats[algorithm] = {}
                # if not run in mean_train_fitness[algorithm]:
                #     mean_train_fitness[algorithm][run] = {}
            train_stats[algorithm][int(run)] = csv
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
            # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not
            # be considered at all
            if int(run) > 30:
                continue
            train_dir = experiment_path / algorithm / run
            if not train_dir.exists():
                print('Warning: the train folder does not exist on: ', experiment_path / algorithm / run)
                continue
            stat_file = train_dir / 'job.0.stat.csv'

            if not stat_file.exists():
                print('Warning: the stat file does not exist: ', stat_file)
                continue
            if not update_train_stats(stat_file, algorithm, run):
                print("Warning: Something is wrong with the file: ", str(stat_file))

    return train_stats


def get_test_stat(experiment_path, inclusion_filter, exclusion_filter, *, num_generations=50) -> dict:
    """
    Given a path to an experiment, the function will consider all the algorithms inside the path
    that match the filters reads the 'job.0.stat.csv' file of all the runs into a DataFrame object.
    The function returns a dictionary that maps the algorithm name to another dictionary that maps
    runs of the algorithm to the DataFrame of the experiment results for the run.
    """
    test_stats = {}

    def update_stats(train_stat_file, test_stat_file, algorithm, run):
        nonlocal test_stats
        try:
            train_csv = pd.read_csv(train_stat_file)
            test_csv = pd.read_csv(test_stat_file)
            test_csv['ProgSizeMean'] = train_csv.ProgSizeMean
            test_csv['ProgSizeStd'] = train_csv.ProgSizeStd
            if 'TestTime' not in test_csv.columns:
                test_csv['TestTime'] = 0
            test_csv['GlobalTime'] = test_csv.Time + test_csv.TestTime
            if not algorithm in test_stats:
                test_stats[algorithm] = {}
                # if not run in mean_train_fitness[algorithm]:
                #     mean_train_fitness[algorithm][run] = {}
            test_stats[algorithm][int(run)] = test_csv
            return True
        except Exception as exp:
            print(exp)
            print(test_stat_file)
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
            # Runs greater than 30 are ignored because they are done to compensate for lost grid jobs and should not
            # be considered at all
            if int(run) > 30:
                continue

            train_dir = experiment_path / algorithm / run
            if not train_dir.exists():
                print('Warning: the train folder does not exist on: ', experiment_path / algorithm / run)
                continue
            train_stat_file = train_dir / 'job.0.stat.csv'

            test_dir = experiment_path / algorithm / run / 'test'
            if not test_dir.exists():
                print('Warning: the test folder does not exist on: ', experiment_path / algorithm / run)
                continue

            # stat_file = test_dir / 'job.0.stat.csv'
            csv_files = glob(str(test_dir / 'timed*.csvt'))
            if not csv_files:
                csv_files = glob(str(test_dir / '*.csv'))
            if not csv_files:
                print('Warning: the stat file does not exist')
                continue
            test_stat_file = Path(csv_files[0])

            if not test_stat_file.exists():
                print('Warning: the stat file does not exist: ', test_stat_file)
                continue
            if not update_stats(train_stat_file, test_stat_file, algorithm, run):
                print("Warning: Something is wrong with the file: ", str(test_stat_file))

    return test_stats


def get_train_fitness(experiment_path, inclusion_filter, exclusion_filter, *, num_generations=50):
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
            if int(
                    run) > 30:  # Runs greater than 30 are ignored because they are done to compensate for lost grid
                                # jobs and should not be considered at all
                continue
            train_dir = experiment_path / algorithm / run
            if not train_dir.exists():
                print('Warning: the train folder does not exist on: ', experiment_path / algorithm / run)
                continue
            stat_file = train_dir / 'job.0.stat.csv'

            if not (stat_file).exists():
                print('Warning: the stat file does not exist: ', stat_file)
                continue
            if not update_mean_train_fitness(stat_file, algorithm, run):
                print("Warning: Something is wrong with the file: ", str(stat_file))

    return mean_train_fitness


def find_failed(basedir, experiments, to_find, num_generations=50):
    """
    Finds the failed runs of experiments given in the list argument 'experiments'. The function 
    considers only the given algorithm and nothing else. The parameter 'algorithm' refers to a single
    algorithm and and is not expected to be a regex. 
    This function can have an edge over 'find_all_failed' in the regard that this function can detect
    the algorithm is missing entirely while the other function will miss this situation. 
    Usage: find_failed(dirbase, experiments, 'WithoutKnowledge')
    """
    for exp in experiments:
        experiment_path = Path(basedir) / exp
        try:
            (_, algorithms, _) = next(os.walk(experiment_path))
        except StopIteration:
            print(exp, 'is missing')
            continue
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
                if csv.shape[0] - 1 < num_generations:  # -1 is for the header
                    print(exp, to_find, str(i), 'test file does not contain', num_generations,
                          'generation', csv.shape[0])
            if not csv_found:
                print(exp, to_find, str(i), 'test file does not contain any CSVs')
                continue


def find_all_failed(basedir, experiments, inclusion_filter, exclusion_filter, num_generations=50):
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
                    if csv.shape[0] - 1 < num_generations:  # -1 is for the header
                        print(exp, algorithm, str(i), 'test file does not contain', num_generations,
                              'generation', csv.shape[0])
                if not csv_found:
                    print(exp, algorithm, str(i), 'test file does not contain any CSVs')
                    continue


def list_algorithms(basedir: Union[Path, str], experiments: List[str], alg_base: str, rename_map: Dict[str, str]) -> \
        List[Dict[str, str]]:
    """
    Lists all the avaible results for algorithms that belong to a family of algorithms, specified by alg_base.
    :param basedir: The base directory of experiment results.
    :param experiments: The experiments to consider.
    :param alg_base: The family of algorithms to look for.
    :param rename_map: The renaming map to use for renaming the found algorithms.
    :return: A dictionary that maps the renamed found algorithms to their corresponding renamed experiments.
    """
    retval = {}
    basedir = Path(basedir)
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(basedir / exp))
        for algorithm in algorithms:
            if alg_base not in algorithm:
                continue

            ren_alg = rename_alg(algorithm, rename_map)
            if ren_alg not in retval:
                retval[ren_alg] = []
            retval[ren_alg].append(rename_exp(exp))
    return sorted(retval)
