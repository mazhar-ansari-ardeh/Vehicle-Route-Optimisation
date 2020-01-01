'''
Created on 23/09/2018

@author: mazhar
'''

from experiment import UCARPExperiment
from experiment import KnowledgeableExperiment

import os

NUM_OF_RUNS = 1
NUM_GENERATIONS = 50

# JAVA_CP = ('.:/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/'
#            + 'commons-lang3-3.7/commons-lang3-3.7.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/commons-math3-3.6.1/'
#            + 'commons-math3-3.6.1.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-core-0.27.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-dense64-0.27.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/EJML-simple-0.27.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/itext-1.2.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/jcommon-1.0.16.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/jzlib-1.0.7.jar'
#            + ':/Users/mzhr/MyPhD/SourceCodes/gpucarp/libraries/pshecj.jar')

JAVA_CP = '.:gpucarp-tl.jar'

STAT_ROOT_DIR = '/local/scratch/'
JAVA_BIN_DIR = '.'
PARAM_DIR = './params/'


def DoKnowledgeExperiment(exp_name, data_set, run_source, num_source_vehicle, num_target_vehicle):
    # finish.final-pop-file-name = final-pop-{experiment_name}.dat

    source_exp_name = exp_name+'-v'+str(num_source_vehicle)
    if run_source:
        source_exp = UCARPExperiment(source_exp_name, data_set, num_source_vehicle,
                                     num_runs=1,  # WE just want it to create a knowledge base
                                     show_ecj_output=True,
                                     num_generations=NUM_GENERATIONS,
                                     java_library_classpath=JAVA_CP,
                                     stat_root_dir=STAT_ROOT_DIR,
                                     java_bin_dir=JAVA_BIN_DIR,
                                     param_dir=PARAM_DIR)
        source_exp.run(False)
        print('Fitness on source: ', source_exp.get_best_fitness())

    target_exp = UCARPExperiment(exp_name+'-v'+str(num_target_vehicle), data_set, num_target_vehicle,
                                 show_ecj_output=True,
                                 num_generations=NUM_GENERATIONS,
                                 java_library_classpath=JAVA_CP,
                                 stat_root_dir=STAT_ROOT_DIR,
                                 java_bin_dir=JAVA_BIN_DIR,
                                 param_dir=PARAM_DIR)
    target_exp.reloadFitnesses()
    target_exp.run(False)
    print('Fitness on target: ', source_exp.get_best_fitness())

    ktarget = KnowledgeableExperiment(target_exp,
                                      KnowledgeableExperiment.FittedCodeFragmentBuilder,
                                      knowledge_file='final-pop-'+source_exp_name+'.dat',
                                      knowledge_probability=0.5,
                                      tournament_size=20, filter_size=None)
    ktarget.run(False)
    ktarget.reloadFitnesses()
    print('Fitness of with knowledge and without filter: ',
          ktarget.get_best_fitness())

    fktarget = KnowledgeableExperiment(target_exp,
                                       KnowledgeableExperiment.FilteredFittedCodeFragmentBuilder,
                                       knowledge_file='final-pop-'+source_exp_name+'.dat',
                                       knowledge_probability=0.5,
                                       tournament_size=20, filter_size=100)
    fktarget.run(False)
    fktarget.reloadFitnesses()
    print('Fitness of with knowledge and filter: ', fktarget.get_best_fitness())


def main():
    '''
    The main function
    '''
    print(os.getcwd())
    DoKnowledgeExperiment('gdb1', 'gdb/gdb1.dat', True, 5, 6)
    ...


if __name__ == '__main__':

    main()
