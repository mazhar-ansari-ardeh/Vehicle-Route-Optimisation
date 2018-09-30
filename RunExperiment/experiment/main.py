'''
Created on 23/09/2018

@author: mazhar
'''

from experiment import UCARPExperiment
from experiment import KnowledgeableExperiment

NUM_OF_RUNS = 1

JAVA_CP = ('.:/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/'
           + 'commons-lang3-3.7/commons-lang3-3.7.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/commons-math3-3.6.1/'
           + 'commons-math3-3.6.1.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-core-0.27.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-dense64-0.27.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-simple-0.27.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/itext-1.2.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/jcommon-1.0.16.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/jzlib-1.0.7.jar'
           + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/pshecj.jar')


def main():
    '''
    The main function
    '''

    gdb1_v5 = UCARPExperiment('gdb1-v5', 'gdb/gdb1.dat', 5, num_runs=NUM_OF_RUNS,
                              show_ecj_output=True,
                              num_generations=30,
                              java_library_classpath=JAVA_CP,
                              stat_root_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/stats/ucarp',
                              java_bin_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/bin/',
                              param_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/params/ucarp')

    # gdb1_v5 is used as a source problem.
    # gdb1_v5.run()
    gdb1_v5.reloadFitnesses()
    print('Fitness of gdb1-v5: ', gdb1_v5.get_best_fitness())

    gdb1_v9 = UCARPExperiment('gdb1-v9', 'gdb/gdb1.dat', 9, num_runs=NUM_OF_RUNS,
                              show_ecj_output=True,
                              num_generations=30,
                              java_library_classpath=JAVA_CP,
                              stat_root_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/stats/ucarp',
                              java_bin_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/bin/',
                              param_dir='/Users/mzhr/MyPhD/SourceCodes/gpucarp/params/ucarp')

    # gdb1_v9.run(False)
    gdb1_v9.reloadFitnesses()
    print('Fitness of gdb1-v9 without knowledge: ', gdb1_v9.get_best_fitness())

    kgdb1_v9 = KnowledgeableExperiment(gdb1_v9,
                                       KnowledgeableExperiment.FittedCodeFragmentBuilder,
                                       knowledge_file='final-pop-gdb1-v5.dat',
                                       knowledge_probability=0.5,
                                       tournament_size=20, filter_size=None)
    # kgdb1_v9.run(False)
    kgdb1_v9.reloadFitnesses()
    print('Fitness of gdb1-v9 with knowledge and without filter: ',
          kgdb1_v9.get_best_fitness())

    fkgdb1_v9 = KnowledgeableExperiment(gdb1_v9,
                                        KnowledgeableExperiment.FilteredFittedCodeFragmentBuilder,
                                        knowledge_file='final-pop-gdb1-v5.dat',
                                        knowledge_probability=0.5,
                                        tournament_size=20, filter_size=100)
    # fkgdb1_v9.run(False)
    fkgdb1_v9.reloadFitnesses()
    print('Fitness of gdb1-v9 with knowledge and filter: ',
          fkgdb1_v9.get_best_fitness())


if __name__ == '__main__':

    main()

    # fitted = ECJExperiment('fitted',
    #                        use_knowledge = True,
    #                        num_runs=num_of_runs,
    #                        knowledge_probability = 0.5,
    #                        knowledge_tournament_size = 20,
    #                        knowledgebased_builder = 'tutorial7.gp.FittedCodeFragmentBuilder',
    #                        knowledge_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/knowledge/',
    #                        stat_root_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/stats/fitted',
    #                        param_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/params/fitted')
    # # fitted.run(False)
    # fitted.reloadFitnesses()
    # print(fitted.getBestFitness())
    # print(fitted.getMean())
    # print(fitted.getStdev())
    # print()

    # filtered = ECJExperiment('filtered',
    #                        use_knowledge = True,
    #                        num_runs=num_of_runs,
    #                        knowledgebased_builder = 'tutorial7.gp.FilteredFittedCodeFragmentBuilder',
    #                        knowledge_filter_size = 100,
    #                        knowledge_probability = 0.5,
    #                        knowledge_tournament_size = 20,
    #                        knowledge_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/knowledge/',
    #                        stat_root_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/stats/filtered',
    #                        param_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/params/filtered')
    # # filtered.run(False)
    # filtered.reloadFitnesses()
    # print(filtered.getBestFitness())
    # print(filtered.getMean())
    # print(filtered.getStdev())
    # print()

    # without = ECJExperiment('without',
    #                        use_knowledge = False,
    #                        num_runs=num_of_runs,
    #                        show_ecj_output=True,
    #                        knowledge_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/knowledge/',
    #                        stat_root_dir='/home/mazhar/MyPhD/SourceCodes/gpucarp/stats/without',
    #                        param_dir = '/home/mazhar/MyPhD/SourceCodes/gpucarp/params/without')
    # # without.run(False)
    # without.reloadFitnesses()
    # print(without.getBestFitness())
    # print(without.getMean())
    # print(without.getStdev())
