from .core import rename_alg
from .core import should_process
from .core import sort_algorithms

import statistics
import matplotlib.pyplot as plt
import pandas as pd

from pathlib import Path

def plot_grid_output(test_fitness, exp_name, inclusion_filter, exclusion_filter, output_folder,
                     rename_map, base_line='WithoutKnowledge', boxplots=False, lcols=2, lfontsize=95, linewidth=19):

    print("Plotting", exp_name)
    markers = ["", ',', 'o', 'v', '^', '<', '>', 's', 'p', '*', 'h', 'H', '+', 'x', 'D', 'd', '|', '_']
    line_styles = ['--', '-.', ':']
    sc = 0 # line style counter
    mc = 0 # marker counter
    
    fig_all = plt.figure(figsize=(60, 32))
    ax_all = fig_all.add_subplot(111)
    ax_all.set_xlabel('Generation', fontdict={'fontsize':120 })
    ax_all.set_ylabel('Fitness', fontdict={'fontsize':120 })
    ax_all.tick_params(axis='both', which='major', labelsize=100)

    # Algorithms in this list will be ignored and filtered out
    
    for algorithm in sort_algorithms(test_fitness):
    # for algorithm in (test_fitness):
        if not should_process(algorithm, inclusion_filter, exclusion_filter):
            continue

        box_data = []
        mean_data = []
        # miin = []
        # maax = []
        # yerr = []
        for gen in test_fitness[algorithm]:
            box_data.append((list(test_fitness[algorithm][gen].values())))
            mean_data.append(statistics.mean(list(test_fitness[algorithm][gen].values())))
            # miin.append(min((list(test_fitness[algorithm][gen].values()))))
            # maax.append(max((list(test_fitness[algorithm][gen].values()))))
            # yerr.append(statistics.stdev(list(test_fitness[algorithm][gen].values())))


        if not Path(output_folder / exp_name).exists(): 
            Path(output_folder / exp_name).mkdir(parents=True)

        if boxplots:
            box_fig = plt.figure(figsize=(60,32))
            box_ax = box_fig.add_subplot(111)
            box_ax.set_title(rename_alg(algorithm, rename_map), fontdict={'fontsize':100 })
            box_ax.set_xlabel('Generation', fontdict={'fontsize':120 })
            box_ax.set_ylabel('Fitness', fontdict={'fontsize':120 })

            flierprops = dict(marker='o', markerfacecolor='r', markersize=30, linestyle='none')
            boxprops = dict(linestyle='-', linewidth=6, color='black')
            whiskerprops = dict(linewidth=6)
            medianprops = dict(linewidth=6)

            box_ax.boxplot(box_data, flierprops=flierprops, boxprops=boxprops, whiskerprops=whiskerprops,medianprops=medianprops)
            box_ax.tick_params(axis='both', which='major', labelsize=45)
            box_ax.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth/4, label=exp_name)
            box_ax.legend(fontsize=lfontsize, ncol=lcols, markerscale=1)
            box_fig.savefig(output_folder / f'{exp_name + "/" + algorithm}.jpg', bbox_inches='tight', pad_inches=0)
            plt.close(box_fig)
            # print('Saved', output_folder / f'{exp_name + "/" + algorithm}.jpg')

        label = rename_alg(algorithm, rename_map)
        if algorithm == base_line:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth,  markersize=15, label="No Transfer", color='k')
        else:
            sc += 1 
            sc %= len(line_styles) 
            mc += 1
            mc %= len(markers)

            # Uncomment this to make it draw error bars. 
            # ax_all.errorbar(range(1, len(mean_data) + 1), mean_data, label=label, linewidth = linewidth
            #                     , linestyle=line_styles[sc], marker=markers[mc], markersize=30, yerr = yerr)
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth = linewidth
                                , linestyle=line_styles[sc], marker=markers[mc], markersize=30)

    leg = ax_all.legend(fontsize=lfontsize, ncol=lcols, markerscale=1)
    for line in leg.get_lines():
        line.set_linewidth(20)
    
    fig_all.savefig(output_folder / exp_name / f'{exp_name}-all.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig_all)

# Warning: This is not finished.
def plot_surr(dirbase, exp, algorithms, output_folder, GENERATIONS=50):

    fontsize=50
    ncol=2
    markerscale=1
    markersize=12
    labelsize=30
    linewidth=15

    # exp = 'gdb2.vs6.gdb2.vt5:gen_50'
    # algs = ['Surrogate:initsurpool_true:tp_0:surupol_Reset', 'Surrogate:initsurpool_true:tp_0:surupol_FIFONoDupPhenotypic', 
    #         'Surrogate:initsurpool_true:tp_0:surupol_AddOncePhenotypic', 
    #         'Surrogate:initsurpool_true:tp_0:surupol_UnboundedPhenotypic',
    #         'Surrogate:initsurpool_true:tp_0:surupol_Unbounded']
    # run = 7
    for run in range(1, 31):
        surrMean = {}
        fitMean = {}
        corr = {}
        fig = plt.figure(figsize=(60,32))
        ax_fit = fig.add_subplot(211)
        ax_fit.set_xlabel('Generation', fontdict={'fontsize':120 })
        ax_fit.set_ylabel('Fitness', fontdict={'fontsize':120 })

        ax_cor = fig.add_subplot(212)
        ax_cor.set_xlabel('Generation', fontdict={'fontsize':120 })
        ax_cor.set_ylabel('Corelation', fontdict={'fontsize':120 })
        for alg in algorithms:
            for i in range(GENERATIONS):
                file_name = f'Pop.{i}.csv.gz'
                file = dirbase / exp / alg / str(run) / 'pop' / file_name
                try:
                    csv = pd.read_csv(file)
                except Exception as e:
                    print(e)
                    continue
                surrMean[i] = csv['SurrogateFitness'].mean()
                fitMean[i] = csv['Fitness'].mean()
                corr[i] = csv['SurrogateFitness'].corr(csv['Fitness'])
            ax_fit.plot(range(1, len(surrMean) + 1), list(surrMean.values()), linewidth=linewidth, label=alg.split('_')[-1]+'Surrogate')
            ax_fit.plot(range(1, len(fitMean) + 1), list(fitMean.values()), linewidth=linewidth, label=alg.split('_')[-1]+'Fitness')
            ax_cor.plot(range(1, len(corr) + 1), list(corr.values()), linewidth=linewidth, label=alg.split('_')[-1]+'')
        
        leg = ax_fit.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
        ax_fit.tick_params(axis='both', which='major', labelsize=labelsize)
        for line in leg.get_lines():
            line.set_linewidth(10)
        leg = ax_cor.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
        ax_cor.tick_params(axis='both', which='major', labelsize=labelsize)
        for line in leg.get_lines():
            line.set_linewidth(10)

        # ax.set_xlabel('Train Fitness')
        # ax.set_ylabel('Test Fitness')
        if not Path(output_folder / exp / 'surr').exists(): 
                Path(output_folder / exp / 'surr').mkdir(parents=True)
        fig.savefig(output_folder / exp / 'surr' / f'surr.{run}.jpg')
        plt.show()


# def test_train_scatter(algorithm=[
#     'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
#     'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.1:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_false',
#     # 'PPTBreeding:ppt_0.1:cmpppt_0:xover_0.7:mut_0.15:repro_0.05:lr_0.2:ss_100:ts:_7:initperc_0.5:igen_49_49:inrad_-1:incap_1:mnThr_0:clear_true',
#     # 'FullTree:tp_10:dup_true:clear_true',
#     'FullTree:tp_50:dup_false:clear_true',
#     # 'Subtree:perc_10:clear_true',
#     'Subtree:perc_50:clear_true',
#     ],
#     base_line='WithoutKnowledge:clear_true'):
#
#     fig = plt.figure(figsize=(20, 14))
#     markers = ['o', 'v', '^', 's', 'p', '*', 'h', '+', 'x', 'D', 'd', '|', '_']
#     cnt = 0
#
#     experiments = [
#                             'gdb1.vs5.gdb1.vt4:gen_50',
#                             # 'gdb1.vs5.gdb1.vt6:gen_50',
#                             # 'gdb2.vs6.gdb2.vt5:gen_50',
#                             # 'gdb2.vs6.gdb2.vt7:gen_50',
#                             # 'gdb3.vs5.gdb3.vt4:gen_50',
#                             'gdb3.vs5.gdb3.vt6:gen_50',
#                             # 'gdb4.vs4.gdb4.vt3:gen_50',
#                             # 'gdb4.vs4.gdb4.vt5:gen_50',
#                             # 'gdb5.vs6.gdb5.vt5:gen_50',
#                             # 'gdb5.vs6.gdb5.vt7:gen_50',
#                             # 'gdb6.vs5.gdb6.vt4:gen_50',
#                             # 'gdb6.vs5.gdb6.vt6:gen_50',
#                             'gdb7.vs5.gdb7.vt4:gen_50',
#                             # 'gdb7.vs5.gdb7.vt6:gen_50',
#                             # 'gdb12.vs7.gdb12.vt6:gen_50',
#                             # 'gdb12.vs7.gdb12.vt8:gen_50',
#                             # 'gdb13.vs6.gdb13.vt5:gen_50',
#                             # 'gdb13.vs6.gdb13.vt7:gen_50',
#                             # 'gdb14.vs5.gdb14.vt4:gen_50',
#                             # 'gdb14.vs5.gdb14.vt6:gen_50',
#                             # 'gdb15.vs4.gdb15.vt3:gen_50',
#                             # 'gdb15.vs4.gdb15.vt5:gen_50', 
#                             # 'gdb16.vs5.gdb16.vt4:gen_50',
#                             # 'gdb16.vs5.gdb16.vt6:gen_50',
#                             # 'gdb17.vs5.gdb17.vt4:gen_50',
#                             # 'gdb17.vs5.gdb17.vt6:gen_50',
#                             'gdb19.vs3.gdb19.vt2:gen_50',
#                             # 'gdb19.vs3.gdb19.vt4:gen_50',
#                             # 'gdb20.vs4.gdb20.vt3:gen_50',
#                             # 'gdb20.vs4.gdb20.vt5:gen_50',
#                             # 'gdb18.vs5.gdb18.vt4:gen_50',
#                             # 'gdb18.vs5.gdb18.vt6:gen_50',
#                             # 'gdb21.vs6.gdb21.vt5:gen_50',
#                             # 'gdb21.vs6.gdb21.vt7:gen_50',
# ]


#     fontsize=18
#     ncol=1
#     markerscale=1
#     markersize=12
#     labelsize=30

#     exp = experiments[0]
#     print('\n', exp)
#     test_fitness = get_test_fitness(dirbase / exp)
#     train_fitness = get_train_mean(dirbase / exp)
#     cnt = 0
#     for alg in [*algorithm, base_line]:
#         x = []
#         y = []
#         for run in range(1, 31):
#             x.append(train_fitness[alg][49][run])
#             y.append(test_fitness[alg][49][run])

#         # x = list(train_fitness[alg][49].values())
#         # y = list(test_fitness[alg][49].values())
#         ax = fig.add_subplot(221)
#         ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
#         # ax.set_title(algorithm + ': combo')
#         # ax.set_xlabel('Generation')
#         # ax.set_ylabel('Fitness')
#         cnt += 1
#         # ax.legend()
#     leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
#     ax.tick_params(axis='both', which='major', labelsize=labelsize)
#     for line in leg.get_lines():
#         line.set_linewidth(25)

#     # plt.show()


#     exp = experiments[1]
#     print('\n', exp)
#     test_fitness = get_test_fitness(dirbase / exp)
#     train_fitness = get_train_mean(dirbase / exp)
#     cnt = 0
#     for alg in [*algorithm, base_line]:
#         x = []
#         y = []
#         for run in range(1, 31):
#             x.append(train_fitness[alg][49][run])
#             y.append(test_fitness[alg][49][run])

#         # x = list(train_fitness[alg][49].values())
#         # y = list(test_fitness[alg][49].values())
#         ax = fig.add_subplot(222)
#         ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt],s=markersize**2)
#         # ax.set_title(algorithm + ': combo')
#         # ax.set_xlabel('Generation')
#         # ax.set_ylabel('Fitness')
#         cnt += 1
#         # ax.legend()
#     leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
#     ax.tick_params(axis='both', which='major', labelsize=labelsize)
#     for line in leg.get_lines():
#         line.set_linewidth(25)
        
#     exp = experiments[2]
#     print('\n', exp)
#     test_fitness = get_test_fitness(dirbase / exp)
#     train_fitness = get_train_mean(dirbase / exp)
#     cnt = 0
#     for alg in [*algorithm, base_line]:
#         x = []
#         y = []
#         for run in range(1, 31):
#             x.append(train_fitness[alg][49][run])
#             y.append(test_fitness[alg][49][run])

#         # x = list(train_fitness[alg][49].values())
#         # y = list(test_fitness[alg][49].values())
#         ax = fig.add_subplot(223)
#         ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
#         # ax.set_title(algorithm + ': combo')
#         # ax.set_xlabel('Generation')
#         # ax.set_ylabel('Fitness')
#         cnt += 1
#         # ax.legend()
#     leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
#     ax.tick_params(axis='both', which='major', labelsize=labelsize)
#     for line in leg.get_lines():
#         line.set_linewidth(25)

#     cnt = 0
#     exp = experiments[3]
#     print('\n', exp)
#     test_fitness = get_test_fitness(dirbase / exp)
#     train_fitness = get_train_mean(dirbase / exp)
#     for alg in [*algorithm, base_line]:
#         x = []
#         y = []
#         for run in range(1, 31):
#             x.append(train_fitness[alg][49][run])
#             y.append(test_fitness[alg][49][run])

#         # x = list(train_fitness[alg][49].values())
#         # y = list(test_fitness[alg][49].values())
#         ax = fig.add_subplot(224)
#         ax.scatter(x, y, label=rename_alg(alg), marker=markers[cnt], s=markersize**2)
#         # ax.set_title('Train Fitness')
#         # ax.set_xlabel('Generation')
#         # ax.set_ylabel('Fitness')
#         cnt += 1
#         # ax.legend()

#     leg = ax.legend(fontsize=fontsize, ncol=ncol, markerscale=markerscale)
#     ax.tick_params(axis='both', which='major', labelsize=labelsize)
#     for line in leg.get_lines():
#         line.set_linewidth(25)

#     # ax.set_xlabel('Train Fitness')
#     # ax.set_ylabel('Test Fitness')
#     fig.savefig('scatter.jpg')
#     plt.show()