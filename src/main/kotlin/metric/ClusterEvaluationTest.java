package metric;

import java.util.*;

// Enum representing different evaluation metrics
enum EvaluationMetric {
    LATENCY,
    BANDWIDTH,
    CPU_USAGE,
    MEMORY_USAGE,
    DISK_IO,
    AVAILABILITY,
    ERROR_RATE,
    TASK_DISTRIBUTION,
    DATA_CONSISTENCY
}

// Represents the result of evaluating a metric for a node
class EvaluationResult {
    private final EvaluationMetric metric;
    private final double score; // A score from 0 to 1 (where 1 is best)

    public EvaluationResult(EvaluationMetric metric, double score) {
        this.metric = metric;
        this.score = score;
    }

    public EvaluationMetric getMetric() {
        return metric;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return metric + ": " + score;
    }
}

// Represents a cluster node
class ClusterNode {
    private final String nodeId;
    private final Map<EvaluationMetric, Double> metrics;

    public ClusterNode(String nodeId) {
        this.nodeId = nodeId;
        this.metrics = new EnumMap<>(EvaluationMetric.class);
    }

    // Set the score for a specific metric
    public void setMetric(EvaluationMetric metric, double score) {
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("Score must be between 0 and 1");
        }
        metrics.put(metric, score);
    }

    // Get the score for a specific metric
    public double getMetric(EvaluationMetric metric) {
        return metrics.getOrDefault(metric, 0.0); // Default to 0 if not set
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<EvaluationResult> evaluate() {
        List<EvaluationResult> results = new ArrayList<>();
        for (Map.Entry<EvaluationMetric, Double> entry : metrics.entrySet()) {
            results.add(new EvaluationResult(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    public double getOverallScore() {
        double totalScore = 0;
        for (double score : metrics.values()) {
            totalScore += score;
        }
        return totalScore / metrics.size(); // Average score
    }
}

// Cluster evaluation system
class ClusterEvaluationSystem {
    private final List<ClusterNode> nodes;

    public ClusterEvaluationSystem() {
        this.nodes = new ArrayList<>();
    }

    // Add a node to be evaluated
    public void addNode(ClusterNode node) {
        nodes.add(node);
    }

    // Evaluate all nodes and print results
    public void evaluateAllNodes() {
        for (ClusterNode node : nodes) {
            System.out.println("Evaluating node: " + node.getNodeId());
            List<EvaluationResult> results = node.evaluate();
            results.forEach(System.out::println);
            System.out.println("Overall score: " + node.getOverallScore());
            System.out.println();
        }
    }

    // Find the best node based on overall score
    public ClusterNode findBestNode() {
        return nodes.stream()
                .max(Comparator.comparingDouble(ClusterNode::getOverallScore))
                .orElse(null);
    }
}

// Main class to test the cluster evaluation system
public class ClusterEvaluationTest {
    public static void main(String[] args) {
        ClusterEvaluationSystem evaluationSystem = new ClusterEvaluationSystem();

        // Create some nodes and set their metrics
        ClusterNode node1 = new ClusterNode("Node-1");
        node1.setMetric(EvaluationMetric.LATENCY, 0.9);
        node1.setMetric(EvaluationMetric.BANDWIDTH, 0.8);
        node1.setMetric(EvaluationMetric.CPU_USAGE, 0.7);
        node1.setMetric(EvaluationMetric.AVAILABILITY, 0.95);

        ClusterNode node2 = new ClusterNode("Node-2");
        node2.setMetric(EvaluationMetric.LATENCY, 0.7);
        node2.setMetric(EvaluationMetric.BANDWIDTH, 0.9);
        node2.setMetric(EvaluationMetric.CPU_USAGE, 0.8);
        node2.setMetric(EvaluationMetric.AVAILABILITY, 0.9);

        // Add nodes to the evaluation system
        evaluationSystem.addNode(node1);
        evaluationSystem.addNode(node2);

        // Evaluate all nodes
        evaluationSystem.evaluateAllNodes();

        // Find the best node based on overall score
        ClusterNode bestNode = evaluationSystem.findBestNode();
        if (bestNode != null) {
            System.out.println("Best node: " + bestNode.getNodeId() + " with score: " + bestNode.getOverallScore());
        } else {
            System.out.println("No nodes available for evaluation.");
        }
    }
}
