/*
*      _______                       _        ____ _     _
*     |__   __|                     | |     / ____| |   | |
*        | | __ _ _ __ ___  ___  ___| |    | (___ | |___| |
*        | |/ _` | '__/ __|/ _ \/ __| |     \___ \|  ___  |
*        | | (_| | |  \__ \ (_) \__ \ |____ ____) | |   | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|   |_|
*                                                         
* -----------------------------------------------------------
*
*  TarsosLSH is developed by Joren Six.
*  
* -----------------------------------------------------------
*
*  Info    : http://0110.be/tag/TarsosLSH
*  Github  : https://github.com/JorenSix/TarsosLSH
*  Releases: http://0110.be/releases/TarsosLSH/
* 
*/

package tl.knowledge.sst.lsh.families;

import ec.util.MersenneTwisterFast;

import java.util.Arrays;

public class EuclidianHashFamily implements HashFamily {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3406464542795652263L;
	private final int dimensions;
	private int w;
		
	public EuclidianHashFamily(int w,int dimensions){
		this.dimensions = dimensions;
		this.w=w;
	}
	
	@Override
	public HashFunction createHashFunction(MersenneTwisterFast rand){
		return new EuclideanHash(dimensions, w, rand);
	}
	
	@Override
	public String combine(int[] hashes){
		//return Arrays.hashCode(hashes);
		return Arrays.toString(hashes);
	}

	@Override
	public DistanceMeasure createDistanceMeasure() {
		return new EuclideanDistance();
	}
}
