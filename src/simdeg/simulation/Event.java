package simdeg.simulation;

import simdeg.util.HashableObject;

/**
 * Defines an event that is used by the simulator. There is four different kind
 * of events that specifies each a worker or a job behavior. Each event happens
 * at a given date.
 */
abstract class Event extends HashableObject {

	private final double date;

	private final Worker worker;

	protected Event(double date, Worker worker) {
		this.date = date;
		this.worker = worker;
	}

	protected final double getDate() {
		return date;
	}

	protected final Worker getWorker() {
		return worker;
	}

}

/**
 * Specifies that a worker becomes available at a given date.
 */
class AvailabilityEvent extends Event {

	protected AvailabilityEvent(double date, Worker worker) {
		super(date, worker);
	}

}

/**
 * Specifies that a worker becomes unavailable at a given date.
 */
class UnavailabilityEvent extends Event {

	protected UnavailabilityEvent(double date, Worker worker) {
		super(date, worker);
	}

}

/**
 * Specifies that a worker has reached its time limit for processing a given
 * job.
 */
class ProcessTimeoutEvent extends Event {

	private final Job job;

	protected ProcessTimeoutEvent(double date, Worker worker, Job job) {
		super(date, worker);
		this.job = job;
	}

	protected final Job getJob() {
		return job;
	}

}

/**
 * Specifies that a worker has successfully processed a given job.
 */
class ProcessCompletionEvent extends Event {

	private final Job job;

	protected ProcessCompletionEvent(double date, Worker worker, Job job) {
		super(date, worker);
		this.job = job;
	}

	protected final Job getJob() {
		return job;
	}

}