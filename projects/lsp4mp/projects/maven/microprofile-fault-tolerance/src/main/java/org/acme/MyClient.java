package org.acme;

import java.io.IOException;
import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

@Asynchronous
@Bulkhead(12)
public class MyClient {

	/**
	 * There should be 0-800ms (jitter is -400ms - 400ms) delays between each
	 * invocation. there should be at least 4 retries but no more than 10 retries.
	 */
	@Retry(delay = 400, maxDuration = 3200, jitter = 400, maxRetries = 10)
	public Connection serviceB() {
		return null;
	}

	/**
	 * Sets retry condition, which means Retry will be performed on IOException.
	 */
	@Retry(retryOn = { IOException.class })
	public void serviceC() {

	}

}