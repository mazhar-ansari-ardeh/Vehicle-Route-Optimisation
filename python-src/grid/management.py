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

def delete_exp(*, experiments, dirbase='/home/mazhar/grid/', save_to='/local/scratch/'):
    """
    Deletes the given experiments and all algorithms inside them. The function will create a zipped
    backup to 'save_to' if it is not 'None'. 
    Usage: delete_exp(experiments=experiments, save_to = '/home/mazhar/')
    """
    dirbase = Path(dirbase)
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
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


def archive_alg(*, experiments, algorithm_to_archive, dirbase='/home/mazhar/grid/', save_to='/local/scratch/'):
    """
    Archives all runs of an algorithm. The function will create a backup to the directory 'save_to' if that directory 
    does not contain an archive file.
    The algorithm parameter can be a regular expression and all algorithms matching the expression 
    will be archived.
    Usage: archive_alg(experiments=experiments, algorithm_to_archive='KnowledgeSource', save_to='/home/mazhar/')
    """
    dirbase = Path(dirbase)
    for exp in experiments:
        (_, algorithms, _) = next(os.walk(dirbase / exp))
        for algorithm in algorithms:
            if (not re.search(algorithm_to_archive, algorithm)): # and (not re.search(r'PPTPipe', algorithm)):
                continue
            (_, runs, _) = next(os.walk(dirbase / exp / algorithm))
            for run in runs: 
                if (not (Path(save_to) / exp / algorithm / (run + ".tar.bz2")).is_file()):
                    print(dirbase / exp / algorithm / run, ' is not backed up. Backing it up now.')
                    shutil.make_archive(Path(save_to) / exp / algorithm / run, 'bztar', dirbase / exp / algorithm / run)
                    print(dirbase / exp / algorithm / run, ' backed up.')
            print(dirbase / exp / algorithm, 'archived.')

def delete_alg(*, experiments, algorithm_to_delete, dirbase='/home/mazhar/grid/', save_to='/local/scratch/'):
    """
    Deletes all runs of an algorithm. The function will create a backup to 'save_to' if it is not 'None'.
    The algorithm parameter can be a regular expression and all algorithms matching the expression 
    will be deleted.
    Usage: delete_alg(experiments=experiments, algorithm_to_delete='KnowledgeSource', save_to='/home/mazhar/')
           delete_alg(experiments=experiments, algortithm_to_delete='Surrogate:initsurpool_true:tp_0:surupol_UnboundedPhenotypic$', save_to='')
    """
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

def unarchive(experiments, algorithm, dir_from, dir_to):
    for exp in experiments:
        alg_dir = Path(dir_from) / exp / algorithm
        print(alg_dir)
        try:
            (_, _, files) = next(os.walk(alg_dir))
        except StopIteration as e:
            print('Failed to process', e, algorithm)
            print('Exception:', str(e))
            continue
        # print(files)
        for file in sorted(files):
            if not file.endswith('.tar.bz2'):
                continue
            save_to = Path(dir_to) / exp / algorithm / file.replace('.tar.bz2', '')
            shutil.unpack_archive(alg_dir / file, save_to)
            print('Unpacked', alg_dir / file, 'to', save_to)

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
