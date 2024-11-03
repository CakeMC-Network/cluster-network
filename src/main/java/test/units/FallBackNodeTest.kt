package test.units

import net.cakemc.library.cluster.api.MemberIdentifier
import net.cakemc.library.cluster.api.MemberIdentifier.Companion.of
import net.cakemc.library.cluster.fallback.AbstractBackUpEndpoint
import net.cakemc.library.cluster.fallback.BackUpClusterNode

/**
 * A test class for simulating the startup of multiple cluster nodes
 * and calculating their potential connections.
 *
 *
 * This class serves as a basic test harness for the cluster node
 * implementation. It initializes several nodes, starts them in separate threads,
 * and calculates the number of unique connections between them.
 */
object FallBackNodeTest {
    private val NODES: MutableList<AbstractBackUpEndpoint> = ArrayList()

    /**
     * A static list of NodeInformation instances representing nodes in the cluster.
     * Each node is initialized with an address and a status of INACTIVE.
     */
    private val INFORMATIONS: List<MemberIdentifier> = java.util.List.of(
        of(1, "127.0.0.1", 30001),
        of(2, "127.0.0.1", 30002),
        of(3, "127.0.0.1", 30003),
        of(4, "127.0.0.1", 30004)
    )

    /**
     * The main method that serves as the entry point for the FallBackNodeTest class.
     * It starts multiple cluster nodes based on the information in the INFORMATIONS list
     * and calculates the total number of connections between them.
     *
     * @param args Command-line arguments (not used).
     */
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Start each node defined in the INFORMATIONS list
        for (information in INFORMATIONS) {
            startNode(information)
        }

        // Calculate and print the total number of unique connections
        println(calculateAllConnections(INFORMATIONS.size).size * 2)
    }

    /**
     * Starts a cluster node in a new thread.
     *
     * @param ownInformation The address of the node being started.
     */
    fun startNode(ownInformation: MemberIdentifier) {
        println("starting ${ownInformation}")
        val thread = Thread {
            val node: AbstractBackUpEndpoint = BackUpClusterNode(ownInformation, INFORMATIONS, "-")
            NODES.add(node)
            node.start()
        }
        thread.name = ownInformation.toString()
        thread.start()
    }

    /**
     * Calculates all unique connections between nodes in the cluster.
     *
     * @param nodeCount The number of nodes in the cluster.
     * @return A list of unique connections represented as pairs of node indices.
     */
    fun calculateAllConnections(nodeCount: Int): List<Pair<Int, Int>> {
        val connections: MutableList<Pair<Int, Int>> = ArrayList()

        // Generate all unique pairs of nodes
        for (i in 0 until nodeCount) {
            for (j in i + 1 until nodeCount) {
                connections.add(Pair(i, j))
            }
        }

        return connections
    }
}
