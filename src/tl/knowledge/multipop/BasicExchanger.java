package tl.knowledge.multipop;

import ec.*;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import tl.TLLogger;
import tl.gp.TLGPIndividual;

import java.io.Serializable;

/**
 * InterPopulationExchange is an Exchanger which implements a simple exchanger
 * between subpopulations. IterPopulationExchange uses an arbitrary graph topology
 * for migrating individuals from subpopulations. The assumption is that all
 * subpopulations have the same representation and same task to solve, otherwise
 * the exchange between subpopulations does not make much sense.

 * <p>InterPopulationExchange has a topology which is similar to the one used by
 * IslandExchange.  Every few generations, a subpopulation will send some number
 * of individuals to other subpopulations.  Since all subpopulations evolve at
 * the same generational speed, this is a synchronous procedure (IslandExchange
 * instead is asynchronous by default, though you can change it to synchronous).

 * <p>Individuals are sent from a subpopulation prior to breeding.  They are stored
 * in a waiting area until after all subpopulations have bred; thereafter they are
 * added into the new subpopulation.  This means that the subpopulation order doesn't
 * matter.  Also note that it means that some individuals will be created during breeding,
 * and immediately killed to make way for the migrants.  A little wasteful, we know,
 * but it's simpler that way.

 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><tt><i>base</i>.chatty</tt><br>
 <font size=-1>boolean, default = true</font></td>
 <td valign=top> Should we be verbose or silent about our exchanges?
 </td></tr>
 </table>
 <p><i>Note:</i> For each subpopulation in your population, there <b>must</b> be
 one exch.subpop... declaration set.
 <table>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.select</tt><br>
 <font size=-1>classname, inherits and != ec.SelectionMethod</font></td>
 <td valign=top> The selection method used by subpopulation #n for picking
 migrants to emigrate to other subpopulations.  If not set, uses the default parameter below.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.select</tt><br>
 <font size=-1>classname, inherits and != ec.SelectionMethod</font></td>
 <td valign=top>
 <i>server</i>: Default parameter: the selection method used by a given subpopulation for picking
 migrants to emigrate to other subpopulations.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.select-to-die</tt><br>
 <font size=-1>classname, inherits and != ec.SelectionMethod (Default is random selection)</font></td>
 <td valign=top> The selection method used by subpopulation #n for picking
 individuals to be replaced by migrants.  If not set, uses the default parameter below.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.select-to-die</tt><br>
 <font size=-1>classname, inherits and != ec.SelectionMethod (Default is random selection)</font></td>
 <td valign=top>
 <i>server</i>: Default parameter: the selection method used by a given subpopulation for picking
 individuals to be replaced by migrants.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.mod</tt><br>
 <font size=-1>int >= 1</font></td>
 <td valign=top> The number of generations that subpopulation #n waits between
 sending emigrants.  If not set, uses the default parameter below.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.mod</tt><br>
 <font size=-1>int >= 1</font></td>
 <td valign=top>
 <i>server</i>: Default parameter: the number of generations that a given subpopulation waits between
 sending emigrants.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.start</tt><br>
 <font size=-1>int >= 0</font></td>
 <td valign=top> The generation when subpopulation #n begins sending emigrants.  If not set, uses the default parameter below.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.start</tt><br>
 <font size=-1>int >= 0</font></td>
 <td valign=top>
 <i>server</i>: Default parameter: the generation when a given subpopulation begins sending emigrants.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.size</tt><br>
 <font size=-1>int >= 0</font></td>
 <td valign=top> The number of emigrants sent at one time by generation #n.  If not set, uses the default parameter below.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.num-dest</tt><br>
 <font size=-1>int >= 0</font></td>
 <td valign=top> The number of destination subpopulations for this subpopulation.
 </td></tr>
 <tr><td valign=top><tt><i>base</i>.subpop.<i>n</i>.dest.<i>m</i></tt><br>
 <font size=-1>int >= 0</font></td>
 <td valign=top> Subpopulation #n's destination #m is this subpopulation.
 </td></tr>
 </table>

 */
public class BasicExchanger extends Exchanger implements TLLogger<GPNode>
{
    private static final long serialVersionUID = 1;

    /**
     * The base for parameters of the mutator.
     */
    private static final String P_MUTATOR_BASE = "mutator";

    // static inner classes don't need SerialVersionUIDs
    protected static class IPEInformation implements Serializable
    {
        // the selection method
        public SelectionMethod immigrantsSelectionMethod;

        // the selection method
        SelectionMethod indsToDieSelectionMethod;

        // the number of destination subpopulations
        public int numDest;

        // the subpopulations where individuals need to be sent
        public int[] destinations;

        // the modulo
        public int modulo;

        // the start (offset)
        public int offset;

        // the size
        public int size;
    }

    protected int logID;

    protected Mutator mutator = new Mutator();

    /** The subpopulation delimiter */
    public static final String P_SUBPOP = "subpop";

    /**
     * The number of mutations to perform on an individual if it is seen in the target sub-population. A value of zero
     * means that no mutation should be performed even if the candidate individual is seen. This value cannot be
     * negative.
     */
    public  static final String P_NUM_MUTATIONS = "num-mutation";
    protected int numMutations;

    /** The parameter for the modulo (how many generations should pass between consecutive sendings of individuals */
    public static final String P_MODULO = "mod";

    /** The number of emigrants to be sent */
    public static final String P_SIZE = "size";

    /** How many generations to pass at the beginning of the evolution before the first
     emigration from the current subpopulation */
    public static final String P_OFFSET = "start";

    /** The number of destinations from current island */
    public static final String P_DEST_FOR_SUBPOP = "num-dest";

    /** The prefix for destinations */
    public static final String P_DEST = "dest";

    /** The selection method for sending individuals to other islands */
    public static final String P_SELECT_METHOD = "select";

    /** The selection method for deciding individuals to be replaced by immigrants */
    public static final String P_SELECT_TO_DIE_METHOD = "select-to-die";

    /**
     * If true, then this exchanger will ask the target population to search its entire history to find a duplicate.
     * Otherwise, the target population will search its current population. The default value of this parameter is
     * {@code true}.
     */
    public static final String P_ENABLE_HISTORY_SEARCH = "enable-history";
    protected boolean enableHistory = true;

    /** Whether or not we're chatty */
    public static final String P_CHATTY = "chatty";

    /** My parameter base -- I need to keep this in order to help the server
     reinitialize contacts */
    // SERIALIZE
    public Parameter base;

    protected IPEInformation[] exchangeInformation;

    //  storage for the incoming immigrants: 2 sizes:
    //    the subpopulation and the index of the emigrant
    // this is virtually the array of mailboxes
    public Individual[][] immigrants;

    // the number of immigrants in the storage for each of the subpopulations
    public int[] nImmigrants;

    public boolean chatty;

    // sets up the Island Exchanger
    public void setup(final EvolutionState state, final Parameter _base )
    {
        base = _base;

        Parameter p_numsubpops = new Parameter( ec.Initializer.P_POP ).push( ec.Population.P_SIZE );
        int numsubpops = state.parameters.getInt(p_numsubpops,null,1);
        // later on, Population will complain with this fatally, so don't
        // exit here, just deal with it and assume that you'll soon be shut
        // down

        // how many individuals (maximally) would each of the mailboxes have to hold
        int[] incoming = new int[ numsubpops ];

        if(!(state instanceof HistoricMultiPopEvolutionState))
            throw new RuntimeException("The evolution state does not support history search.");

        // allocate some of the arrays
        exchangeInformation = new IPEInformation[ numsubpops ];
        for( int i = 0 ; i < numsubpops ; i++ )
            exchangeInformation[i] = new IPEInformation();
        nImmigrants = new int[ numsubpops ];

        Parameter p;

        Parameter localBase = base.push( P_SUBPOP );

        chatty = state.parameters.getBoolean(base.push(P_CHATTY), null, true);

        for( int i = 0 ; i < numsubpops ; i++ )
        {
            // update the parameter for the new context
            p = localBase.push( "" + i );

            // read the selection method
            exchangeInformation[i].immigrantsSelectionMethod = (SelectionMethod)
                    state.parameters.getInstanceForParameter( p.push( P_SELECT_METHOD ), base.push(P_SELECT_METHOD), ec.SelectionMethod.class );
            if( exchangeInformation[i].immigrantsSelectionMethod == null )
                state.output.fatal( "Invalid parameter.",  p.push( P_SELECT_METHOD ), base.push(P_SELECT_METHOD) );
            exchangeInformation[i].immigrantsSelectionMethod.setup( state, p.push(P_SELECT_METHOD) );

            // read the selection method
            if( state.parameters.exists( p.push( P_SELECT_TO_DIE_METHOD ), base.push(P_SELECT_TO_DIE_METHOD ) ) )
                exchangeInformation[i].indsToDieSelectionMethod = (SelectionMethod)
                        state.parameters.getInstanceForParameter( p.push( P_SELECT_TO_DIE_METHOD ), base.push( P_SELECT_TO_DIE_METHOD ), ec.SelectionMethod.class );
            else // use RandomSelection
                exchangeInformation[i].indsToDieSelectionMethod = new ec.select.RandomSelection();
            exchangeInformation[i].indsToDieSelectionMethod.setup( state, p.push(P_SELECT_TO_DIE_METHOD));

            // get the modulo
            exchangeInformation[i].modulo = state.parameters.getInt( p.push( P_MODULO ), base.push(P_MODULO ), 1 );
            if( exchangeInformation[i].modulo == 0 )
                state.output.fatal( "Parameter not found, or it has an incorrect value.", p.push( P_MODULO ), base.push( P_MODULO ) );

            // get the offset
            exchangeInformation[i].offset = state.parameters.getInt( p.push( P_OFFSET ), base.push( P_OFFSET ), 0 );
            if( exchangeInformation[i].offset == -1 )
                state.output.fatal( "Parameter not found, or it has an incorrect value.", p.push( P_OFFSET ), base.push( P_OFFSET ) );

            // get the size
            exchangeInformation[i].size = state.parameters.getInt( p.push( P_SIZE ), base.push( P_SIZE ), 1 );
//            if( exchangeInformation[i].size == 0 )
//                state.output.fatal( "Parameter not found, or it has an incorrect value.", p.push( P_SIZE ), base.push( P_SIZE ) );

            // get the number of destinations
            exchangeInformation[i].numDest = state.parameters.getInt( p.push( P_DEST_FOR_SUBPOP ), null, 0 );
            if( exchangeInformation[i].numDest == -1 )
                state.output.fatal( "Parameter not found, or it has an incorrect value.", p.push( P_DEST_FOR_SUBPOP ) );

            exchangeInformation[i].destinations = new int[ exchangeInformation[i].numDest ];
            // read the destinations
            for( int j = 0 ; j < exchangeInformation[i].numDest ; j++ )
            {
                exchangeInformation[i].destinations[j] =
                        state.parameters.getInt( p.push( P_DEST ).push( "" + j ), null, 0 );
                if( exchangeInformation[i].destinations[j] == -1 ||
                        exchangeInformation[i].destinations[j] >= numsubpops )
                    state.output.fatal( "Parameter not found, or it has an incorrect value.", p.push( P_DEST ).push( "" + j ) );
                // update the maximum number of incoming individuals for the destination island
                incoming[ exchangeInformation[i].destinations[j] ] += exchangeInformation[i].size;
            }

        }

        // calculate the maximum number of incoming individuals to be stored in the mailbox
        int max = -1;

        for (int value : incoming)
            if (max == -1 || max < value)
                max = value;

        // set up the mailboxes
        immigrants = new Individual[ numsubpops ][ max ];

        logID = setupLogger(state, base, true);

        mutator.setup(state, base.push(P_MUTATOR_BASE));

        numMutations = state.parameters.getInt(base.push(P_NUM_MUTATIONS), null);
        if(numMutations < 0)
            logFatal(state, logID, "Number of mutations for the exchanger cannot be negative.");
        else
            log(state, logID, true, "Number of mutations for the exchanger: " + numMutations + ".\n");

        enableHistory = state.parameters.getBoolean(base.push(P_ENABLE_HISTORY_SEARCH), null, true);
        log(state, logID, true, String.format("Enable history search: %b\n", enableHistory));
    }


    /**
     Initializes contacts with other processes, if that's what you're doing.
     Called at the beginning of an evolutionary run, before a population is set up.
     It doesn't do anything, as this exchanger works on only 1 computer.
     */
    public void initializeContacts(EvolutionState state)
    {
    }

    /**
     Initializes contacts with other processes, if that's what you're doing.  Called after restarting from a checkpoint.
     It doesn't do anything, as this exchanger works on only 1 computer.
     */
    public void reinitializeContacts(EvolutionState state)
    {
    }

    public Population preBreedingExchangePopulation(EvolutionState state)
    {
        // exchange individuals between subpopulations
        // BUT ONLY if the modulo and offset are appropriate for this
        // generation (state.generation)
        // I am responsible for returning a population.  This could
        // be a new population that I created fresh, or I could modify
        // the existing population and return that.

        // for each of the islands that sends individuals
        for( int i = 0 ; i < exchangeInformation.length ; i++ )
        {

            // else, check whether the emigrants need to be sent
            if( ( state.generation >= exchangeInformation[i].offset ) && ( ( exchangeInformation[i].modulo == 0 ) ||
                    ( ( ( state.generation - exchangeInformation[i].offset ) % exchangeInformation[i].modulo ) == 0 ) ) )
            {
                // send the individuals!!!!

                // for each of the islands where we have to send individuals
                for( int x = 0 ; x < exchangeInformation[i].numDest ; x++ )
                {
                    int destination = exchangeInformation[i].destinations[x];
                    log(state, logID, false, "Sending immigrants from subpop " + i + " to subpop " + destination+"\n");

                    selectToImmigrate(state, i, destination);
                }
            }
        }
        log(state, logID, "\n");

        return state.population;

    }

    // This method updates the 'immigrants' field.
    protected void selectToImmigrate(EvolutionState state, int from, int destination)
    {
        // select "size" individuals and send then to the destination as emigrants
        exchangeInformation[from].immigrantsSelectionMethod.prepareToProduce( state, from, 0 );
        for( int y = 0 ; y < exchangeInformation[from].size ; y++ ) // send all necesary individuals
        {
            // get the index of the immigrant
            int index = exchangeInformation[from].immigrantsSelectionMethod.produce( from, state, 0 );
            Individual immigrant = (Individual) state.population.subpops[from].individuals[index].clone();
            log(state, logID, false, immigrant.toString() + "\n");
            if(immigrant instanceof TLGPIndividual)
            {
                double fitness = immigrant.fitness.fitness();
                ((TLGPIndividual)immigrant).setOrigin(String.format("subpop.%d,%f", from, fitness));
            }
            immigrants[destination][nImmigrants[destination]] =
                    process(state, 0, null, destination, immigrant);
            nImmigrants[destination]++;
        }
        exchangeInformation[from].immigrantsSelectionMethod.finishProducing( state, from, 0 ); // end the selection step
    }

    protected int[] selectToDie(EvolutionState state, int subpop)
    {
        int len = state.population.subpops[subpop].individuals.length;
        boolean[] selected = new boolean[ len ];
        int[] indices = new int[ nImmigrants[subpop] ];
        exchangeInformation[subpop].indsToDieSelectionMethod.prepareToProduce( state, subpop, 0 );
        for( int i = 0 ; i < nImmigrants[subpop] ; i++ )
        {
            do {
                indices[i] = exchangeInformation[subpop].indsToDieSelectionMethod.produce( subpop, state, 0 );
            }
            while( selected[indices[i]] );
            selected[indices[i]] = true;
        }
        exchangeInformation[subpop].indsToDieSelectionMethod.finishProducing( state, subpop, 0 );

        return indices;
    }

    public Population postBreedingExchangePopulation(EvolutionState state)
    {
        // receiving individuals from other islands
        // This is where the immigrants are inserted into the target population.

        log(state, logID, String.format("Gen: %d\n", state.generation));

        for( int subTo = 0 ; subTo < nImmigrants.length ; subTo++ )
        {

            if( nImmigrants[subTo] > 0 && chatty )
            {
                log(state, logID, "Immigrating " +  nImmigrants[subTo] + " individuals from mailbox for subpopulation " + subTo + "\n");
            }

            int len = state.population.subpops[subTo].individuals.length;
            // double check that we won't go into an infinite loop!
            if ( nImmigrants[subTo] >= state.population.subpops[subTo].individuals.length )
                state.output.fatal("Number of immigrants ("+nImmigrants[subTo] +
                        ") is larger than subpopulation #" + subTo + "'s size (" +
                        len +").  This would cause an infinite loop in the selection-to-die procedure.");

            int[] indices = selectToDie(state, subTo); // new int[ nImmigrants[subTo] ];

            HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState) state;
            for( int y = 0 ; y < nImmigrants[subTo] ; y++ )
            {
                Individual mutated = immigrants[subTo][y];
                log(state, logID, "Sending " + mutated.toString() + " to " + subTo + "\n");

                for(int i=0; i < numMutations; i++)
                {
                    boolean isNew;
                    if (!enableHistory)
                        isNew = !hstate.IsSeenInCurrentPop(subTo, mutated);
                    else
                        isNew = hstate.isSeenIn(subTo, mutated) <= 0;

                    if(isNew)
                    {
                        log(state, logID, "new," + i + "\n" );
                        log(state, logID, "replacing," + state.population.subpops[subTo].individuals[ indices[y] ].toString() + "\n\n");
                        // read the individual
                        state.population.subpops[subTo].individuals[indices[y]] = mutated;

                        // reset the evaluated flag (the individuals are not evaluated in the current island */
                        state.population.subpops[subTo].individuals[indices[y]].evaluated = false;
                        break;
                    }
                    log(state, logID, "seen," + i + ",");
                    mutated = mutator.mutate(subTo, (GPIndividual) immigrants[subTo][y], state, 0);
                    log(state, logID, "mutated," + mutated.toString() + "\n");
                }
                log(state, logID, "\n\n");
            }

            // reset the number of immigrants in the mailbox for the current subpopulation
            // this doesn't need another synchronization, because the thread is already synchronized
            nImmigrants[subTo] = 0;
        }

        return state.population;
    }



    /** Called after preBreedingExchangePopulation(...) to evaluate whether or not
     the exchanger wishes the run to shut down (with ec.EvolutionState.R_FAILURE).
     This would happen for two reasons.  First, another process might have found
     an ideal individual and the global run is now over.  Second, some network
     or operating system error may have occurred and the system needs to be shut
     down gracefully.
     This function does not return a String as soon as it wants to exit (another island found
     the perfect individual, or couldn't connect to the server). Instead, it sets a flag, called
     message, to remember next time to exit. This is due to a need for a graceful
     shutdown, where checkpoints are working properly and save all needed information. */
    public String runComplete(EvolutionState state)
    {
        return null;
    }

    /** Closes contacts with other processes, if that's what you're doing.  Called at the end of an evolutionary run. result is either ec.EvolutionState.R_SUCCESS or ec.EvolutionState.R_FAILURE, indicating whether or not an ideal individual was found. */
    public void closeContacts(EvolutionState state, int result)
    {
    }

}
