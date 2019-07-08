import re 
import os

def create_dot(directory, file_name):
    """
    Gets the address to a data file that contains description of CARP instance and 
    creates a 'dot' file that contains the graph of the data in GraphViz language. 
    The function creates the output file in the same direction as the input file. 
    """

    in_name = directory + file_name
    file_name = file_name.replace('.dat', '') 
    out_name = directory + file_name + '.gv'


    out = open(out_name, 'w')
    fwl = lambda *x, **y: print(*x, **y, file=out)
    fwl('strict graph ' + file_name.replace('-', '_') + ' {')
    fwl('node [shape=circle];')
    fwl(f'label="{file_name}";')
    fwl('labelloc=top;')
    fwl('labeljust=center;')

    out.flush()

    in_file = open(in_name)

    for line in in_file:
        if 'VERTICES' in line:
            num_vert = int(line.split(':')[-1].strip(' '))
            print('Number of vertice: ', num_vert)
            break

    for i in range(1, num_vert+1):
        fwl(f'n{i};')

    for line in in_file:
        line = line.strip()
        if not line.startswith('('):
            continue
        
        nums = [int(x) for x in re.findall(r'\d+', line)]
        nums.append(0)
        fwl(f'n{nums[0]}--n{nums[1]}[label="({nums[3]}, {nums[2]})"];')


    fwl('}')
    out.flush()
    out.close()
    in_file.close()


if __name__ == '__main__':
    directory = '/home/mazhar/MyPhD/SourceCodes-bk/gpucarp/data/egl/'
    file_name = 'egl-s4-C.dat'
    (_, _, files) = next(os.walk(directory))

    for file in files:
        if not file.endswith('.dat'):
            continue

        create_dot(directory, file)