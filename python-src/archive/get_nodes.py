import re

def get_num_nodes(tree):
    # tree = '[(CTT1 min (((FULL * (DC max FUT)) min (((FUT / CTD) min (CR + FULL)) min ((CFH / FULL) min (FUT / CTD)))) max FULL)), use:1, dup: 0, o:]'
    nodes = tree.split(" ")

    return len(nodes) - 5

def num_nodes_in_file(file):
    retval = 0
    for line in open(file):
        if len(line.strip()) == 0:
            continue
        retval += get_num_nodes(line)

    return retval

if __name__ == '__main__':
    file = '/home/mazhar/scratch/CEC/gdb8-v10-to9/1/stats/gdb8-v10-to-9-wk-TLGPCriptor/knowinit.succ.log'
    print(num_nodes_in_file(file))