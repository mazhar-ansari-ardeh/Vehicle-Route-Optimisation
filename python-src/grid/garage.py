from pathlib import Path
import os
import pandas as pd



# def missing_exps(basedir, experiments, inclusion_filter, exclusion_filter):
#     for exp in experiments:
#         experiment_path = Path(basedir) / exp
# def improv_hist(imp_table):
#     box_fig = plt.figure(figsize=(12,14))
#     box_ax = box_fig.add_subplot(111)
#     box_ax.set_title('algorithm')
#     box_ax.set_xlabel('Generation')
#     box_ax.set_ylabel('Fitness')

#     labels, data = imp_table.keys(), imp_table.values()

#     ren_labels = []
#     for label in labels:
#         label = rename_alg(label)
#         ren_labels.append(label)
#     box_ax.boxplot(data)
#     # box_ax.set_xticks(range(1, len(labels) + 1), ren_labels)
#     box_ax.set_xticklabels(ren_labels, rotation=90)
#     box_fig.savefig('improvements.jpg')
    

# def save_stats(test_fitness, experiment_path, round_results = False, baseline_alg = 'WithoutKnowledge'):
#     if not Path(output_folder / experiment_path.name).exists(): 
#         Path(output_folder / experiment_path.name).mkdir()

#     csv_file = open(output_folder / f'{experiment_path.name + "/" + "train_stats"}.csv', 'w')
#     latex_file = open(output_folder / f'{experiment_path.name + "/" + "train_stats"}.tex', 'w')

#     experiment = experiment_path.name
#     experiment = r'\textbf{' + experiment.replace('gen_50', '').replace('.', ' ') + '}'
#     experiment = r'\multicolumn{3}{c}{' + experiment + r'}'
#     csv_file.write('Algorithm, Mean, Stdev, Min, Max, median, p_wi\n')
#     latex_file.write(r'\begin{table}[]' + '\n'
#                        + r'\resizebox{\columnwidth}{!}{%'  + '\n'
#                        + r'\begin{tabular}{llll} \hline'  + '\n'
#                        + f'Scenario: & {experiment}' + r'\\ \hline' + '\n'
#                        + r'Algorithm & Mean(Stdev) & Min & Max & $p_{WT}$          \\ \hline' + '\n')

#     for alg in sort_algorithms(test_fitness):
#         if alg == 'FullTree_25' or alg == 'FullTree_50':
#             continue
#         # if len(list(test_fitness[alg][0].values())) < 30:
#         #     print("Imbalanced data: ", alg)
#         #     continue

#         mini = min(list(test_fitness[alg][49].values()))
#         maxi = max(list(test_fitness[alg][49].values()))
#         mean = statistics.mean(list(test_fitness[alg][49].values()))
#         std =statistics.stdev(list(test_fitness[alg][49].values()))
#         median = statistics.median(list(test_fitness[alg][49].values()))
#         # if alg == 'FullTree:tp_50:dup_false':
#         #     pval_full = '--'
#         # else:
#         #     pval_full = stats.wilcoxon(list(test_fitness['FullTree:tp_50:dup_false'][0].values()), list(test_fitness[alg][0].values()))[1]
        
#         if alg == baseline_alg:
#             pval_wo_wil = '--'
#             # pval_wo_t = '--'
#         else: 
#             # pval_wo = stats.mannwhitneyu(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]
#             if len(list(test_fitness[baseline_alg][49].values())) != len(list(test_fitness[alg][49].values())):
#                 print("Len of ", alg, "(", len(list(test_fitness[alg][49].values())), ") is not 30.")
#                 pval_wo_wil = -1
#                 # pval_wo_t = -1
#             else:
#                 pval_wo_wil = stats.wilcoxon(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]
#                 # pval_wo_t = stats.ttest_rel(list(test_fitness[baseline_alg][49].values()), list(test_fitness[alg][49].values()))[1]

#         if round_results:
#             mini = round(mini, 2)
#             maxi = round(maxi, 2)
#             mean = round(mean, 2)
#             std = round(std, 2)
#             median = round(median, 2)
#             if isinstance(pval_wo_wil, float):
#                 pval_wo_wil = round(pval_wo_wil, 2)
#             # if isinstance(pval_wo_t, float):
#             #     pval_wo_wil = round(pval_wo_t, 2)

#         # alg = rename_alg(alg)
#         latex_file.write(f'{alg} & {mean}({std}) & {mini} & {maxi} & {pval_wo_wil} \\\\\n')
#         csv_file.write(f'{alg}, {mean}, {std}, {mini}, {maxi}, {median}, {pval_wo_wil} \n')
#         # print(alg, mean, std, pval_full, pval_wo)
    
#     csv_file.close()
#     latex_file.write(r'\hline \end{tabular}' + '\n'
#                     + r'}' + '\n'
#                     + r'\caption{Means of 30 runs of best of GP generations for dataset \textbf{}, from  to  vehicles}' + '\n'
#                     + r'\end{table}')
#     latex_file.close()

# def compare_train():
#     '''
#     Compare the train performance of two generations 50 and 200.
#     '''
#     for exp in experiments:
#         alg = 'KnowledgeSource'
#         train_fitness = get_train_mean(dirbase / exp)
#         mean1 = round(statistics.mean(list(train_fitness[alg][49].values())), 2)
#         median1 = round(statistics.median(list(train_fitness[alg][49].values())), 2)

#         mean2 = round(statistics.mean(list(train_fitness[alg][199].values())), 2)
#         median2 = round(statistics.median(list(train_fitness[alg][199].values())), 2)

#         # std =statistics.stdev(list(test_fitness[alg][49].values()))
#         pval_wo = stats.wilcoxon(list(train_fitness[alg][49].values()), list(train_fitness[alg][199].values()))[1]
#         print(exp, mean1, mean2, median1, median2, round(pval_wo, 2))

# def compare_test():
#     '''
#     Compare the test performance of two generations 50 and 200.
#     '''

#     out = open('stats.csv', 'w')
#     out.write('dataset, average, median, min, max\n')
#     for exp in experiments:
#         print('\n')
#         alg = 'KnowledgeSource'
#         test_fitness = get_test_fitness(dirbase / exp, 200)
#         fit1 = list(test_fitness[alg][49].values())
#         fit2 = list(test_fitness[alg][199].values())

#         min1 = round(min(fit1), 2)
#         # min1_run = fit1.index(min(fit1))
#         min1_run = [i for i in test_fitness[alg][49] if test_fitness[alg][49][i] == min(fit1)][0]
#         min2 = round(min(fit2), 2)
#         min2_run = [i for i in test_fitness[alg][199] if test_fitness[alg][199][i] == min(fit2)][0]

#         max1 = round(max(fit1), 2)
#         max2 = round(max(fit2), 2)

#         mean1 = round(statistics.mean(fit1), 2)
#         mean2 = round(statistics.mean(fit2), 2)

#         median1 = round(statistics.median(fit1), 2)
#         median2 = round(statistics.median(fit2), 2)

#         pval_wo = stats.wilcoxon(fit1, fit2)[1]
#         print(exp)
#         print("mean:\t", mean1, mean2)
#         print("median:\t", median1, median2)
#         print("min:\t", f"{min1}({min1_run})", f"{min2}({min2_run})")
#         print("max:\t", max1, max2)
#         print("pval:\t", round(pval_wo, 2), "\n")

#         out.write(f'{exp}\n')
#         out.write(f'50, {mean1}, {median1}, {min1}, {max1}\n')
#         out.write(f'200, {mean2}, {median2}, {min2}, {max2}\n')
#         out.write(f'PVal, {round(pval_wo, 3)}\n')
#         out.write('\n')
#         out.flush()
#     out.close()

# def find_improvement(train_fitness, *, base_line='WithoutKnowledge:clear_true', generations=GENERATIONS):

#     retval = {} 
#     for alg in train_fitness:
#         if alg == base_line: 
#             continue
#         retval[alg] = []
#         for i in range(generations):
#             # if len(list(train_fitness[base_line][generations-1].values())) !=  len(list(train_fitness[alg][i].values())):
#             #     print('Imbalanced len:', alg, 'at generation:', i)
#             #     continue

#             pval_wo_wil = stats.wilcoxon(list(train_fitness[base_line][generations-1].values())[:len(train_fitness[alg][i].values())], list(train_fitness[alg][i].values()))[1]
#             if pval_wo_wil >= 0.05:
#                 # The performance is similar now. 
#                 retval[alg].append(0)
#             if pval_wo_wil < 0.05:
#                 mean_alg = statistics.mean(list(train_fitness[alg][i].values()))
#                 mean_bse = statistics.mean(list(train_fitness[base_line][generations-1].values()))
#                 if mean_alg > mean_bse:
#                     # Oh oh
#                     retval[alg].append(-1) # = f'{i} failed.'
#                 else: 
#                     retval[alg].append(1)

#     return retval

# def update_improvement_percentage(improvements, improve_perc_table, improve_table):
#     # retval = {}
#     for alg in improvements: 
#         # retval[alg] = 0
#         if alg not in improve_perc_table:
#             improve_perc_table[alg] = 0
#             improve_table[alg] = []
#         improvement = improvements[alg]
#         improved = 0
#         for i in range(len(improvement)):
#             if improvement[i] >= 0 :
#                 improved += 1
#                 if improved == 3: 
#                     # retval[alg] = 1 -  (i + 1 - improved)/len(improvement)
#                     perc = 1 -  (i + 1 - improved)/len(improvement)
#                     improve_perc_table[alg] += perc
#                     improve_table[alg].append(perc)
#                     break
#             else: 
#                 improved = 0
#     # return retval

# def calc_all_improvements(expbase=dirbase, base_line = 'WithoutKnobox_ax.plot(range(1, len(mean_data) + 1)wledge:clear_true', file_name=''):
#     imp_perc_table = {} # Holds the sum of improvements that each algorithm achieves over all experiments
#     imp_table = {} # Holds a list of improvements that each algorithm achieves over all experiments
#     for exp in experiments:
#         print(exp, '\n')
#         test_fitness = get_test_fitness(dirbase / exp, inclusion_filter, exclusion_filter, num_generations=50)
#         imps = find_improvement(test_fitness)
#         update_improvement_percentage(imps, imp_perc_table, imp_table)
    

#     for alg in imp_perc_table: 
#         imp_perc_table[alg] = imp_perc_table[alg] / len(experiments)
#         print(alg, imp_perc_table[alg], '\n')
    
#     if file_name:
#         with open(file_name+"-perc") as f:
#             json.dump(imp_perc_table, f)
#         with open(file_name+"-list") as f:
#             json.dump(imp_table, f)

#         return imp_perc_table, imp_table


def improv_hist(imp_table):
    box_fig = plt.figure(figsize=(60,50))
    box_ax = box_fig.add_subplot(111)
    box_ax.set_title('algorithm', fontdict={'fontsize':100 })
    box_ax.set_xlabel('Generation', fontdict={'fontsize':100 })
    box_ax.set_ylabel('Fitness', fontdict={'fontsize':100 })
    flierprops = dict(marker='o', markerfacecolor='r', markersize=30, linestyle='none')
    boxprops = dict(linestyle='-', linewidth=5, color='black')
    whiskerprops = dict(linewidth=5)
    medianprops = dict(linewidth=5)

    labels, data = imp_table.keys(), imp_table.values()

    ren_labels = []
    for label in labels:
        label = rename_alg(label, rename_map)
        ren_labels.append(label)
    box_ax.boxplot(data, flierprops=flierprops, boxprops=boxprops, whiskerprops=whiskerprops,medianprops=medianprops)
    # box_ax.set_xticks(range(1, len(labels) + 1), ren_labels)
    box_ax.set_xticklabels(ren_labels, rotation=90)
    box_ax.tick_params(axis='both', which='major', labelsize=50)

    box_fig.savefig('improvements.jpg', bbox_inches='tight', pad_inches=0)

def plot_corr():
    exp = experiments[5]
    algorithms = [
        # 'EnsembleSurrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:dms_30',
                #   'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:dms_30',
                #   'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset',
                  'Surrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_Reset',
                  'Surrogate:initsurpool_true:tp_0.1:knndistmetr_hamming:avefitdup_true:dms_30:surupol_Reset',
                #   'Surrogate:initsurpool_true:tp_0:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic',
                  'Surrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:avefitdup_true:dms_30:surupol_AddOncePhenotypic',
                  'Surrogate:initsurpool_true:tp_0:knndistmetr_phenotypic:avefitdup_true:surupol_Reset',
                  'Surrogate:initsurpool_true:tp_0:surupol_resgen',
    ]
    run = '3'
    corr = {}
    for alg in algorithms:
        if alg not in corr:
            corr[alg] = []
        for gen in range(0, 50):
            df = pd.read_csv(dirbase / exp / alg / str(run) / 'pop' / f'Pop.{gen}.csv.gz')
            corr[alg].append( df['SurrogateFitness'].corr(df['Fitness']))

    fig = plt.figure(figsize=(60, 32))
    ax = fig.add_subplot(111)
    ax.set_xlabel('Generation', fontdict={'fontsize':120 })
    ax.set_ylabel('Weight', fontdict={'fontsize':120 })
    ax.tick_params(axis='both', which='major', labelsize=100)

    for alg in corr:
        ax.plot(range(0, 50), corr[alg], label = rename_alg(alg, rename_map), linewidth=19)
    leg = ax.legend(fontsize=95)
    for line in leg.get_lines():
        line.set_linewidth(20)
    
    fig.savefig(f'{exp.replace(".", "-").replace(":gen_50", "")}-corr-{run}.jpg', bbox_inches='tight', pad_inches=0)

def plot_weight():
    exp = experiments[13]
    alg = 'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:dms_30'
    run = '2'
    w1 = []
    w2 = []
    for gen in range(0, 50):
        df = pd.read_csv(dirbase / exp / alg / str(run) / 'surr' / f'SurrogatePool.{gen}.csv.gz')
        w1.append(df['weight 0'][0])
        w2.append(df['weight 1'][0])

    fig = plt.figure(figsize=(60, 32))
    ax = fig.add_subplot(111)
    ax.set_xlabel('Generation', fontdict={'fontsize':120 })
    ax.set_ylabel('Weight', fontdict={'fontsize':120 })
    ax.tick_params(axis='both', which='major', labelsize=100)
    
    ax.plot(range(0, 50), w1, label = '$ \omega_1 $', linewidth=19)
    ax.plot(range(0, 50), w2, label = '$ \omega_2 $', linewidth=19)
    leg = ax.legend(fontsize=95)
    for line in leg.get_lines():
        line.set_linewidth(20)
    
    fig.savefig(f'{exp.replace(".", "-").replace(":gen_50", "")}-weights-{run}.jpg', bbox_inches='tight', pad_inches=0)

def plot_weights():
    exp = experiments[2]
    alg = 'EnsembleSurrogate:initsurpool_true:tp_0.1:knndistmetr_corrphenotypic:dms_30'
    run = '6'
    w1 = {}
    w2 = {}
    for run in range(1, 31):
        for gen in range(0, 50):
            if not gen in w1:
                w1[gen] = []
                w2[gen] = []
            df = pd.read_csv(dirbase / exp / alg / str(run) / 'surr' / f'SurrogatePool.{gen}.csv.gz')
            w1[gen].append(df['weight 0'][0])
            w2[gen].append(df['weight 1'][0])
        # for i in range(0, 50):
        #     w1.append(df['weight 0'][0])
        #     w2.append(df['weight 1'][0])

        # print(df['weight 0'][0], df['weight 1'][0])

    ww1 = []
    ww2 = []
    for gen in range(0, 50):
        ww1.append( statistics.mean(w1[gen]))
        ww2.append( statistics.mean(w2[gen]))
    plt.plot(range(0, 50), ww1)
    plt.plot(range(0, 50), ww2)
    plt.show()
    plt.savefig('weights' + '.jpg')
