import statistics
import json
import csv
from pathlib import Path

from scipy import stats

from .core import should_process
from .core import get_test_fitness
from .core import sort_algorithms
from .core import rename_alg

def update_wdl(exp_data, wdltable, rename_map, wdltable_exp_names, exp_name, *,
               base_line='WithoutKnowledge', num_generations=50):
    """
    Computes the Win-Draw-Loss of experiment results given with the experiement data.
    The function does not return any value but updates the input argument 'wdltable'
    """

    # wins = 0
    # draws = 0
    # loses = 0
    
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
        pval_wo_wil = stats.wilcoxon(list(exp_data[base_line][generation].values())[:alg_len], list(exp_data[alg][generation].values()))[1]

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

def wdl(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map, *, base_line='WithoutKnowledge', num_generations=50, dump_file=Path('./wdl')):
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
        update_wdl(exp_data, wdltable, rename_map, wdltable_exp_name, exp, base_line=base_line, num_generations=num_generations)

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
        update_wdl(exp_data, wdltable, rename_map, wdltable_exp_name, exp, base_line=base_line, num_generations=num_generations)

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

def wdl2_exp_name(): pass

def summary(dirbase, experiments, inclusion_filter, exclusion_filter, rename_map,
            *, num_generations=50, dump_file=Path('./wdl'), baseline_alg):
    summary_table = {}
    test_data_table = {}
    gen = 49
    for exp in experiments: 
        print('Summary: processing', dirbase / exp)
        test_fitness = get_test_fitness(dirbase / exp, inclusion_filter, exclusion_filter, num_generations=num_generations)
        test_data_table[exp] = test_fitness
        if not exp in summary_table:
            summary_table[exp] = {}
        for alg in sort_algorithms(test_fitness):
            mini = round(min(list(test_fitness[alg][gen].values())), 2)
            maxi = round(max(list(test_fitness[alg][gen].values())), 2)
            mean = round(statistics.mean(list(test_fitness[alg][gen].values())), 2)
            std = round(statistics.stdev(list(test_fitness[alg][gen].values())), 2)
            median = round(statistics.median(list(test_fitness[alg][gen].values())), 2)
            
            if alg == baseline_alg:
                pval_wo_wil = '--'
                pval_wo_t = '--'
            else: 
                if len(list(test_fitness[baseline_alg][gen].values())) != len(list(test_fitness[alg][gen].values())):
                    alg_len = len(list(test_fitness[alg][gen].values()))
                    print("Warning: Len of ", alg, "(", alg_len, ") is not 30. Test is done for this length.")
                    pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][gen].values())[:alg_len], list(test_fitness[alg][gen].values()))[1]
                    pval_wo_t = stats.ttest_rel(list(test_fitness[baseline_alg][gen].values())[:alg_len], list(test_fitness[alg][gen].values()))[1]
                    # pval_wo_t = -1
                else:
                    pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]
                    pval_wo_t   = stats.ttest_rel(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]

            if isinstance(pval_wo_wil, float):
                pval_wo_wil = round(pval_wo_wil, 2)
                pval_wo_t = round(pval_wo_t, 2)
            if not alg in summary_table[exp]:
                summary_table[exp][alg] = {}
            summary_table[exp][alg]['Average'] = mean
            summary_table[exp][alg]['Stdev'] = std
            summary_table[exp][alg]['min'] = mini
            summary_table[exp][alg]['max'] = maxi
            summary_table[exp][alg]['pval_wo_t'] = pval_wo_t
            summary_table[exp][alg]['pval_wo_wil'] = pval_wo_wil

    return summary_table, test_data_table

def save_summary(summary_table, output_folder, rename_map):
    output_folder = Path(output_folder)
    if not output_folder.exists(): 
        output_folder.mkdir(parents=True)
    csv_file = open(output_folder / 'summary_stats.csv', 'w')
    cvs_table = ''
    latex_table = ''
    
    latex_header = r'\begin{table}[]' + '\n' \
                 + r'\resizebox{\columnwidth}{!}{%'  + '\n' \
                 + r'\begin{tabular}{} \hline'  + '\n'
    latex_file = open(output_folder / 'summary_stats.tex', 'w')
    header = []
    hc = 0
    header.append(',')
    for exp in summary_table:
        csv_line = f'{exp},'
        latex_line = f'{exp} & '.replace(':gen_50', r'').replace('.vs', '-').replace('.vt', '').replace('.gdb', '').replace('gdb', 'G')
        for alg in sort_algorithms(summary_table[exp]):
            ren_alg = rename_alg(alg, rename_map)
            header[hc] += f'"{ren_alg}"' + ','
            mean = summary_table[exp][alg]['Average'] 
            std = summary_table[exp][alg]['Stdev']
            mini = summary_table[exp][alg]['min'] 
            maxi = summary_table[exp][alg]['max'] 
            pval_wo_t = summary_table[exp][alg]['pval_wo_t'] 
            pval_wo_wil = summary_table[exp][alg]['pval_wo_wil'] 

            csv_line += f'{mean} + {std} + {pval_wo_t},'
            if isinstance(pval_wo_wil, float) and  pval_wo_wil < 0.05: 
                latex_line += r'\textbf{' + f'{mean}' + r'$\pm$' + f'{std}' + '} &' 
            else: 
                latex_line += f' {mean}' + r'$\pm$' + f'{std} &' 
        hc += 1
        header.append('')
        # csv_file.write(line.strip(',') +  '\n')
        cvs_table += (csv_line.strip(',') +  '\n')
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

def save_stats(test_fitness, exp_name, output_folder, *, rename_map, gen = 49, round_results = True, baseline_alg = 'WithoutKnowledge'):
    if not Path(output_folder / exp_name).exists(): 
        Path(output_folder / exp_name).mkdir(parents=True)

    csv_file = open(output_folder / f'{exp_name + "/" + "train_stats"}.csv', 'w')
    latex_file = open(output_folder / f'{exp_name + "/" + "train_stats"}.tex', 'w')

    experiment = exp_name
    experiment = r'\textbf{' + experiment.replace('gen_50', '').replace('.', ' ') + '}'
    experiment = r'\multicolumn{3}{c}{' + experiment + r'}'
    csv_file.write('Algorithm, Mean, Stdev, Min, Max, median, p_wi, p_t\n')
    latex_file.write(r'\begin{table}[]' + '\n'
                       + r'\resizebox{\columnwidth}{!}{%'  + '\n'
                       + r'\begin{tabular}{llll} \hline'  + '\n'
                       + f'Scenario: & {experiment}' + r'\\ \hline' + '\n'
                       + r'Algorithm & Mean(Stdev) & Min & Max & $p_{WT}$ & $p_{T}$         \\ \hline' + '\n')

    for alg in sort_algorithms(test_fitness):
        mini = min(list(test_fitness[alg][gen].values()))
        maxi = max(list(test_fitness[alg][gen].values()))
        mean = statistics.mean(list(test_fitness[alg][gen].values()))
        std =statistics.stdev(list(test_fitness[alg][gen].values()))
        median = statistics.median(list(test_fitness[alg][gen].values()))
                
        if alg == baseline_alg:
            pval_wo_wil = '--'
            pval_wo_t = '--'
        else: 
            if len(list(test_fitness[baseline_alg][gen].values())) != len(list(test_fitness[alg][gen].values())):
                alg_len = len(list(test_fitness[alg][gen].values()))
                print("Warning: Len of ", alg, "(", alg_len, ") is not 30. Test is done for this length.")
                pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][gen].values())[:alg_len], list(test_fitness[alg][gen].values()))[1]
                # pval_wo_t = stats.ttest_rel(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]
                pval_wo_t = -1
            else:
                pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]
                pval_wo_t   = stats.ttest_rel(list(test_fitness[baseline_alg][gen].values()), list(test_fitness[alg][gen].values()))[1]

        if round_results:
            mini = round(mini, 2)
            maxi = round(maxi, 2)
            mean = round(mean, 2)
            std = round(std, 2)
            median = round(median, 2)
            if isinstance(pval_wo_wil, float):
                pval_wo_wil = round(pval_wo_wil, 2)
                pval_wo_t = round(pval_wo_t, 2)

        latex_file.write(f'{rename_alg(alg, rename_map)} & {mean}({std}) & {mini} & {maxi} & {pval_wo_wil} & {pval_wo_t} \\\\\n')
        csv_file.write(f'"{rename_alg(alg, rename_map)}", {mean}, {std}, {mini}, {maxi}, {median}, {pval_wo_wil}, {pval_wo_t} \n')
    
    csv_file.close()
    latex_file.write(r'\hline \end{tabular}' + '\n'
                    + r'}' + '\n'
                    + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
                    + r'\end{table}')
    latex_file.close()


def improvements(test_fitnesses, rename_map, num_generations = 50, dump_file=Path('./improvements'), base_line='WithoutKnowledge'):
    """
    Calculates the improvement in training time achieved over baseline algorithm.
    """
    def find_improvement(fitness, *, base_line='WithoutKnowledge', generations=num_generations):
        retval = {} 
        for alg in fitness:
            if alg == base_line: 
                continue
            retval[alg] = []
            for i in range(generations): # Delete -1. Added that to test something and it should be removed as soon as the something is done.
                # if len(list(fitness[base_line][generations-1].values())) !=  len(list(fitness[alg][i].values())):
                #     print('Imbalanced len:', alg, 'at generation:', i)
                #     continue

                pval_wo_wil = stats.wilcoxon(list(fitness[base_line][generations-1].values())[:len(fitness[alg][i].values())], list(fitness[alg][i].values()))[1]
                if pval_wo_wil >= 0.05:
                    # The performance is similar now. 
                    retval[alg].append(0)
                if pval_wo_wil < 0.05:
                    mean_alg = statistics.mean(list(fitness[alg][i].values()))
                    mean_bse = statistics.mean(list(fitness[base_line][generations-1].values()))
                    if mean_alg > mean_bse:
                        # Oh oh
                        retval[alg].append(-1) # = f'{i} failed.'
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
                if improvement[i] >= 0 :
                    improved += 1
                    if improved == 3: 
                        # retval[alg] = 1 -  (i + 1 - improved)/len(improvement)
                        perc = 1 -  (i + 1 - improved)/len(improvement)
                        improve_perc_table[alg] += perc
                        improve_table[alg].append(perc)
                        break
                else: 
                    improved = 0
        # return retval

    imp_perc_table = {}
    imp_table = {}
    for exp in test_fitnesses:
        print('\n', exp)
        test_fitness = test_fitnesses[exp]
        imps = find_improvement(test_fitness, base_line=base_line)
        print(imps)
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
