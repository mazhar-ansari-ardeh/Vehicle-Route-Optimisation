'''
Created on 17/09/2018

@author: mazhar
'''

from pathlib import Path
import os # For chdir
import subprocess # For call
import matplotlib.pyplot as plt
import statistics


# The base that holds all the files,
stat_base_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/stats'

java_bin_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/bin/'

knowledge_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/'

param_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/'

fittedknowl_stat_dir = 'withfittedknowledge.stat'
filteredfittedknowl_stat_dir = 'withfilteredfittedknowledge.stat'
without_kwnol_stat_dir = 'withoutknowledge.stat'

generations = 100
knowledge_probability = 0.5
knowledge_filter_size = 100
knowledge_tournament_size = 15
knowledgebaed_builder = ...

# The number of times that each experiment will be run.
no_runs = 10

run_ecj = False

show_ecj_output = False

param_template = """
parent.0 = {java_bin_dir}ec/gp/koza/koza.params

# Override the number of generations
generations = {generations}

# Originally, this parameter was inherited from ec.simple.simple.params via ec.gp.koza.koza.params
# finish = ec.simple.SimpleFinisher
# finish = tutorial7.FilePopSaverFinisher
# finish.final-pop-file-name = final-pop.dat

# --------------------------- Knowledge ---------------------------------------------
# Originally it was gp.tc.0.init = ec.gp.koza.HalfBuilder
{enable_knowledge} gp.tc.0.init = {knowledgebaed_builder}
{enable_knowledge} gp.tc.0.init.knowledge-file = {knowledge_dir}final-pop.dat
{enable_knowledge} gp.tc.0.init.knowledge-probability = {knowledge_probability}
{enable_knowledge} gp.tc.0.init.knowledge-tournament-size = {knowledge_tournament_size}
{enable_knowledge} gp.tc.0.init.knowledge-filter-size = {knowledge_filter_size}

# --------------------------- EvolutionState ----------------------------------------
# I need to find fitness of inds after initialization and this one does that!
# I will add more modifications to this if needed.
# state = tutorial7.gp.CFCEvolutionState


# --------------------------- Functions and Nodes -----------------------------------

# We have one function set, of class GPFunctionSet
gp.fs.size = 1
gp.fs.0 = ec.gp.GPFunctionSet
# We'll call the function set "f0".
gp.fs.0.name = f0

# We have four functions in the function set. They are:

gp.fs.0.size = 5
gp.fs.0.func.0 = tutorial7.problems.regression.Add
gp.fs.0.func.0.nc = nc2
gp.fs.0.func.1 = tutorial7.problems.regression.Sub
gp.fs.0.func.1.nc = nc2
gp.fs.0.func.2 = tutorial7.problems.regression.Mul
gp.fs.0.func.2.nc = nc2
gp.fs.0.func.3 = tutorial7.problems.regression.RegERC
gp.fs.0.func.3.nc = nc0
gp.fs.0.func.4 = tutorial7.problems.regression.X
gp.fs.0.func.4.nc = nc0

# --------------------------- Problem Definition ------------------------------------
eval.problem = tutorial7.problems.regression.one_d.Poly1
eval.problem.data = tutorial7.problems.regression.VectorData

eval.problem.poly1.range_min = -2
eval.problem.poly1.range_max = +2
eval.problem.poly1.number-of-tests = 100
eval.clone-problem = false

# --------------------------- Statistics --------------------------------------------
stat = tutorial7.gp.FCFStatistics
stat.file = ${stat_dir}/poly1.stat
stat.gen-pop-file = {stat_dir}/population.gen
stat.save-pop = true
stat.save-tree = true

gp.tree.print-style=c
"""


def createParamFile(param_file_name, stat_path, enable_knowledge = True):
    params = param_template.format(java_bin_dir = java_bin_dir,
                                       generations = generations,
                                       knowledge_dir = knowledge_dir,
                                       stat_dir = str(stat_path),
                                       knowledge_probability = knowledge_probability,
                                       knowledge_tournament_size = knowledge_tournament_size,
                                       knowledge_filter_size = knowledge_filter_size,
                                       enable_knowledge = '' if enable_knowledge else '#')

    file = open(param_dir + param_file_name, "w")
    file.write(params)
    file.close()
#     print(params)

    return param_dir + param_file_name


def create_dir(directory, mode = 0o777, parents = True, exists_ok = False):
    if directory.exists() and directory.is_dir():
        pass # print(f'The {str(directory)} exists. Files may get mixed up or overwritten.')
    else:
        directory.mkdir(mode, parents, exists_ok)


def readRunFitnessFromDir(path):

    best = []
    for line in open(path / 'population.gen.statistics'):
        if line.startswith('#'):
            continue
        best.append(float(line.split(',')[-1].strip(' ').strip('\n').strip('\t')))

    return best

def getBestOfEachRun(runFitnesses):
    """
    Receives a dictionary of run indexes and the fitness of generations corresponding
    runs and returns a dictionary of run indexes and the best fitness of
    corresponding runs.
    """
    bestFitnesses = {}
    for ind in runFitnesses:
        bestFitnesses[ind] = min(runFitnesses[ind])

    return bestFitnesses;

def findBestRun(runFitnesses):
    """
    Finds the best run that has the best fitness value and returns a tuple that
    contains the index of the run and the best fitness value.
    The function expects to receive a dictionary whose keys are indexes of runs and
    and values of fitness values of the corresponding run.
    """

    if len(runFitnesses) == 0:
        return ()

    minRunInd, minFitness = next(iter(runFitnesses.items()))
    minFitness = min(minFitness)

    for runInd in runFitnesses:
        if min(runFitnesses[runInd]) < minFitness:
            minFitness = min(runFitnesses[runInd])
            minRunInd = runInd

    return minRunInd, minFitness


def getFitnessStats(runFitnesses):
    if len(runFitnesses) == 0:
        return ()

    bestFitnesses = getBestOfEachRun(runFitnesses)
    mean = statistics.mean(bestFitnesses.values())
    std = statistics.stdev(bestFitnesses.values())

    return mean, std

if __name__ == '__main__':

    os.chdir(java_bin_dir)
    print('Current working directory is: ', os.getcwd())
    stat_base_path = Path(stat_base_dir)

    withFittedknowl_runFitnesses = {}
    withFilteredFittedknowl_runFitnesses = {}
    withoutknowl_runFitnesses = {}

    for i in range(no_runs):

        withfittedknowl_stat_path = stat_base_path / (fittedknowl_stat_dir + '.' + str(i))
        create_dir(withfittedknowl_stat_path )

        withfilteredknowl_stat_path = stat_base_path / (filteredfittedknowl_stat_dir + '.' + str(i))
        create_dir(withfilteredknowl_stat_path )

        withoutknowl_stat_path = stat_base_path / (without_kwnol_stat_dir + '.' + str(i))
        create_dir(withoutknowl_stat_path)

        if run_ecj:
            param_file_name = f"fitted-knowledge-experiment.{i}.param"
            createParamFile(param_file_name, withfittedknowl_stat_path ,
                                          enable_knowledge = True)
            print('Running part 1 of experiment with knowledge ' + str(i) + ". ")
            subprocess.call(['java', 'ec.Evolve', '-file', param_file_name],
                            stdout = None if show_ecj_output else subprocess.DEVNULL,
                            stderr = None if show_ecj_output else subprocess.DEVNULL)

            param_file_name = f"filtered-knowledge-experiment.{i}.param"

            createParamFile(param_file_name, withfilteredknowl_stat_path ,
                                          enable_knowledge = True)
            print('Running part 2 of experiment with knowledge ' + str(i) + ". ")
            subprocess.call(['java', 'ec.Evolve', '-file', param_file_name],
                            stdout = None if show_ecj_output else subprocess.DEVNULL,
                            stderr = None if show_ecj_output else subprocess.DEVNULL)

            param_file_name = f"without-knowledge-experiment.{i}.param"
            createParamFile(param_file_name, withoutknowl_stat_path,
                                          enable_knowledge = False)
            print('Running part 2 of experiment without knowledge ' + str(i) + ". ")
            subprocess.call(['java', 'ec.Evolve', '-file', param_file_name],
                            stdout = None if show_ecj_output else subprocess.DEVNULL,
                            stderr = None if show_ecj_output else subprocess.DEVNULL)

        withFittedknowl_runFitnesses[i] = readRunFitnessFromDir(withfittedknowl_stat_path)
        print('Best fitness: ', min(withFittedknowl_runFitnesses[i]))

        withFilteredFittedknowl_runFitnesses[i] = readRunFitnessFromDir(withfittedknowl_stat_path)
        print('Best fitness: ', min(withFilteredFittedknowl_runFitnesses[i]))

        withoutknowl_runFitnesses[i] = readRunFitnessFromDir(withoutknowl_stat_path)
        print('Best fitness: ', min(withoutknowl_runFitnesses[i]), '\n')


    bestRun, bestFit = findBestRun(withFittedknowl_runFitnesses)
    meanFit, stdFit = getFitnessStats(withFittedknowl_runFitnesses)
    print(f'Best run of the experiment with knowledge: {bestRun}.',
          f'\nBest fitness: {bestFit}. \nMean fitness of {no_runs} runs: {meanFit}.'
          f'\nStdev: {stdFit}\n')
    plot1, = plt.plot(range(len(withFittedknowl_runFitnesses[bestRun])),
                      withFittedknowl_runFitnesses[bestRun],
                      label = 'with knowledge')

    bestRun, bestFit = findBestRun(withFilteredFittedknowl_runFitnesses)
    meanFit, stdFit = getFitnessStats(withFilteredFittedknowl_runFitnesses)
    print(f'Best run of the experiment with knowledge: {bestRun}.',
          f'\nBest fitness: {bestFit}. \nMean fitness of {no_runs} runs: {meanFit}.'
          f'\nStdev: {stdFit}\n')
    plot1, = plt.plot(range(len(withFilteredFittedknowl_runFitnesses[bestRun])),
                      withFilteredFittedknowl_runFitnesses[bestRun],
                      label = 'with filtered knowledge')

    bestRun, bestFit = findBestRun(withoutknowl_runFitnesses)
    meanFit, stdFit = getFitnessStats(withoutknowl_runFitnesses)
    print(f'Best run of the experiment without knowledge: {bestRun}.',
          f'\nBest fitness: {bestFit}. \nMean fitness of {no_runs} runs: {meanFit}.'
          f'\nStdev: {stdFit}\n')
    plot2, = plt.plot(range(len(withoutknowl_runFitnesses[bestRun])),
             withoutknowl_runFitnesses[bestRun],
             label = 'without knowledge')

    plt.legend([plot1, plot2])
    plt.show()




        #