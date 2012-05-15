/*
 * Copyright (c) 2012, University of Innsbruck, Austria.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package at.sti2.spark.network;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import at.sti2.spark.core.stream.Triple;
import at.sti2.spark.epsilon.network.run.Token;

import com.google.common.base.Stopwatch;

public class SparkWeaveNetworkServerThread implements Runnable {

	static Logger logger = Logger
			.getLogger(SparkWeaveNetworkServerThread.class);

	private SparkWeaveNetwork sparkWeaveNetwork = null;
	private BlockingQueue<Triple> blockingQueue;
	private boolean run = true;

	public SparkWeaveNetworkServerThread(SparkWeaveNetwork sparkWeaveNetwork,
			BlockingQueue<Triple> queue) {
		this.sparkWeaveNetwork = sparkWeaveNetwork;
		this.blockingQueue = queue;
	}

	public void run() {

		long tripleCounter = 0;

		try {
			
			Stopwatch stopWatch = new Stopwatch();
			stopWatch.start();
			
			while (run) {

				// get triple from queue
				Triple triple = blockingQueue.take();

				if (!triple.isPoisonTriple()) {

					// activate network
					long currentTimeMillis = System.currentTimeMillis();
					triple.setTimestamp(currentTimeMillis);
					sparkWeaveNetwork.activateNetwork(triple);

					// GC
					tripleCounter++;
					if (tripleCounter % 2 == 0)
						runGC();
					
				} else {
					run = false;
				}

				// if (tripleCounter%1000 == 0){
				// logger.info(sparkWeaveNetwork.getEpsilonNetwork().getNetwork().getEpsilonMemoryLevels());
				// logger.info(sparkWeaveNetwork.getReteNetwork().getWorkingMemory().getAlphaMemoryLevels());
				// logger.info(sparkWeaveNetwork.getReteNetwork().getBetaMemoryLevels());

				// logger.info("Processing " + (1000/(sTriple.getTimestamp() -
				// timepoint)) + " triples/sec.");
				// timepoint = sTriple.getTimestamp();
				// }

			}
			
			stopWatch.stop();

			StringBuffer timeBuffer = new StringBuffer();
			timeBuffer.append("Processing took ["+ stopWatch.elapsedTime(TimeUnit.MILLISECONDS) + "ms] ");
			timeBuffer.append(stopWatch.elapsedTime(TimeUnit.MINUTES));
			timeBuffer.append(" min ");
			timeBuffer.append(stopWatch.elapsedTime(TimeUnit.SECONDS));
			timeBuffer.append(" s ");
			timeBuffer.append(stopWatch.elapsedTime(TimeUnit.MILLISECONDS));
			timeBuffer.append(" ms.");

			logger.info(timeBuffer.toString());
			logger.info("Pattern has been matched "+ sparkWeaveNetwork.getReteNetwork().getNumMatches()+ " times.");

		} catch (InterruptedException e) {
			logger.error(e);
		}
	}


	public void runGC() {

		/************************************************
		 * CLEANING EPSILON NETWORK
		 ************************************************/

		for (Iterator<Triple> ptIter = sparkWeaveNetwork.getEpsilonNetwork()
				.getProcessedTriples().iterator(); ptIter.hasNext();) {

			Triple processedTriple = ptIter.next();

			for (Token token : sparkWeaveNetwork.getEpsilonNetwork()
					.getTokenNodesByStreamedTriple(processedTriple))
				token.removeTokenFromNode();

			// Remove the list of tokens for given streamed triple
			sparkWeaveNetwork.getEpsilonNetwork().removeListByStreamedTriple(
					processedTriple);

			// Remove the streamed triple from the list
			ptIter.remove();
		}

	}
}
