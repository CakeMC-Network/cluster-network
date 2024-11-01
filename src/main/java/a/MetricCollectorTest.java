package a;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.nio.file.*;

// Metric collector class
class MetricCollector {
    
    private final OperatingSystemMXBean osBean;
    
    public MetricCollector() {
        osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    // Collect latency by pinging a known host (e.g., google.com)
    public double collectLatency() {
        try {
            InetAddress address = InetAddress.getByName("google.com");
            long startTime = System.nanoTime();
            if (address.isReachable(5000)) {  // 5-second timeout
                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000.0;  // Convert to ms
            } else {
                return -1;  // Unreachable
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Collect bandwidth - TODO: You'd need a specific library or tool for this, returning placeholder for now
    public double collectBandwidth() {
        // TODO: Implement actual bandwidth measurement using a networking library or external tool.
        return -1;  // Placeholder
    }

    // Collect CPU usage in percentage
    public double collectCpuUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOsBean.getSystemCpuLoad() * 100;
        }
        return -1; // Return -1 if not supported
    }

    // Collect memory usage in percentage
    public double collectMemoryUsage() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            long totalMemory = sunOsBean.getTotalPhysicalMemorySize();
            long freeMemory = sunOsBean.getFreePhysicalMemorySize();
            return ((totalMemory - freeMemory) / (double) totalMemory) * 100;
        }
        return -1; // Return -1 if not supported
    }

    // Collect disk I/O - checking free space and total space on the default filesystem
    public double collectDiskIo() {
        Path root = Paths.get("/");  // Root path, or specify other paths if necessary
        try {
            long totalSpace = Files.getFileStore(root).getTotalSpace();
            long freeSpace = Files.getFileStore(root).getUsableSpace();
            return ((totalSpace - freeSpace) / (double) totalSpace) * 100;  // Used space as percentage
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Collect availability (mocked for this example, typically related to uptime or service health checks)
    public double collectAvailability() {
        // TODO: Implement real uptime monitoring (this is a placeholder for now)
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();  // Uptime in ms
        return 100; // Placeholder, assume 100% uptime
    }

    // Error rate collection (mocked, depends on error logging and monitoring)
    public double collectErrorRate() {
        // TODO: Implement error rate based on logs or monitoring system
        return 0.01; // Placeholder, assume 1% error rate
    }

    // Task distribution collection (this would be system-specific)
    public double collectTaskDistribution() {
        // TODO: Implement actual task distribution logic
        return -1;  // Placeholder
    }

    // Data consistency collection (mocked, for now, depends on the system's requirements)
    public double collectDataConsistency() {
        // TODO: Implement actual data consistency check (e.g., checksumming or sync monitoring)
        return -1;  // Placeholder
    }

    // Collect all metrics
    public void collectAllMetrics() {
        System.out.println("Collecting metrics:");
        System.out.println("Latency: " + collectLatency() + " ms");
        System.out.println("Bandwidth: " + collectBandwidth() + " Mbps");
        System.out.println("CPU Usage: " + collectCpuUsage() + " %");
        System.out.println("Memory Usage: " + collectMemoryUsage() + " %");
        System.out.println("Disk I/O: " + collectDiskIo() + " %");
        System.out.println("Availability: " + collectAvailability() + " %");
        System.out.println("Error Rate: " + collectErrorRate() + " %");
        System.out.println("Task Distribution: " + collectTaskDistribution() + " %");
        System.out.println("Data Consistency: " + collectDataConsistency() + " %");
    }
}

// Test the MetricCollector
public class MetricCollectorTest {
    public static void main(String[] args) {
        MetricCollector collector = new MetricCollector();
        collector.collectAllMetrics();
    }
}
