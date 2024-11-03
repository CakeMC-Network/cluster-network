package metric;

import java.util.*;
import java.util.concurrent.*;

// Represents a job to be processed
class Job {
    private final String id;

    public Job(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void execute() {
        System.out.println("Executing job: " + id);
        // Simulate job processing
    }
}

// Interface for job receivers
interface JobReceiver {
    boolean isAvailable();
    void assignJob(Job abstractJob);
    int getCapacity(); // For picking the best receiver
}

// Implementation of a basic JobReceiver
class BasicJobReceiver implements JobReceiver {
    private final String name;
    private final int capacity;
    private final Queue<Job> abstractJobQueue;

    public BasicJobReceiver(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.abstractJobQueue = new LinkedList<>();
    }

    @Override
    public boolean isAvailable() {
        return abstractJobQueue.size() < capacity;
    }

    @Override
    public void assignJob(Job abstractJob) {
        if (isAvailable()) {
            abstractJobQueue.add(abstractJob);
            System.out.println("AbstractJob assigned to receiver: " + name);
            abstractJob.execute(); // Execute abstractJob immediately
        } else {
            System.out.println("Receiver " + name + " is full!");
        }
    }

    @Override
    public int getCapacity() {
        return capacity - abstractJobQueue.size(); // Remaining capacity
    }
}

// AbstractJob system that manages job queue and receivers
class JobSystem {
    private final Queue<Job> abstractJobQueue;
    private final List<JobReceiver> jobReceivers;

    public JobSystem() {
        abstractJobQueue = new ConcurrentLinkedQueue<>();
        jobReceivers = new ArrayList<>();
    }

    // Add a abstractJob to the queue
    public void addJob(Job abstractJob) {
        abstractJobQueue.add(abstractJob);
        assignJob(); // Try to assign the abstractJob immediately
    }

    // Add a job receiver to the system
    public void addJobReceiver(JobReceiver receiver) {
        jobReceivers.add(receiver);
    }

    // Assign a job from the queue to the best available receiver
    private void assignJob() {
	    Job abstractJob = abstractJobQueue.poll();
        if (abstractJob == null) return;

        JobReceiver bestReceiver = jobReceivers.stream()
                .filter(JobReceiver::isAvailable)
                .max(Comparator.comparingInt(JobReceiver::getCapacity))
                .orElse(null);

        if (bestReceiver != null) {
            bestReceiver.assignJob(abstractJob);
        } else {
            System.out.println("No available receivers, abstractJob re-queued: " + abstractJob.getId());
            abstractJobQueue.add(abstractJob); // Requeue the abstractJob if no receiver is available
        }
    }
}

// Main class to test the job system
public class JobSystemTest {
    public static void main(String[] args) {
        JobSystem jobSystem = new JobSystem();

        // Create some receivers
        jobSystem.addJobReceiver(new BasicJobReceiver("Receiver-1", 2));
        jobSystem.addJobReceiver(new BasicJobReceiver("Receiver-2", 3));

        // Add jobs
        for (int i = 1; i <= 5; i++) {
            jobSystem.addJob(new Job("AbstractJob-" + i));
        }
    }
}
