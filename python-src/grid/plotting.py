import statistics
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns

from .core import natural_keys
from .core import rename_alg
from .core import sort_algorithms


def violin_plot(data_table, gen, rename_map, output_folder, alg_order):
    plt.style.use('bmh')
    plt.rcParams['figure.figsize'] = [16, 9]
    plt.rcParams['figure.dpi'] = 300

    df = pd.DataFrame(columns=["Fitness", "Algorithm", 'Run'])
    for alg in data_table:
        for run in data_table[alg][gen]:
            pdf = pd.DataFrame(
                {"Algorithm": rename_alg(alg, rename_map), 'Run': run, 'Fitness': data_table[alg][gen][run]},
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
    ax.set_xlabel('', fontdict={'fontsize': 1})
    ax.set_ylabel('Fitness', fontdict={'fontsize': 35})
    for label in ax.get_xticklabels():
        label.set_rotation(90)
    fig.savefig(output_folder / 'violin.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig)


def plot_grid_output(test_fitness, exp_name, output_folder, rename_map, base_line='WithoutKnowledge', target_alg='',
                     boxplots=False, markersize=15, linewidth=19, lcols=2,
                     lfontsize=95, lmarkerscale=1.2, llinewidth=20, figsize=(60, 32)):
    print("Plotting", exp_name)
    plt.style.use('default')
    markers = ["", ',', 'v', 's', '*', 'd', '^', '<', '>', 'p', 'h', 'H', '+', 'x', 'D', '|', '_']
    line_styles = ['--', '-.', ':']
    colors = ['#ff7f0e', '#2ca02c', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf', '#1f77b4']
    sc = 0  # line style counter
    mc = 0  # marker counter
    cc = -1  # color counter

    fig_all = plt.figure(figsize=figsize)
    ax_all = fig_all.add_subplot(111)
    ax_all.set_xlabel('Generation', fontdict={'fontsize': 120})
    ax_all.set_ylabel('Fitness', fontdict={'fontsize': 120})
    ax_all.tick_params(axis='both', which='major', labelsize=100)

    # axins = zoomed_inset_axes(ax_all, 1.5, loc=5)  # zoom = 6
    # Algorithms in this list will be ignored and filtered out

    for algorithm in sort_algorithms(test_fitness):
        # if not should_process(algorithm, inclusion_filter, exclusion_filter):
        #     continue

        box_data = []
        mean_data = []
        yerr = []

        for gen in test_fitness[algorithm]:
            box_data.append((list(test_fitness[algorithm][gen].values())))
            mean_data.append(statistics.mean(list(test_fitness[algorithm][gen].values())))
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
        if label == base_line or algorithm == base_line:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth, markersize=markersize,
                        label="GPHH", color='k')
        elif label == target_alg or algorithm == target_alg:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth, markersize=markersize,
                        label=target_alg, color='#d62728', marker='o', linestyle='-', rasterized=True)
        else:
            sc += 1
            sc %= len(line_styles)
            mc += 1
            mc %= len(markers)
            cc += 1
            cc %= len(colors)

            # Uncomment this to make it draw error bars. 
            # ax_all.errorbar(range(1, len(mean_data) + 1), mean_data, label=label, linewidth=linewidth
            #                     , linestyle=line_styles[sc], marker=markers[mc], markersize=30, yerr=yerr)

            ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth=linewidth, rasterized=True,
                        linestyle=line_styles[sc], marker=markers[mc], markersize=markersize, color=colors[cc])

    # mark_inset(ax_all, axins, loc1=3, loc2=4, fc="none", ec="0.5")
    leg = ax_all.legend(fontsize=lfontsize, ncol=lcols, markerscale=lmarkerscale)
    for line in leg.get_lines():
        line.set_linewidth(llinewidth)

    fig_all.savefig(output_folder / exp_name / f'{exp_name}-all.jpg', bbox_inches='tight', pad_inches=0)
    plt.close(fig_all)


def plot_entropy(data, exp_name, output_folder, rename_map, base_line='GPHH',
                 markersize=15, linewidth=19, lcols=2, target_alg='DDMT', legend=False,
                 lfontsize=95, lmarkerscale=1.2, llinewidth=20, figsize=(60, 32), data_name='Entropy', ax=None):
    print(f"Plotting {data_name}: {exp_name}")
    markers = ["", ',', 'v', 's', '*', 'd', '^', '<', '>', 'p', 'h', 'H', '+', 'x', 'D', '|', '_']
    line_styles = ['--', '-.', ':']
    colors = ['#ff7f0e', '#2ca02c', '#9467bd', '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf', '#1f77b4']
    sc = 0  # line style counter
    mc = 0  # marker counter
    cc = -1  # color counter

    if not ax:
        fig_all = plt.figure(figsize=figsize)
        ax_all = fig_all.add_subplot(111)
        ax_all.set_xlabel('Generation', fontdict={'fontsize': 120})
        ax_all.set_ylabel(data_name, fontdict={'fontsize': 120})
    else:
        ax_all = ax
        fig_all = None

    ax_all.tick_params(axis='both', which='major', labelsize=100)

    for algorithm in sort_algorithms(data):
        df = data[algorithm]
        df = df.mean(axis=1)
        mean_data = df.to_list()

        if output_folder and not Path(output_folder / exp_name).exists():
            Path(output_folder / exp_name).mkdir(parents=True)

        label = rename_alg(algorithm, rename_map)
        if algorithm == base_line:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth, markersize=markersize,
                        label="GPHH", color='k')
        elif algorithm == target_alg:
            ax_all.plot(range(1, len(mean_data) + 1), mean_data, linewidth=linewidth, markersize=markersize,
                        label=algorithm, color='#d62728', marker='o', linestyle='-')
        else:
            sc += 1
            sc %= len(line_styles)
            mc += 1
            mc %= len(markers)
            cc += 1
            cc %= len(colors)

            ax_all.plot(range(1, len(mean_data) + 1), mean_data, label=label, linewidth=linewidth,
                        linestyle=line_styles[sc], marker=markers[mc], markersize=markersize, color=colors[cc])

    # mark_inset(ax_all, axins, loc1=3, loc2=4, fc="none", ec="0.5")
    if legend:
        leg = ax_all.legend(fontsize=lfontsize, ncol=lcols, markerscale=lmarkerscale)
        for line in leg.get_lines():
            line.set_linewidth(llinewidth)

    if output_folder and fig_all:
        fig_all.savefig(output_folder / exp_name / f'{data_name}.jpg', bbox_inches='tight', pad_inches=0)

    if not fig_all:
        plt.close(fig_all)


def plot_twinx(df, save_file, markersize=60, linewidth=6, lcols=2,
               lfontsize=95, lmarkerscale=1.2, llinewidth=15):
    df = df.reindex(sorted(df.index, key=natural_keys), axis=0)
    fig = plt.figure(figsize=(60, 30))
    ax1 = fig.add_subplot(111)
    ax1.set_ylabel(df.columns[0], fontdict={'fontsize': 120})
    ax1.set_xticks(np.arange(len(df.index)))
    ax1.tick_params(axis='both', labelsize=80)
    df.iloc[:, 0].plot(ax=ax1, style='r*:', label=df.columns[0], linewidth=linewidth, markersize=markersize + 5)
    for label in ax1.get_xticklabels():
        label.set_rotation(90)

    xticks = ax1.get_xticks()  # .xticks()
    xmin = (2.9 * xticks[0] - xticks[1]) / 2.
    xmax = (3 * xticks[-1] - xticks[-2]) / 2.
    ax1.set_xlim(xmin, xmax)
    ax1.set_xticks(xticks)
    ax1.grid(True)

    ax2 = ax1.twinx()
    ax2.set_ylabel(df.columns[1], fontdict={'fontsize': 120})
    ax2.tick_params(axis='y', labelsize=80)
    df.iloc[:, 1].plot(ax=ax2, style='go:', label=df.columns[1], linewidth=linewidth, markersize=markersize)
    h1, l1 = ax1.get_legend_handles_labels()
    h2, l2 = ax2.get_legend_handles_labels()
    leg = ax2.legend(h1 + h2, l1 + l2, loc='upper right', fontsize=lfontsize, ncol=1, markerscale=lmarkerscale)
    for line in leg.get_lines():
        line.set_linewidth(llinewidth)
    xticks = ax2.get_xticks()  # .xticks()
    xmin = (3 * xticks[0] - xticks[1]) / 2.
    xmax = (3 * xticks[-1] - xticks[-2]) / 2.
    ax2.set_xlim(xmin, xmax)
    ax2.set_xticks(xticks)

    fig.savefig(save_file, bbox_inches='tight', pad_inches=0)
