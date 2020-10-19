import statistics
import json
import csv
from pathlib import Path

from promise.utils import deprecated
from scipy import stats

from .core import should_process, rename_exp
from .core import get_test_fitness
from .core import sort_algorithms
from .core import rename_alg

import stac

import scipy.stats as ss
import scikit_posthocs as sp


def update_wdl(exp_data, wdltable, rename_map, wdltable_exp_names, exp_name, *,
               base_line='WithoutKnowledge', num_generations=50):
    """
    Computes the Win-Draw-Loss of experiment results given with the experiement data.
    The function does not return any value but updates the input argument 'wdltable'
    """

    # wins = 0
    # draws = 0
    # loses = 0

    if not base_line:
        return

    generation = num_generations - 1

    bmean = statistics.mean(list(exp_data[base_line][generation].values()))
    for alg in exp_data:
        if alg == base_line:
            continue

        renamed_alg = rename_alg(alg, rename_map)

        if not renamed_alg in wdltable:
            # wins, draws, losses, missing
            wdltable[renamed_alg] = [0, 0, 0, 0]
        if renamed_alg not in wdltable_exp_names:
            # wins, draws, losses, missing
            wdltable_exp_names[renamed_alg] = [[], [], [], []]

        try:
            mean = statistics.mean(list(exp_data[alg][generation].values()))
            # base_mean = statistics.mean(list(exp_data[base_line][generation].values()))
        except KeyError as e:
            print(alg, e)
            wdltable[renamed_alg][3] += 1
            wdltable_exp_names[renamed_alg][3].append(exp_name)
            continue
        if len(list(exp_data[base_line][generation].values())) != len(list(exp_data[alg][generation].values())):
            print("Len of ", alg, "(", len(list(exp_data[alg][generation].values())), ") is not 30.")
            wdltable[renamed_alg][3] += 1
            wdltable_exp_names[renamed_alg][3].append(exp_name)
            # continue
        alg_len = len(list(exp_data[alg][generation].values()))
        pval_wo_wil = stats.wilcoxon(list(exp_data[base_line][generation].values())[:alg_len],
                                     list(exp_data[alg][generation].values()))[1]

        if pval_wo_wil < 0.05:
            if mean < bmean:
                wdltable[renamed_alg][0] += 1
                wdltable_exp_names[renamed_alg][0].append(exp_name)
            else:
                wdltable[renamed_alg][2] += 1
                wdltable_exp_names[renamed_alg][2].append(exp_name)
        else:
            wdltable[renamed_alg][1] += 1
            wdltable_exp_names[renamed_alg][1].append(exp_name)


def wdl(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map, *, base_line='WithoutKnowledge',
        num_generations=50, dump_file=Path('./wdl')):
    """
    Computes the Win-Draw-Loss statistics of algorithms compared to a baseline. The function saves
    the stats to a JSON and CSV files and also returns them. This function reads the fitness values
    from the 'dirbase' location.
    Usage: wdl(dirbase, experiments, inclusion_filter, exclusion_filter, dump_file=output_folder / 'wdl', rename_map=rename_map)
    """
    wdltable = {}
    wdltable_exp_name = {}

    for exp in experiments:
        print('WDL: processing', dirbase / exp)
        exp_data = get_test_fitness(dirbase / exp, inclusion_filter, exclusion_filter, num_generations=num_generations)
        update_wdl(exp_data, wdltable, rename_map, wdltable_exp_name, exp, base_line=base_line,
                   num_generations=num_generations)

    with open(str(dump_file) + '.json', 'w') as file:
        json.dump(wdltable, file, indent=4)
        print('WDL: results saved to:', dump_file)

    with open(str(dump_file) + '-expnames.json', 'w') as file:
        json.dump(wdltable_exp_name, file, indent=4)

    with open(str(dump_file) + '.csv', 'w', newline="") as csv_file:
        writer = csv.writer(csv_file)
        for key, value in wdltable.items():
            writer.writerow([key, *value])

    return wdltable, wdltable_exp_name


def wdl2(experiment_data, rename_map, *, base_line='WithoutKnowledge', num_generations=50, dump_file=Path('./wdl')):
    """
    Computes the Win-Draw-Loss statistics of algorithms compared to a baseline. The function saves
    the stats to a JSON and CSV files and also returns them. The function does not read fitness data
    from files and treats 'experiment_data' as a dictionary that contains fitness information for
    each experiment.
    Usage: wdl2(experiment_data, dump_file=output_folder / 'wdl', rename_map=rename_map)
    """
    wdltable = {}
    wdltable_exp_name = {}

    for exp in experiment_data:
        print('WDL2: processing', exp)
        exp_data = experiment_data[exp]
        update_wdl(exp_data, wdltable, rename_map, wdltable_exp_name, exp, base_line=base_line,
                   num_generations=num_generations)

    with open(str(dump_file) + '.json', 'w') as file:
        json.dump(wdltable, file, indent=4)
        print('WDL2: results saved to:', dump_file)

    with open(str(dump_file) + '-expnames.json', 'w') as file:
        json.dump(wdltable_exp_name, file, indent=4)
        # print('WDL2: results saved to:', dump_file)

    with open(str(dump_file) + '.csv', 'w', newline="") as csv_file:
        writer = csv.writer(csv_file)
        for key, value in wdltable.items():
            writer.writerow([key, *value])

    return wdltable, wdltable_exp_name


import pandas as pd


def friedman_test2(test_fitness, ignore_list=[]):
    if len(test_fitness) < 3:
        return -1, [], []
    data = []
    alg_names = []
    for alg in test_fitness:
        if alg in ignore_list:
            continue
        data.append(list(test_fitness[alg]))
        alg_names.append(alg)

    _, p, rank, pivot = stac.nonparametric_tests.friedman_test(*data)
    post = {}
    ctr = 0
    for alg in test_fitness:
        if alg in ignore_list:
            continue
        post[alg] = (pivot[ctr])
        ctr = ctr + 1

    names, _, _, adjusted_pval = stac.nonparametric_tests.nemenyi_multitest(post)

    return p, list(zip(alg_names, rank)), list(zip(names, adjusted_pval))


def summary(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map,
            *, num_generations=50, dump_file=Path('./wdl'), baseline_alg):
    def summarise(test_fit):
        mini = round(min(list(test_fit.values())), 2)
        maxi = round(max(list(test_fit.values())), 2)
        mean = round(statistics.mean(list(test_fit.values())), 2)
        std = round(statistics.stdev(list(test_fit.values())), 2)
        median = round(statistics.median(list(test_fit.values())), 2)

        return mini, maxi, mean, std, median

    def pval(fitness, alg, generation):
        if not baseline_alg or alg == baseline_alg:
            pval_wo_wil = '--'
            pval_wo_t = '--'
        else:
            if len(list(fitness[baseline_alg][generation].values())) != len(list(fitness[alg][generation].values())):
                alg_len = len(list(fitness[alg][generation].values()))
                print("Warning: Len of ", alg, "(", alg_len, ") is not 30. Test is done for this length.")
                try:
                    pval_wo_wil = stats.wilcoxon(list(fitness[baseline_alg][generation].values())[:alg_len],
                                                 list(fitness[alg][generation].values()))[1]
                    pval_wo_t = stats.ttest_rel(list(fitness[baseline_alg][generation].values())[:alg_len],
                                                list(fitness[alg][generation].values()))[1]
                except ValueError:
                    pval_wo_t = -1
                    pval_wo_wil = -1
            else:
                pval_wo_wil = \
                    stats.wilcoxon(list(fitness[baseline_alg][generation].values()),
                                   list(fitness[alg][generation].values()))[1]
                pval_wo_wil = round(pval_wo_wil, 2)
                pval_wo_t = \
                    stats.ttest_rel(list(fitness[baseline_alg][generation].values()),
                                    list(fitness[alg][generation].values()))[
                        1]
                pval_wo_t = round(pval_wo_t, 2)

        return pval_wo_wil, pval_wo_t

    def friedman_test(fitness, generation):
        if len(fitness) < 3:
            return -1, [], []
        data = []
        alg_names = []
        for algorithm in fitness:
            # if alg == baseline or len(test_fitness[alg][gen].values()) != 30:
            if len(fitness[algorithm][generation].values()) != 30:
                continue
            data.append(list(fitness[algorithm][generation].values()))
            alg_names.append(algorithm)

        _, p, rank, pivot = stac.nonparametric_tests.friedman_test(*data)
        post = {}
        ctr = 0
        for alg in fitness:
            # if alg == baseline or len(test_fitness[alg][gen].values()) != 30:
            if len(fitness[alg][generation].values()) != 30:
                continue
            post[alg] = (pivot[ctr])
            ctr = ctr + 1

        names, _, _, adjusted_pval = stac.nonparametric_tests.nemenyi_multitest(post)

        return p, list(zip(alg_names, rank)), list(zip(names, adjusted_pval))

    test_summary_table = {}
    best_summary_table = {}
    test_data_table = {}
    best_data_table = {}
    test_fri_table = {}
    best_fri_table = {}
    gen = 49

    def do_summary(fitness, generation):
        smmry_table = {}
        fri_table = friedman_test(fitness, generation)
        for algo in best_fitness:
            mini, maxi, mean, std, median = summarise(fitness[algo][generation])

            pval_wo_wil, pval_wo_t = pval(fitness, algo, generation)

            if algo not in smmry_table:
                smmry_table[algo] = {}
            smmry_table[algo]['Average'] = mean
            smmry_table[algo]['Stdev'] = std
            smmry_table[algo]['min'] = mini
            smmry_table[algo]['max'] = maxi
            smmry_table[algo]['median'] = median
            smmry_table[algo]['pval_wo_t'] = pval_wo_t
            smmry_table[algo]['pval_wo_wil'] = pval_wo_wil
        return smmry_table, fri_table

    for exp in experiments:
        print('Summary: processing', dirbase / exp)
        test_fitness, best_fitness = get_test_fitness(dirbase / exp, inclusion_filter, exclusion_filter,
                                                      num_generations=num_generations)
        test_data_table[exp] = test_fitness
        best_data_table[exp] = best_fitness
        test_summary_table[exp], test_fri_table[exp] = do_summary(test_fitness, gen)
        best_summary_table[exp], best_fri_table[exp] = do_summary(best_fitness, -1)

    return test_summary_table, test_data_table, test_fri_table, best_summary_table, best_data_table, best_fri_table


def save_stat2(summary_table, output_folder, rename_map, fried_table):
    def calc_wdl(fried, base):
        win, draw, loss = {}, {}, {}
        for xp in fried:
            # if fried[xp][0] >= 0.05:
            #     d = d + 1
            #     continue
            for comparison in fried[xp][2]:
                if base not in comparison[0]:
                    continue
                print(comparison)
                pval = comparison[1]
                vs = comparison[0].replace(base, '').replace(' vs ', '')
                ren_vs = rename_alg(vs, rename_map)
                if pval >= 0.05:
                    draw[ren_vs] = draw.get(ren_vs, 0) + 1
                    continue
                if summary_table[xp][base]['Average'] <= summary_table[xp][vs]['Average']:
                    win[ren_vs] = win.get(ren_vs, 0) + 1
                else:
                    loss[ren_vs] = loss.get(ren_vs, 0) + 1

        return win, draw, loss

    def calc_wdl2(fried, alg1, alg2):
        """
        Compares alg1 against alg2
        :param fried: the friedman table
        :param alg1: the baseline algorithm
        :param alg2: the algorithm that alg1 is compared against.
        :return: (wins, draws, losses) of alg1 against alg2
        """
        win, draw, loss = 0, 0, 0
        for xp in fried:
            # if fried[xp][0] >= 0.05:
            #     d = d + 1
            #     continue
            for comparison in fried[xp][2]:
                if alg1 not in comparison[0]:
                    continue
                if alg2 not in comparison[0]:
                    continue
                pval = comparison[1]
                # ren_alg1 = rename_alg(alg1, rename_map)
                # ren_alg2 = rename_alg(alg2, rename_map)
                if pval >= 0.05:
                    draw = draw + 1
                    continue
                if summary_table[xp][alg1]['Average'] <= summary_table[xp][alg2]['Average']:
                    win = win + 1
                else:
                    loss = loss + 1

        return win, draw, loss

    output_folder = Path(output_folder)
    if not output_folder.exists():
        output_folder.mkdir(parents=True)

    all_averages = None
    all_pvals = None
    all_stds = None
    all_ranks = pd.DataFrame()
    for exp in summary_table:
        exp_summary = {rename_alg(alg, rename_map): summary_table[exp][alg] for alg in
                       sort_algorithms(summary_table[exp])}
        df = pd.DataFrame(exp_summary)

        ren_exp = rename_exp(exp)
        save_path = output_folder / ren_exp
        if not save_path.exists():
            save_path.mkdir(parents=True)
        df = df.T
        ranks = fried_table[exp][1]
        rnks = {rename_alg(rank[0], rename_map): rank[1] for rank in ranks}
        df["Rank"] = pd.Series(rnks)
        all_ranks[rename_exp(exp)] = pd.Series(rnks)
        df.to_csv(save_path / f'{ren_exp}_summary.csv')
        df.to_latex(save_path / f'{ren_exp}_summary.tex')

        exp_posthoc_dict = dict(map(lambda x: (rename_alg(x[0], rename_map), x[1]), fried_table[exp][2]))
        exp_posthoc_dict["Friedman"] = fried_table[exp][0]
        exp_posthoc_pvals = pd.DataFrame({'P-Value': exp_posthoc_dict})
        exp_posthoc_pvals.to_csv(save_path / f'{ren_exp}_post_hoc.csv')
        exp_posthoc_pvals.to_latex(save_path / f'{ren_exp}_post_hoc.tex')

        if all_averages is None:
            all_averages = df.Average.to_frame(ren_exp)
            all_pvals = df.pval_wo_wil.to_frame(ren_exp)
            all_stds = df.Stdev.to_frame(ren_exp)
        else:
            all_averages[ren_exp] = df.Average.to_frame(ren_exp)
            all_pvals[ren_exp] = df.pval_wo_wil.to_frame(ren_exp)
            all_stds[ren_exp] = df.Stdev.to_frame(ren_exp)

    # averages = all_averages.T.to_dict('list')
    # fried_average = friedman_test2(averages, [])
    all_averages['AverageRank'] = all_ranks.T.mean()
    all_ranks['AverageRank'] = all_ranks.T.mean()

    algs = set()
    for exp in summary_table:
        for alg in summary_table[exp]:
            algs.add(alg)
    algs = list(algs)
    wdl_matrix = {}
    for i in range(len(algs)):
        for j in range(i + 1, len(algs)):
            w, d, l = calc_wdl2(fried_table, algs[i], algs[j])
            algi = rename_alg(algs[i], rename_map)
            algj = rename_alg(algs[j], rename_map)
            if algi not in wdl_matrix:
                wdl_matrix[algi] = {}
            wdl_matrix[algi][algj] = (w, d, l)
    wdl_df = pd.DataFrame(wdl_matrix).fillna('--').T # Row is compared agaist column
    wdl_df.to_csv(output_folder / 'fried_wdl.csv')
    wdl_df.to_latex(output_folder / 'fried_wdl.tex')

    # if baseline:
    #     wins, draws, losses = calc_wdl(fried_table, baseline)
    #     wdl_df = pd.DataFrame({'W': wins, 'D': draws, 'L': losses, 'AverageRank': all_ranks.T.mean()}).fillna(0)
    #     wdl_df.to_csv(output_folder / 'fried_wdl.csv')
    #     wdl_df.to_latex(output_folder / 'fried_wdl.tex')

    latex_df = all_averages.astype(str).add(r"$\pm$" + all_stds.astype(str), fill_value="")
    # for i in all_pvals.index:
    #     if i == baseline:
    #         continue
    #     for j in all_pvals.loc[i].index:
    #         if all_pvals.loc[i][j] == '--' or all_pvals.loc[i][j] >= 0.05:
    #             continue
    #         if all_averages.loc[i][j] < all_averages.loc[baseline][j]:
    #             latex_df.loc[i][j] = r'\textbf{' + latex_df.loc[i][j] + '}'
    #         else:
    #             latex_df.loc[i][j] = r'\underline{\textit{' + latex_df.loc[i][j] + '}}'
    latex_df.T.to_latex(output_folder / 'sum.tex', escape=False)
    csv_df = all_averages.astype(str).add(r"(" + all_stds.astype(str) + ", " + all_pvals.astype(str) + r")",
                                          fill_value="")
    csv_df.T.to_csv(output_folder / "sum.csv")
    all_ranks.to_latex(output_folder / 'ranks.tex')
    all_ranks.to_csv(output_folder / 'ranks.csv')


@deprecated('save_summary is deprecated in favor of save_stat2')
def save_summary(summary_table, output_folder, rename_map):
    output_folder = Path(output_folder)
    if not output_folder.exists():
        output_folder.mkdir(parents=True)
    csv_file = open(output_folder / 'summary_stats.csv', 'w')
    cvs_table = ''
    latex_table = ''

    latex_header = r'\begin{table}[]' + '\n' \
                   + r'\resizebox{\columnwidth}{!}{%' + '\n' \
                   + r'\begin{tabular}{} \hline' + '\n'
    latex_file = open(output_folder / 'summary_stats.tex', 'w')
    header = []
    hc = 0
    header.append(',')
    for exp in summary_table:
        csv_line = f'{exp},'
        latex_line = f'{exp} & '.replace(':gen_50', r'').replace('.vs', '-').replace('.vt', '').replace('.gdb',
                                                                                                        '').replace(
            'gdb', 'G')
        for alg in sort_algorithms(summary_table[exp]):
            ren_alg = rename_alg(alg, rename_map)
            header[hc] += f'"{ren_alg}"' + ','
            mean = summary_table[exp][alg]['Average']
            std = summary_table[exp][alg]['Stdev']
            mini = summary_table[exp][alg]['min']
            maxi = summary_table[exp][alg]['max']
            pval_wo_t = summary_table[exp][alg]['pval_wo_t']
            pval_wo_wil = summary_table[exp][alg]['pval_wo_wil']

            # csv_line += f'"({mini}, {maxi}, {mean}, {std}, {pval_wo_t})",'
            csv_line += f'"{mean}({std}, {pval_wo_wil})",'
            if isinstance(pval_wo_wil, float) and pval_wo_wil < 0.05:
                latex_line += r'\textbf{' + f'{mean}' + r'$\pm$' + f'{std}' + '} &'
            else:
                latex_line += f' {mean}' + r'$\pm$' + f'{std} &'
        hc += 1
        header.append('')
        # csv_file.write(line.strip(',') +  '\n')
        cvs_table += (csv_line.strip(',') + '\n')
        latex_table += (latex_line.strip('&') + r'\\' + ' \n')
    csv_file.write(header[0].rstrip(',') + '\n' + cvs_table + '\n')
    csv_file.close()

    latex_file.write(latex_header + header[0].rstrip(',').replace(',', ' & ').replace('_', r'\_').replace('"', '')
                     + '\\\\ \\hline \n'
                     + latex_table
                     + r'\hline \end{tabular}' + '\n'
                     + r'}' + '\n'
                     + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
                     + r'\end{table}')
    latex_file.close()


def save_stats(test_fitness, exp_name, output_folder, *, rename_map, gen=49, round_results=True,
               baseline_alg='WithoutKnowledge'):
    if not Path(output_folder / exp_name).exists():
        Path(output_folder / exp_name).mkdir(parents=True)

    csv_file = open(output_folder / f'{exp_name + "/" + "train_stats"}.csv', 'w')
    latex_file = open(output_folder / f'{exp_name + "/" + "train_stats"}.tex', 'w')

    experiment = exp_name
    experiment = r'\textbf{' + experiment.replace('gen_50', '').replace('.', ' ') + '}'
    experiment = r'\multicolumn{3}{c}{' + experiment + r'}'
    csv_file.write('Algorithm, Mean, Stdev, Min, Max, median, p_wi, p_t\n')
    latex_file.write(r'\begin{table}[]' + '\n'
                     + r'\resizebox{\columnwidth}{!}{%' + '\n'
                     + r'\begin{tabular}{llll} \hline' + '\n'
                     + f'Scenario: & {experiment}' + r'\\ \hline' + '\n'
                     + r'Algorithm & Mean(Stdev) & Min & Max & $p_{WT}$ & $p_{T}$         \\ \hline' + '\n')

    for alg in sort_algorithms(test_fitness):
        mini = min(list(test_fitness[alg][gen].values()))
        maxi = max(list(test_fitness[alg][gen].values()))
        mean = statistics.mean(list(test_fitness[alg][gen].values()))
        std = statistics.stdev(list(test_fitness[alg][gen].values()))
        median = statistics.median(list(test_fitness[alg][gen].values()))

        if not baseline_alg or alg == baseline_alg:
            pval_wo_wil = '--'
            pval_wo_t = '--'
        else:
            if len(list(test_fitness[baseline_alg][gen].values())) != len(list(test_fitness[alg][gen].values())):
                alg_len = len(list(test_fitness[alg][gen].values()))
                print("Warning: Len of ", alg, "(", alg_len, ") is not 30. Test is done for this length.")
                pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][gen].values())[:alg_len],
                                             list(test_fitness[alg][gen].values()))[1]
                # pval_wo_t = stats.ttest_rel(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]
                pval_wo_t = -1
            else:
                pval_wo_wil = \
                    stats.wilcoxon(list(test_fitness[baseline_alg][gen].values()),
                                   list(test_fitness[alg][gen].values()))[1]
                pval_wo_t = \
                    stats.ttest_rel(list(test_fitness[baseline_alg][gen].values()),
                                    list(test_fitness[alg][gen].values()))[
                        1]

        if round_results:
            mini = round(mini, 2)
            maxi = round(maxi, 2)
            mean = round(mean, 2)
            std = round(std, 2)
            median = round(median, 2)
            if isinstance(pval_wo_wil, float):
                pval_wo_wil = round(pval_wo_wil, 2)
                pval_wo_t = round(pval_wo_t, 2)

        latex_file.write(
            f'{rename_alg(alg, rename_map)} & {mean}({std}) & {mini} & {maxi} & {pval_wo_wil} & {pval_wo_t} \\\\\n')
        csv_file.write(
            f'"{rename_alg(alg, rename_map)}", {mean}, {std}, {mini}, {maxi}, {median}, {pval_wo_wil}, {pval_wo_t} \n')

    csv_file.close()
    latex_file.write(r'\hline \end{tabular}' + '\n'
                     + r'}' + '\n'
                     + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
                     + r'\end{table}')
    latex_file.close()


def improvements(test_fitnesses, rename_map, num_generations=50, dump_file=Path('./improvements'),
                 base_line='WithoutKnowledge'):
    """
    Calculates the improvement in training time achieved over baseline algorithm.
    """

    if not base_line:
        print("Improvement: Warning: The baseline algorithm is not specified. Returning an empty result")
        return {}

    def find_improvement(fitness, *, base_line='WithoutKnowledge', generations=num_generations):
        retval = {}
        for alg in fitness:
            if alg == base_line:
                continue
            retval[alg] = []
            for i in range(
                    generations):  # Delete -1. Added that to test something and it should be removed as soon as the something is done.
                # if len(list(fitness[base_line][generations-1].values())) !=  len(list(fitness[alg][i].values())):
                #     print('Imbalanced len:', alg, 'at generation:', i)
                #     continue

                pval_wo_wil = \
                    stats.wilcoxon(list(fitness[base_line][generations - 1].values())[:len(fitness[alg][i].values())],
                                   list(fitness[alg][i].values()))[1]
                if pval_wo_wil >= 0.05:
                    # The performance is similar now. 
                    retval[alg].append(0)
                if pval_wo_wil < 0.05:
                    mean_alg = statistics.mean(list(fitness[alg][i].values()))
                    mean_bse = statistics.mean(list(fitness[base_line][generations - 1].values()))
                    if mean_alg > mean_bse:
                        # Oh oh
                        retval[alg].append(-1)  # = f'{i} failed.'
                    else:
                        retval[alg].append(1)

        return retval

    def update_improvement_percentage(improvements, improve_perc_table, improve_table):
        # retval = {}
        for alg in improvements:
            # retval[alg] = 0
            if alg not in improve_perc_table:
                improve_perc_table[alg] = 0
                improve_table[alg] = []
            improvement = improvements[alg]
            improved = 0
            for i in range(len(improvement)):
                if improvement[i] >= 0:
                    improved += 1
                    if improved == 3:
                        # retval[alg] = 1 -  (i + 1 - improved)/len(improvement)
                        perc = 1 - (i + 1 - improved) / len(improvement)
                        improve_perc_table[alg] += perc
                        improve_table[alg].append(perc)
                        break
                else:
                    improved = 0
        # return retval

    imp_perc_table = {}
    imp_table = {}
    for exp in test_fitnesses:
        # print('\n', exp)
        test_fitness = test_fitnesses[exp]
        imps = find_improvement(test_fitness, base_line=base_line)
        # print(imps)
        update_improvement_percentage(imps, imp_perc_table, imp_table)

    retval = {}
    for alg in imp_perc_table:
        retval[rename_alg(alg, rename_map)] = imp_perc_table[alg] / len(test_fitnesses)
        print(rename_alg(alg, rename_map), imp_perc_table[alg] / len(test_fitnesses), '\n')

    with open(str(dump_file) + '.json', 'w') as file:
        json.dump(retval, file, indent=4)
        print('IMPR: results saved to:', dump_file)

    # print(imp_table)

    return retval
