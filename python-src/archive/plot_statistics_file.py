import matplotlib.pyplot as plt 
import sys

if __name__ == '__main__':
    file_name = sys.argv[1]

    fitnesses = []
    for line in open(file_name):
        if line.startswith('#'):
            continue 

        fitness = line.split(',')[-1].strip(' ').strip('\t')
        fitnesses.append(float(fitness))



    plt.plot(range(len(fitnesses)), fitnesses)
    plt.show()
