from .core import rename_alg
from .core import should_process
from .core import sort_algorithms
from .core import get_test_fitness
from .core import get_train_fitness
from .core import natural_keys

import statistics
import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path
import seaborn as sns
import numpy as np
import re


def violin_plot(data_table, gen, rename_map, output_folder, alg_order):
    # plt.style.use('seaborn-muted')
    # plt.style.use('fast')
    plt.style.use('bmh')
    plt.rcParams['figure.figsize'] = [16, 9]
    plt.rcParams['figure.dpi'] = 300

    df = pd.DataFrame(columns=["Fitness", "Algorithm", 'Run'])
    for alg in data_table:
        for run in data_table[alg][-1]:
            pdf = pd.DataFrame(
                {"Algorithm": rename_alg(alg, rename_map), 'Run': run, 'Fitness': data_table[alg][-1][run]},
                index=range(1, 2))
            df = df.append(pdf, sort=False)

    algs = []
    data = []
    for alg in data_table:
        algs.append(rename_alg(alg, rename_map))
        data.append(list(data_table[alg][gen].values()))

    fig, ax = plt.subplots(1, 1)
    sns.violinplot(data=df, ax=ax, x='Algorithm', y="Fitness", order=alg_order)
    ax.tick_params(axis='both', which='major', labelsize=30)
    ax.set_xlabel('Algorithm', fontdict={'fontsize': 35})
    ax.set_ylabel('Fitness', fontdict={'fontsize': 35})
    for label in ax.get_xticklabels():
        label.set_rotation(90)
    fig.savefig(output_folder / 'violin.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig)
    # def set_axis_style(ax, labels):
    #     ax.get_xaxis().set_tick_params(direction='out')
    #     ax.xaxis.set_ticks_position('bottom')
    #     ax.set_xticks(range(1, len(labels) + 1))
    #     ax.set_xticklabels(labels)
    #     ax.set_xlim(0.25, len(labels) + 0.75)
    #     ax.set_xlabel('Algorithm', fontdict={'fontsize': 120})
    #     ax.set_ylabel('Fitness', fontdict={'fontsize': 120})
    #     ax.tick_params(axis='both', which='major', labelsize=45)
    #
    # def adjacent_values(vals, q1, q3):
    #     upper_adjacent_value = q3 + (q3 - q1) * 1.5
    #     upper_adjacent_value = np.clip(upper_adjacent_value, q3, vals[-1])
    #
    #     lower_adjacent_value = q1 - (q3 - q1) * 1.5
    #     lower_adjacent_value = np.clip(lower_adjacent_value, vals[0], q1)
    #     return lower_adjacent_value, upper_adjacent_value
    #
    #
    # fig = plt.figure(figsize=(60, 32))
    # ax = fig.add_subplot(111)
    # parts = ax.violinplot(data, showmeans=True, showmedians=True)
    #
    # for pc in parts['bodies']:
    #     # pc.set_facecolor('#D43F3A')
    #     # pc.set_edgecolor('black')
    #     pc.set_alpha(1)
    #
    # # flierprops = dict(marker='o', markerfacecolor='r', markersize=30, linestyle='none')
    # # boxprops = dict(linestyle='-', linewidth=6, color='black')
    # # whiskerprops = dict(linewidth=6)
    # # medianprops = dict(linewidth=6)
    # #
    # # ax.boxplot(data, flierprops=flierprops, boxprops=boxprops, whiskerprops=whiskerprops,
    # #                medianprops=medianprops)
    #
    # # quartile1, medians, quartile3 = np.percentile(data, [25, 50, 75], axis=1)
    # # whiskers = np.array([
    # #     adjacent_values(sorted_array, q1, q3)
    # #     for sorted_array, q1, q3 in zip(data, quartile1, quartile3)])
    # # whiskers_min, whiskers_max = whiskers[:, 0], whiskers[:, 1]
    # # inds = np.arange(1, len(medians) + 1)
    # # ax.scatter(inds, medians, marker='o', color='white', s=30, zorder=3)
    # # ax.vlines(inds, quartile1, quartile3, color='k', linestyle='-', lw=5)
    # # ax.vlines(inds, whiskers_min, whiskers_max, color='k', linestyle='-', lw=11)
    #
    # set_axis_style(ax, algs)
    # plt.show()
    pass


def plot_grid_output(test_fitness, exp_name, inclusion_filter, exclusion_filter, output_folder,
                     rename_map, base_line='WithoutKnowledge', boxplots=False, markersize=15, linewidth=19, lcols=2,
                     lfontsize=95, lmarkerscale=1.2, llinewidth=20):
    print("Plotting", exp_name)
    plt.style.use('default')
    markers = ["", ',', 'o', 'v', '^', '<', '>', 's', 'p', '*', 'h', 'H', '+', 'x', 'D', 'd', '|', '_']
    line_styles = ['--', '-.', ':']
    sc = 0  # line style counter
    mc = 0  # marker counter

    fig_all = plt.figure(figsize=(60, 32))
    ax_all = fig_all.add_subplot(111)
    ax_all.set_xlabel('Generation', fontdict={'fontsize': 120})
    ax_all.set_ylabel('Fitness', fontdict={'fontsize': 120})
    ax_all.tick_params(axis='both', which='major', labelsize=100)

    # Algorithms in this list will be ignored and filtered out

    for algorithm in sort_algorithms(test_fitness):
        if not should_process(algorithm, inclusion_filter, exclusion_filter):
            continue

        box_data = []
        mean_data = []
        # miin = []
        # maax = []
        yerr = []
        for gen in test_fitness[algorithm]:
            box_data.append((list(test_fitness[algorithm][gen].values())))
            mean_data.append(statistics.mean(list(test_fitness[algorithm][gen].values())))
            # miin.append(min((list(test_fitness[algorithm][gen].values()))))
            # maax.append(max((list(test_fitness[algorithm][gen].values()))))
            yerr.append(statistics.stdev(list(test_fitness[algorithm][gen].values())))

        if not Path(output_folder / exp_name).exists():
            Path(output_folder / exp_name).mkdir(parents=True)

        if boxplots:
            box_fig = plt.figure(figsize=(60, 32))
            box_ax = box_fig.add_subplot(111)
            box_ax.set_title(rename_alg(algorithm, rename_map), fontdict={'fontsize': 100})
            box_ax.set_xlabel('Generation', fontdict={'fontsize': 120})
            box_ax.set_ylabel('Fitness', fontdict={'fontsize': 120})

            flierprops = dict(marker='o', markerfacecolor='r', markersize=30, linestyle='none')
            boxprops = dict(linestyle='-', linewidth=6, color='black')
            whiskerprops = dict(linewidth=6)
            medianprops = dict(linewidth=6)

            box_ax.boxplot(box_data, flierprops=flierprops, boxprops=boxprops, whiskerprops=whiskerprops,
                           medianprops=medianprops)
            box_ax.tick_params(axis='both', which='major', labelsize=45)
            box_ax.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth / 4, label=exp_name)
            box_ax.legend(fontsize=lfontsize, ncol=lcols, markerscale=lmarkerscale)
            box_fig.savefig(output_folder / f'{exp_name + "/" + algorithm}.jpg', bbox_inches='tight', pad_inches=0)
            plt.close(box_fig)
            # print('Saved', output_folder / f'{exp_name + "/" + algorithm}.jpg')

        label = rename_alg(algorithm, rename_map)
        if algorithm == base_line:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth, markersize=markersize,
                        label="No Transfer", color='k')
        else:
            sc += 1
            sc %= len(line_styles)
            mc += 1
            mc %= len(markers)

            # Uncomment this to make it draw error bars. 
            # ax_all.errorbar(range(1, len(mean_data) + 1), mean_data, label=label, linewidth=linewidth
            #                     , linestyle=line_styles[sc], marker=markers[mc], markersize=30, yerr=yerr)
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth=linewidth
                        , linestyle=line_styles[sc], marker=markers[mc], markersize=markersize)

    leg = ax_all.legend(fontsize=lfontsize, ncol=lcols, markerscale=lmarkerscale)
    for line in leg.get_lines():
        line.set_linewidth(llinewidth)

    fig_all.savefig(output_folder / exp_name / f'{exp_name}-all.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig_all)


# def atof(text):
#     try:
#         retval = float(text)
#     except ValueError:
#         retval = text
#     return retval


# def natural_keys(text):
#     """
#     alist.sort(key=natural_keys) sorts in human order
#     http://nedbatchelder.com/blog/200712/human_sorting.html
#     (See Toothy's implementation in the comments)
#     float regex comes from https://stackoverflow.com/a/12643073/190597
#     """
#     return [atof(c) for c in re.split(r'[+-]?([0-9]+(?:[.][0-9]*)?|[.][0-9]+)', text)]


def plot_twinx(df, save_file, markersize=60, linewidth=6, lcols=2,
               lfontsize=95, lmarkerscale=1.2, llinewidth=15):
    df = df.reindex(sorted(df.index, key=natural_keys), axis=0)
    fig = plt.figure(figsize=(60, 30))
    ax1 = fig.add_subplot(111)
    ax1.set_xlabel('Algorithm', fontdict={'fontsize': 125})
    ax1.set_ylabel(df.columns[0], fontdict={'fontsize': 120})
    # ax1.set_xticks(np.arange(len(df.index)), minor=True)
    # ax1.set_xticklabels(df.index)
    ax1.set_xticks(np.arange(len(df.index)))
    ax1.tick_params(axis='both', labelsize=80)
    df.iloc[:, 0].plot(ax=ax1, style='r*:', label=df.columns[0], linewidth=linewidth, markersize=markersize+5)
    for label in ax1.get_xticklabels():
        label.set_rotation(90)
    leg = ax1.legend(fontsize=lfontsize, ncol=1, markerscale=lmarkerscale+0.1, loc='upper left')
    for line in leg.get_lines():
        line.set_linewidth(llinewidth)

    ax2 = ax1.twinx()
    ax2.set_ylabel(df.columns[1], fontdict={'fontsize': 120})
    ax2.tick_params(axis='y', labelsize=80)
    df.iloc[:, 1].plot(ax=ax2, style='go:', label=df.columns[1], linewidth=linewidth, markersize=markersize)
    leg = ax2.legend(fontsize=lfontsize, ncol=1, markerscale=lmarkerscale, loc='upper right')
    for line in leg.get_lines():
        line.set_linewidth(llinewidth)

    fig.savefig(save_file, bbox_inches='tight', pad_inches=0)


# Warning: This is not finished.
def plot_surr(dirbase, exp, algorithms, output_folder, GENERATIONS=50):
    fontsize = 50
    ncol = 2
    markerscale = 1
    markersize = 12
    labelsize = 30
    linewidth = 15

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
        fig = plt.figure(figsize=(60, 32))
        ax_fit = fig.add_subplot(211)
        ax_fit.set_xlabel('Generation', fontdict={'fontsize': 120})
        ax_fit.set_ylabel('Fitness', fontdict={'fontsize': 120})

        ax_cor = fig.add_subplot(212)
        ax_cor.set_xlabel('Generation', fontdict={'fontsize': 120})
        ax_cor.set_ylabel('Corelation', fontdict={'fontsize': 120})
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
            ax_fit.plot(range(1, len(surrMean) + 1), list(surrMean.values()), linewidth=linewidth,
                        label=alg.split('_')[-1] + 'Surrogate')
            ax_fit.plot(range(1, len(fitMean) + 1), list(fitMean.values()), linewidth=linewidth,
                        label=alg.split('_')[-1] + 'Fitness')
            ax_cor.plot(range(1, len(corr) + 1), list(corr.values()), linewidth=linewidth,
                        label=alg.split('_')[-1] + '')

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


def test_train_scatter(inclusion, exclusion, experiments, dirbase, base_line='WithoutKnowledge'):
#
    fig = plt.figure(figsize=(20, 14))
    markers = ['o', 'v', '^', 's', 'p', '*', 'h', '+', 'x', 'D', 'd', '|', '_']
    cnt = 0

#     fontsize=18
#     ncol=1
#     markerscale=1
#     markersize=12
#     labelsize=30

    exp = experiments[0]
    print('\n', exp)
    test_fitness = get_test_fitness(dirbase / exp, inclusion, exclusion)
    train_fitness = get_train_fitness(dirbase / exp, inclusion, exclusion)
    cnt = 0
    for alg in test_fitness:
        if should_process(alg, inclusion, exclusion):
            x = []
            y = []
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
