import os
import shutil
from pathlib import Path
import re

def compress_grid(*, experiments, dirbase, save_to='/local/scratch/'):
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            if (Path(save_to) / exp / (algorithm + ".tar.bz2")).is_file():
                continue
            shutil.make_archive(Path(save_to) / exp / algorithm, 'bztar', Path(dirbase) / exp / algorithm)
            print(Path(save_to) / exp / algorithm, 'finished.')


def delete_alg(*, experiments, algorithm_to_delete, dirbase='/home/mazhar/grid/', save_to='/local/scratch/'):
    dirbase = Path(dirbase)
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            if (not re.search(algorithm_to_delete, algorithm)): # and (not re.search(r'PPTPipe', algorithm)):
                continue
            if save_to:
                (_, runs, _) = next(os.walk(dirbase / exp / algorithm))
                for run in runs: 
                    if (not (Path(save_to) / exp / algorithm / (run + ".tar.bz2")).is_file()):
                        print(dirbase / exp / algorithm / run, ' is not backed up. Backing it up now.')
                        shutil.make_archive(Path(save_to) / exp / algorithm / run, 'bztar', dirbase / exp / algorithm / run)
                        print(dirbase / exp / algorithm / run, ' backed up.')
                        
                        shutil.rmtree(dirbase / exp / algorithm / run)
                        print(dirbase / exp / algorithm / run, ' deleted.')
            shutil.rmtree(dirbase / exp / algorithm)
            print(dirbase / exp / algorithm, 'deleted.')

def delete_alg_files(experiment_path, alg, file_name, runs_to_exclude=range(1, 5)):
    """
    Deletes specified files in given experiments to save space. 
    This function does not create any backups of the files. 
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
            if int(run) in runs_to_exclude:
                continue
            run_path = Path((experiment_path / algorithm) / run)
            (_, _, files) = next(os.walk(run_path))
            for file in files:
                if re.search(file_name, file.lower()):
                    print(run_path / file)
                    os.remove(run_path / file)

def rename_alg_folder(*, experiments, dirbase, rename_from, rename_to):
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            # if not re.search(rename_from, algorithm):
            if rename_from != algorithm:
                continue
            os.rename(Path(dirbase) / exp / rename_from, Path(dirbase) / exp / rename_to )
            print('Renamed', Path(dirbase) / exp / rename_from, 'to', Path(dirbase) / exp / rename_to)
