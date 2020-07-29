package tl.knowledge.sst;

enum IndividualOrigin
{
	/**
	 * The individual is transferred from a source domain and used for initialising the GP population.
	 */
	InitTransfer,

	/**
	 * The individual is created during initialisation. The individual is not transferred from a source domain but is
	 * created randomly.
	 */
	InitRandom,

	/**
	 * The individual is created with crossover. The crossover method could create a new individual that has not been
	 * seen before.
	 */
	CrossOverUnseen,

	/**
	 * The individual is created with crossover. The crossover operator could not find a new and unseen individual so it
	 * returned a new one that a similar individual to it has been seen previously.
	 */
	CrossOverSeen,

	/**
	 * The individual is created with mutation. The mutation operator could find an unseen individual.
	 */
	MutationUnseen,

	/**
	 * The individual is created with mutation. The mutation operator could not find a new and unseen individual, so it
	 * decided to use an individual that to which a similar individual has been seen before.
	 */
	MutationSeen,

	/**
	 * The individual is created with mutation. The mutation operator could not find a new and unseen individual, so it
	 * created a random one.
	 */
	MutationRandom,

	/**
	 * The individual is reproduced from a previous generation
	 */
	Reproduction
}
