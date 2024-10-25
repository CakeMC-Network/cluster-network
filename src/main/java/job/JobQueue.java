package job;

import java.util.concurrent.LinkedBlockingDeque;

public class JobQueue extends LinkedBlockingDeque<AbstractJob> {

	// AbstractJob -> Queue => presented to cluster
	// forEach(node) -> can take job ?
	// validate nodes and get best matching
	// assign AbstractJob -> best node

}
