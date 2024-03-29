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
import tl.knowledge.sst.lsh.Vector;

import java.util.Arrays;
//import java.util.Random;


public class CityBlockHash implements HashFunction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -635398900309516287L;
	private int w;
	private Vector randomPartition;
	
	public CityBlockHash(int dimensions, int width, MersenneTwisterFast rand){
//		Random rand = new Random();
		this.w = width;
		
		randomPartition = new Vector(dimensions);
		for(int d=0; d<dimensions; d++) {
			//mean 0
			//standard deviation 1.0
			double val = rand.nextDouble()*w;
			randomPartition.set(d, val);
		}
	}
	
	public int hash(Vector vector){
		int[] hash = new int[randomPartition.getDimensions()];
		for(int d=0; d<randomPartition.getDimensions(); d++) {
			hash[d] =  (int) Math.floor((vector.get(d)-randomPartition.get(d)) / (double) w);
		}
		return Arrays.hashCode(hash);
	}
	
	public String toString(){
		return String.format("w:%d\nrandomPartition:%s",w,randomPartition); 
	}
}
