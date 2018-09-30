'''
Created on 23/09/2018

@author: mazhar
'''

import statistics
import pathlib
import subprocess
import os
import copy
from abc import ABCMeta, abstractmethod


class Experiment:
    '''
    An instance of this class represents an experiment that needs to be conducted.
    An experiment has a series of inputs that it needs to be able to run and after a
    run, it has a collection of outputs that are the results of the experiment.
    '''

    def __init__(self, name, num_runs=30):
        '''
        Constructs a new experiment instance.
        Parameters:
            name: the name of experiment
            num_runs: the number of times that an experiment needs to be run.
            stat_root_dir: the root of the directory into which statistics of
            experiments will be written to.
        '''

        self.num_runs = num_runs
        self.name = name

        self.run_fitnesses = {}
        '''
        The fitness value of each run of the experiment is saved as dictionary in
        which the key is the run index and the value is fitness of the run.
        '''

    def get_mean(self):
        """
        Gets mean of results of self.num_runs of running an experiment.
        """
        return statistics.mean(self.run_fitnesses.values())

    def get_stdev(self):
        """
        Gets std dev of results of self.num_runs of running an experiment.
        """
        return statistics.stdev(self.run_fitnesses.values())

    def get_best_fitness(self):
        """
        Gets the best fitness value of all runs. Best fitness is the fitness value
        that has the lowest value. Returns a tuple of run index and fitness value.
        """

        if not self.run_fitnesses:
            return ()

        min_run_ind, min_fitness = next(iter(self.run_fitnesses.items()))

        for run_ind in self.run_fitnesses:
            if self.run_fitnesses[run_ind] < min_fitness:
                min_fitness = self.run_fitnesses[run_ind]
                min_run_ind = run_ind

        return min_run_ind, min_fitness

    def run(self, silent=True):
        """
        Instructs the experiment object to start running. This class is not
        asynchronous and blocks until the run of experiment is finished.
        """
        pass


class GPExperiment(Experiment):
    '''
    Encapsulates a Genetic Programming experiment. Genetic Programming is by nature
    a generational algorithm in which it performs an iteration of operations for a
    number of generations. This class captures this feature of the algorithm by adding
    a `get_run_generational_fitness` method that returns the fitness value for each
    generation of the algorithm.
    '''

    def __init__(self, name, num_runs, num_generations):
        Experiment.__init__(self, name, num_runs)

        self.num_generations = num_generations

        self.generation_fitnesses = {}
        """
        Genetic Programming is a generational algorithm in which each run of an
        experiment is comprised of running genetic operators multiple times over a
        number of generations. Each generation produces a fitness value of its own
        (which is the best fitness in the genetic population). This variable holds
        these generational fitness for each generation and each run of the
        experiment in which run index is paired with a list of fitness values for
        each generation of GP. For example, if number of runs of an experiment is 3
        and number of generations of a GP run is 5 then, a typical value of this
        variable may look like:
            self.generation_fitnesses = {
            0 : [2.2, 1.1, 2.1, 2.3, 3.2],
            1 : [1.1, 2.1, 6.3, 4.3, 3.1],
            2 : [1.8, 0.1, 3.3, 4.5, 3.9]
            }

        """

    def get_run_generational_fitness(self, run_index):
        '''
        Returns a list that contains fitness values for all generations of a 
        GP run that is indicated with the run_index parameter. 
        '''
        pass


class ECJExperiment(GPExperiment, metaclass=ABCMeta):
    '''
    Encapsulates an ECJ experiment.
    '''

    java_bin_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/bin/'
    '''
    The directory that contains ECJ implementation.
    '''

    java_library_classpath = '.'
    '''
    If the problem to be solved needs specific libraries, the path to that libraries
    can be specified here. This variable will be passed to Java as it is. The use of
    current directory as a part of classpath is not implied and needs to be added
    manually.
    '''

    stat_root_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/stat/'
    """
    The directory into which ECJ will be instructed to write its statistics. This is
    a default value and class instances may choose to use a different path.
    """

    param_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/'
    """
    The default value for the location of the parameter file that instances of this
    class need to be passed to ECJ. This is a default value and instances can use a
    value of their own.
    """

    show_ecj_output = False
    '''
    If True, the outputs of the EJC framework will be displayed when the experiment
    is run. This is a default value and instances of this class can use a value of
    their own.
    '''

    def __init__(self, name,
                 num_runs=30,
                 num_generations=100,
                 stat_root_dir=stat_root_dir,
                 param_dir=param_dir,
                 show_ecj_output=show_ecj_output,
                 java_library_classpath=java_library_classpath,
                 java_bin_dir=java_bin_dir):

        GPExperiment.__init__(self, name, num_runs, num_generations)
        self.show_ecj_output = show_ecj_output
        self.param_dir = pathlib.Path(param_dir)
        self.stat_root_dir = pathlib.Path(stat_root_dir)
        self.java_library_classpath = java_library_classpath
        self.java_bin_dir = pathlib.Path(java_bin_dir)

        # if not self.stat_root_dir.exists():
        #     self.stat_root_dir.mkdir(0o777, True)

    def get_stat_dir(self, runindex):
        '''
        Returns the directory into which experiment statistics will be written to.
        If the directory does not exist, the function tries to create it and will
        raise an exception if it fails to do so.
        '''
        retval = self.stat_root_dir / (self.name + '.' + str(runindex))
        if not retval.exists():
            retval.mkdir(0o777, True)
        return retval

    def getRunGenerationalFitness(self, runindex):

        gen_fitness = []
        for line in open(self.get_stat_dir(runindex) / 'population.gen.statistics'):
            if line.startswith('#'):
                continue
            gen_fitness.append(
                float(line.split(',')[-1].strip(' ').strip('\n').strip('\t')))

        return gen_fitness

    def reloadFitnesses(self):
        '''
        Reloads fitness statistics for this experiment. When an experiment is run,
        its statistics are loaded meanwhile the experiment is being run but because
        this class saves its statistics to file system, it is possible to load these
        statistics without running the experiment.
        The method returns True if it loads the results successfully and False
        otherwise.
        '''

        for i in range(self.num_runs):
            self.generation_fitnesses[i] = self.getRunGenerationalFitness(i)
            self.run_fitnesses[i] = min(self.generation_fitnesses[i])

        return True

    def __createParamFile(self, run_index):
        if not self.param_dir.exists():
            self.param_dir.mkdir(0o777, True)

        param_file_name = self.param_dir / \
            (self.name + '.' + str(run_index) + ".param")
        file = open(param_file_name, "w")
        file.write(self.get_param_content(run_index))
        file.close()

        return param_file_name

    @abstractmethod
    def get_param_content(self, run_index):
        '''
        Returns the content of the parameter file that ECJ will use to run
        experiment.
        '''
        pass

    def run(self, silent=True):
        '''
        Runs the experiment. This method changes the working directory of the program to
        the java binary folder that is given to its instance object at initialization. However,
        it does not change back to its original directory when it finishes.
        '''

        os.chdir(self.java_bin_dir)

        for i in range(self.num_runs):

            param_file_name = self.__createParamFile(i)
            subprocess.call(['java', '-cp', self.java_library_classpath,
                             'ec.Evolve', '-file', param_file_name],
                            stdout=None if self.show_ecj_output else subprocess.DEVNULL,
                            stderr=None if self.show_ecj_output else subprocess.DEVNULL)

            self.generation_fitnesses[i] = self.getRunGenerationalFitness(i)
            self.run_fitnesses[i] = min(self.generation_fitnesses[i])
            if not silent:
                print(f'Finished run {i} of experiment {self.name}.',
                      f'Fitness: {self.run_fitnesses[i]}', '\n')

        if not silent:
            print(f'Finished {self.num_runs} of experiment {self.name}.',
                  f'Best fitness {self.get_best_fitness()}.',
                  f'Mean of best fitnesses: {self.get_mean()}',
                  f'Stdev of best fitnesses: {self.get_stdev()}' if self.num_runs > 1 else '',
                  '\n', sep='\n')


class KnowledgeableExperiment:
    '''
    A decorator class that adds knowledge resuability to ECJ experiments.
    '''

    FilteredFittedCodeFragmentBuilder = 'tl.gp.FilteredFittedCodeFragmentBuilder'

    FittedCodeFragmentBuilder = 'tl.gp.FittedCodeFragmentBuilder'

    def __init__(self, experiment, builder, knowledge_file, knowledge_probability,
                 tournament_size, filter_size=None):
        self.experiment = copy.deepcopy(experiment)
        self.experiment.generation_fitnesses = {}
        self.experiment.run_fitnesses = {}
        self.builder = builder
        self.knowledge_file = knowledge_file
        self.knowledge_probability = knowledge_probability
        self.tournament_size = tournament_size
        self.filter_size = filter_size
        self.experiment.name = (f'know-{knowledge_probability}-{tournament_size}-{filter_size}-'
                                + experiment.name)
        self.experiment.get_param_content = self.get_param_content

    def get_param_content(self, run_ind):
        '''
        Returns the content of ECJ parameter file.
        '''
        param_content = self.experiment.__class__.get_param_content(
            self.experiment, run_ind)
        param_content += '\n'
        param_content += F'gp.tc.0.init = {self.builder}\n'
        param_content += F'gp.tc.0.init.knowledge-file = {self.knowledge_file}\n'
        param_content += F'gp.tc.0.init.knowledge-probability = {self.knowledge_probability}\n'
        param_content += F'gp.tc.0.init.knowledge-tournament-size = {self.tournament_size}\n'
        if self.filter_size:
            param_content += F'gp.tc.0.init.knowledge-filter-size = {self.filter_size}\n'

        return param_content

    def __getattr__(self, attrname):
        return getattr(self.experiment, attrname)


class UCARPExperiment(ECJExperiment):
    '''
    Implements the experiment run of a UCARP problem based on ECJ.
    '''

    def __init__(self, name, data_set, num_vehicles, num_runs=30,
                 num_generations=100,
                 stat_root_dir=ECJExperiment.stat_root_dir,
                 param_dir=ECJExperiment.param_dir,
                 show_ecj_output=ECJExperiment.show_ecj_output,
                 java_library_classpath=ECJExperiment.java_library_classpath,
                 java_bin_dir=ECJExperiment.java_bin_dir):

        ECJExperiment.__init__(self, name, num_runs,
                               num_generations=num_generations,
                               stat_root_dir=stat_root_dir, param_dir=param_dir,
                               show_ecj_output=show_ecj_output,
                               java_library_classpath=java_library_classpath,
                               java_bin_dir=java_bin_dir)
        self.data_set = data_set
        self.num_vehicles = num_vehicles

    def get_param_content(self, run_index):

        # The template contains the following variables:
        #    data_set: the data set to use. this can contain the path to the dataset.
        #    num_vehicles: number of vehicles.
        #    experiment_name: the name of experiment. this is used for naming files
        #                     and directories of various statistical purposes.
        #    stat_dir: the directory to which statistics must be written.
        #    num_generations: number of generations of GP.
        param_template = '''
# ==============================
# The problem
# ==============================

eval.problem = gphhucarp.gp.ReactiveGPHHProblem
eval.problem.pool-filter = gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter
eval.problem.tie-breaker = gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker
eval.problem.data = gputils.DoubleData
eval.problem.eval-model = gphhucarp.gp.evaluation.ReactiveEvaluationModel
eval.problem.eval-model.objectives = 1
eval.problem.eval-model.objectives.0 = total-cost
eval.problem.eval-model.instances = 1
eval.problem.eval-model.instances.0.file = {data_set}
eval.problem.eval-model.instances.0.samples = 5
eval.problem.eval-model.instances.0.vehicles = {num_vehicles}
eval.problem.eval-model.instances.0.demand-uncertainty-level = 0.2
eval.problem.eval-model.instances.0.cost-uncertainty-level = 0.2

# ==============================
# Statistics
# ==============================
stat = tl.gp.FCFStatistics
stat.file = ${stat_dir}/{experiment_name}.stat
stat.gen-pop-file = {stat_dir}/population.gen
stat.save-pop = true
stat.save-tree = true

gp.tree.print-style=dot


# ==============================
# The GPHH evolution state parameters
# ==============================
terminals-from = extended
include-erc = true
rotate-eval-model = true

#print-unaccessed-params = true
#print-used-params = true

# ==============================
# Administrative parameters
# ==============================
evalthreads = 1
breedthreads = 1
seed.0 = 0
checkpoint = false
checkpoint-modulo = 1
checkpoint-prefix = ec

# ==============================
# Basic evolution parameters
# ==============================
state = gphhucarp.gp.GPHHEvolutionState
finish = tl.gp.FilePopSaverFinisher
finish.final-pop-file-name = final-pop-{experiment_name}.dat
exch = ec.simple.SimpleExchanger
breed = ec.simple.SimpleBreeder
eval = ec.simple.SimpleEvaluator
#stat.num-children = 1
#stat.child.0 = ec.gp.koza.KozaShortStatistics
#stat.child.0.file = $outtabular.stat

generations = {num_generations}
quit-on-run-complete = true

pop = ec.Population
pop.subpops = 1
pop.subpop.0 = ec.Subpopulation
pop.subpop.0.size =    1024

breed.elite.0 = 10


# ==============================
# GP general parameters
# ==============================

# GP population parameters
pop.subpop.0.species = ec.gp.GPSpecies
pop.subpop.0.species.ind = ec.gp.GPIndividual
pop.subpop.0.species.ind.numtrees = 1
pop.subpop.0.species.ind.tree.0 = ec.gp.GPTree
pop.subpop.0.species.ind.tree.0.tc = tc0

#pop.subpop.0.species.fitness = ec.gp.koza.KozaFitness
pop.subpop.0.species.fitness = ec.multiobjective.MultiObjectiveFitness
pop.subpop.0.species.fitness.num-objectives = 1
pop.subpop.0.species.fitness.maximize = false
pop.subpop.0.duplicate-retries = 100

# GP initializer
init = ec.gp.GPInitializer

# GP breeding pipeline
pop.subpop.0.species.pipe = ec.breed.MultiBreedingPipeline
pop.subpop.0.species.pipe.generate-max = false
pop.subpop.0.species.pipe.num-sources = 3
pop.subpop.0.species.pipe.source.0 = ec.gp.koza.CrossoverPipeline
pop.subpop.0.species.pipe.source.0.prob = 0.80
pop.subpop.0.species.pipe.source.1 = ec.gp.koza.MutationPipeline
pop.subpop.0.species.pipe.source.1.prob = 0.15
pop.subpop.0.species.pipe.source.2 = ec.breed.ReproductionPipeline
pop.subpop.0.species.pipe.source.2.prob = 0.05

# Selection for reproduction
breed.reproduce.source.0 = ec.select.TournamentSelection

# Koza crossover parameters
gp.koza.xover.source.0 = ec.select.TournamentSelection
gp.koza.xover.source.1 = same
gp.koza.xover.ns.0 = ec.gp.koza.KozaNodeSelector
gp.koza.xover.ns.1 = same
gp.koza.xover.maxdepth = 8
gp.koza.xover.tries = 1

# Koza mutation parameters
gp.koza.mutate.source.0 = ec.select.TournamentSelection
gp.koza.mutate.ns.0 = ec.gp.koza.KozaNodeSelector
gp.koza.mutate.build.0 = ec.gp.koza.GrowBuilder
gp.koza.mutate.maxdepth = 8
gp.koza.mutate.tries = 1

# Tournament selection, for reproduction, crossover and mutation
select.tournament.size = 7

# Koza grow parameters, for mutation
gp.koza.grow.min-depth = 4
gp.koza.grow.max-depth = 4

# Koza node selector, for crossover and mutation
gp.koza.ns.terminals = 0.1
gp.koza.ns.nonterminals = 0.9
gp.koza.ns.root = 0.0

# GP atomic (a) and set (s) type parameters
gp.type.a.size = 1
gp.type.a.0.name = nil
gp.type.s.size = 0

# GP tree constraints
gp.tc.size = 1
gp.tc.0 = ec.gp.GPTreeConstraints
gp.tc.0.name = tc0
gp.tc.0.fset = f0
gp.tc.0.returns = nil
gp.tc.0.init = ec.gp.koza.HalfBuilder

# Koza half-ramp-and-ramp parameters, for gp.tc.0.init
gp.koza.half.min-depth = 2
gp.koza.half.max-depth = 6
gp.koza.half.growp = 0.5

# GP node constraints (nc[k] means the node takes k children), no other constraint
gp.nc.size = 7

gp.nc.0 = ec.gp.GPNodeConstraints
gp.nc.0.name = nc0
gp.nc.0.returns = nil
gp.nc.0.size = 0

gp.nc.1 = ec.gp.GPNodeConstraints
gp.nc.1.name = nc1
gp.nc.1.returns = nil
gp.nc.1.size = 1
gp.nc.1.child.0 = nil

gp.nc.2 = ec.gp.GPNodeConstraints
gp.nc.2.name = nc2
gp.nc.2.returns = nil
gp.nc.2.size = 2
gp.nc.2.child.0 = nil
gp.nc.2.child.1 = nil

gp.nc.3 = ec.gp.GPNodeConstraints
gp.nc.3.name = nc3
gp.nc.3.returns = nil
gp.nc.3.size = 3
gp.nc.3.child.0 = nil
gp.nc.3.child.1 = nil
gp.nc.3.child.2 = nil

gp.nc.4 = ec.gp.GPNodeConstraints
gp.nc.4.name = nc4
gp.nc.4.returns = nil
gp.nc.4.size = 4
gp.nc.4.child.0 = nil
gp.nc.4.child.1 = nil
gp.nc.4.child.2 = nil
gp.nc.4.child.3 = nil

gp.nc.5 = ec.gp.GPNodeConstraints
gp.nc.5.name = nc5
gp.nc.5.returns = nil
gp.nc.5.size = 5
gp.nc.5.child.0 = nil
gp.nc.5.child.1 = nil
gp.nc.5.child.2 = nil
gp.nc.5.child.3 = nil
gp.nc.5.child.4 = nil

gp.nc.6 = ec.gp.GPNodeConstraints
gp.nc.6.name = nc6
gp.nc.6.returns = nil
gp.nc.6.size = 6
gp.nc.6.child.0 = nil
gp.nc.6.child.1 = nil
gp.nc.6.child.2 = nil
gp.nc.6.child.3 = nil
gp.nc.6.child.4 = nil
gp.nc.6.child.5 = nil

# GP ADF parameters
gp.problem.stack = ec.gp.ADFStack
gp.adf-stack.context = ec.gp.ADFContext

# ==============================
# GP problem specific parameters
# ==============================

# GP function set parameters
gp.fs.size = 1
gp.fs.0.name = f0
gp.fs.0.size = 7
gp.fs.0.func.0 = gputils.terminal.TerminalERCUniform
gp.fs.0.func.0.nc = nc0
gp.fs.0.func.1 = gputils.function.Add
gp.fs.0.func.1.nc = nc2
gp.fs.0.func.2 = gputils.function.Sub
gp.fs.0.func.2.nc = nc2
gp.fs.0.func.3 = gputils.function.Mul
gp.fs.0.func.3.nc = nc2
gp.fs.0.func.4 = gputils.function.Div
gp.fs.0.func.4.nc = nc2
gp.fs.0.func.5 = gputils.function.Max
gp.fs.0.func.5.nc = nc2
gp.fs.0.func.6 = gputils.function.Min
gp.fs.0.func.6.nc = nc2
gp.fs.0.func.7 = gputils.function.If
gp.fs.0.func.7.nc = nc3

# ==============================
# The output format
# ==============================
#gp.tree.print-style = c
#pop.subpop.0.species.ind.tree.0.c-operators = false
#gp.tree.print-style = dot
'''
        param = param_template.format(experiment_name=self.name,
                                      num_generations=self.num_generations,
                                      stat_dir=self.get_stat_dir(run_index),
                                      num_vehicles=self.num_vehicles,
                                      data_set=self.data_set)

        return param
