from pathlib import Path
import re
import os

experiments = [
                            'gdb1.vs5.gdb1.vt4:gen_50',
                            'gdb1.vs5.gdb1.vt6:gen_50',
                            'gdb2.vs6.gdb2.vt5:gen_50',
                            'gdb2.vs6.gdb2.vt7:gen_50',
                            'gdb3.vs5.gdb3.vt4:gen_50',
                            'gdb3.vs5.gdb3.vt6:gen_50',
                            'gdb4.vs4.gdb4.vt3:gen_50',
                            'gdb4.vs4.gdb4.vt5:gen_50',
                            'gdb5.vs6.gdb5.vt5:gen_50',
]

dirbase = Path('/home/mazhar/grid/')


def delete_alg_files(experiment_path, alg, file_name, runs_to_exclude=range(1, 6)):
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
            file_path = run_path / 'pop'
            for i in range(0, 51):
                if (file_path / f'InterimPop.{i}.csv').is_file(): 
                    os.remove(file_path / f'InterimPop.{i}.csv')
                    os.remove(file_path / f'Pop.{i}.csv')
                    print(file_path / f'InterimPop.{i}.csv', 'deleted.')
            # (_, folders, files) = next(os.walk(run_path))
            # for file in files:
            #     if re.search(file_name, file.lower()):
            #         print(run_path / file)
            #         os.remove(run_path / file)

if __name__ == "__main__":
    for exp in experiments:
        delete_alg_files(dirbase / exp, 'Surrogate:initsurpool_true:tp_0:surupol_AddOncePhenotypic', '')
        delete_alg_files(dirbase / exp, 'Surrogate:initsurpool_true:tp_0.1:surupol_AddOncePhenotypic', '')