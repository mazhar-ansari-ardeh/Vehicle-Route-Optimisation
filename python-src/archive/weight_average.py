from pathlib import Path
import statistics as stats
from scipy import stats as scstats


# base = Path("/home/mazhar/grid/gdb1-v5-to4/")
base = Path("/home/mazhar/scratch/GECCO2019/")


experiments = [
                'gdb1-v5-to-4',
                # 'gdb1-v5-to-6', 
                'gdb2-v6-to-5', 
                # 'gdb2-v6-to-7', 
                'gdb21-v6-to-5', 
                # 'gdb21-v6-to-7', 
                'gdb8-v10-to-9', 
                # 'gdb8-v10-to-11', 
                'gdb9-v10-to-9', 
                # 'gdb9-v10-to-11', 
                'gdb23-v10-to-9', 
                # 'gdb23-v10-to-11', 
                # 'val9C-v5-to-4', 
                'val9C-v5-to-6', 
                'val9D-v10-to-9', 
                # 'val9D-v10-to-11', 
                # 'val10C-v5-to-4', 
                # 'val10C-v5-to-6', 
                # 'val10D-v10-to-9', 
                # 'val10D-v10-to-11',
                ]

def print_weights(experiment):
    weights = {}

    exp_base = base / ('niche-' + experiment.replace('to-', 'to'))
    for i in range(1, 31):
        # /home/mazhar/grid/gdb1-v5-to4/30/stats/gdb1-v5-to-4-wk-cwt-fitall-gen35-49-p0.8-r0.8-all/
        addr = exp_base / str(i) / 'stats' / f'{experiment}-wk-cwt-niching-gen49-49-p0.8-r0.8-all' / 'TerminalERCContribOnlyWeightedLog.succ.log'
        file = open(addr, 'r')
        # file.readline()

        for j in range(14):
            line = file.readline().strip().split(':')
            terminal = line[0]
            weight = line[-1]

            if terminal not in weights:
                weights[terminal] = [] # = weights[terminal] + float(weight)
            # else:
            #     weights[terminal] = float(weight)

            weights[terminal].append(float(weight))

    for terminal in weights:
        print(f'{terminal}: mean={stats.mean(weights[terminal])},' + 
              f'std={stats.stdev(weights[terminal])}, ' + 
              f'min={min(weights[terminal])}({[i for i in range(0, len(weights[terminal])) if weights[terminal][i] == min(weights[terminal])]}), ' + 
              f'max={max(weights[terminal])}({[i for i in range(0, len(weights[terminal])) if weights[terminal][i] == max(weights[terminal])]})')
    print()
    return weights

st = []
for exp in experiments:
    print("weights in experiment ", exp)
    st.append(print_weights(exp))

# gdb1_weights = st[0]
# gdb2_weights = st[2]
# gdb21_weights = st[4]
# for terminal in gdb1_weights:
#     print(terminal)
#     print(scstats.wilcoxon(gdb1_weights[terminal], gdb2_weights[terminal])[1])
#     print(scstats.wilcoxon(gdb1_weights[terminal], gdb21_weights[terminal])[1])
#     print(scstats.wilcoxon(gdb2_weights[terminal], gdb21_weights[terminal])[1])

