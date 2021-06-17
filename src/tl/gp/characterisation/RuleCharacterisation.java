package tl.gp.characterisation;

import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;

public interface RuleCharacterisation<CharacterType>
{
	CharacterType characterise(RoutingPolicy policy);

	default CharacterType characterise(GPIndividual individual, int tree, PoolFilter filter)
	{
		return characterise(new GPRoutingPolicy(filter, individual.trees[tree]));
	}

	default CharacterType characterise(GPIndividual individual)
	{
		return characterise(individual, 0, new IdentityPoolFilter());
	}
}
