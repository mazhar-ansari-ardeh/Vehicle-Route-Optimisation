package tl.knowledge.sst;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.TLGPIndividual;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.sst.lsh.Vector;
import tl.knowledge.surrogate.lsh.FittedVector;

import java.util.List;

public class LoggingLSHSSTEvolutionState extends LSHSSTEvolutionState
{
    public static final String P_BASE = SSTEvolutionState.P_BASE;

    public static final String P_LOG_FILE = "log-file";
    private int logID;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        base = new Parameter(P_BASE);

        String file = this.parameters.getString(base.push(P_LOG_FILE), null);
        if(file == null)
        {
            logFatal(this, knowledgeSuccessLogID, "SST Log file is not specified");
            return;
        }

        logID = setupLogger(this, file);
        log(this, logID, false, "Query,HistoryFitness,HistoryGeneration\n");
    }

    private boolean isSeenIn(LSH database, Individual i)
    {
        assert database != null;

        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0]);
        int[] characterise = trc.characterise(policy);

        List<Vector> query = database.query(new Vector(characterise), 1);
        logQuery(query, i);
        return query.size() > 0;
    }

    private void logQuery(List<Vector> query, Individual i)
    {
        assert query != null;

        String q = i.toString();
        if(query.isEmpty())
            log(this, logID, String.format("%s,new,new\n", q));
        for(Vector v : query)
        {
            if(!(v instanceof FittedVector))
                continue;
            FittedVector fv = (FittedVector)v;
            log(this, logID, String.format("'%s',%f,%d\n", q, fv.getFitness(), fv.getGeneration()));
            q = "q";
        }
        log(this, logID, "\n");
    }
}
