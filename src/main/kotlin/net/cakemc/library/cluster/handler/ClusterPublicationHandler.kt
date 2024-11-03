package net.cakemc.library.cluster.handler

import net.cakemc.library.cluster.ClusterMember
import net.cakemc.library.cluster.ClusterMember.MemberState
import net.cakemc.library.cluster.Context
import net.cakemc.library.cluster.Session
import net.cakemc.library.cluster.address.ClusterIdRegistry
import net.cakemc.library.cluster.codec.ClusterPublication
import net.cakemc.library.cluster.codec.DefaultSyncPublication
import net.cakemc.library.cluster.codec.Publication
import net.cakemc.library.cluster.codec.PublicationBundle

/**
 * Handles the synchronization callbacks for cluster nodes.
 *
 *
 * This class implements the APIPublicationHandler interface and manages the processing
 * of synchronization messages between cluster nodes. It handles startup scenarios,
 * new and existing nodes, and various synchronization commands.
 */
class ClusterPublicationHandler(private val syncContext: Context) : PublicationHandler {
  /**
   * Processes the synchronization callback for a given session and message.
   *
   * @param session   the session for the synchronization
   * @param message   the publication message received
   * @param withNodes the nodes involved in the sync
   * @param out       the output bundle for sending responses
   * @return true if the callback was successfully handled, false otherwise
   */
  override fun callBack(
    session: Session?,
    message: Publication,
    withNodes: ClusterIdRegistry,
    out: PublicationBundle
  ): Boolean {
    val clusterPublication = message as ClusterPublication

    if (clusterPublication.id.toInt() == -1) {
      return handleStartup(out)
    }

    return handleMessage(clusterPublication, withNodes, out)
  }

  /**
   * Handles the startup synchronization for the cluster.
   *
   * @param out               the output bundle for sending responses
   * @return true if startup handling was successful, false otherwise
   */
  private fun handleStartup(out: PublicationBundle): Boolean {
    val snapshot = syncContext.snapshot
    if (snapshot?.cluster == null || snapshot.cluster!!.isEmpty()) {
      return false
    }

    val responses: MutableList<Publication> = ArrayList()
    for (node in snapshot.cluster!!) {
      if (node == null)
        continue

      val command =
        if (node.isValid) DefaultSyncPublication.Command.COMMAND_TAKE_THIS
        else DefaultSyncPublication.Command.COMMAND_DEL_THIS
      val response = ClusterPublication(
        node.id, node.isAuthByKey, node.key,
        node.lastModified, node.syncAddresses, command
      )
      responses.add(response)
    }
    out.write(responses)
    return true
  }

  /**
   * Handles incoming synchronization messages based on the state of the node.
   *
   * @param clusterPublication the cluster publication message
   * @param withNodes         the nodes involved in the sync
   * @param out               the output bundle for sending responses
   * @return true if the message was handled, false otherwise
   */
  private fun handleMessage(
    clusterPublication: ClusterPublication,
    withNodes: ClusterIdRegistry,
    out: PublicationBundle
  ): Boolean {
    val subjectNode = syncContext.getMemberById(clusterPublication.id)

    if (subjectNode == null) {
      handleNewNode(clusterPublication, withNodes, out)
      return true
    } else {
      return handleExistingNode(clusterPublication, subjectNode, withNodes, out)
    }
  }

  /**
   * Handles the synchronization for a new node in the cluster.
   *
   * @param clusterPublication the cluster publication message
   * @param withNodes         the nodes involved in the sync
   * @param out               the output bundle for sending responses
   */
  private fun handleNewNode(
    clusterPublication: ClusterPublication,
    withNodes: ClusterIdRegistry,
    out: PublicationBundle
  ) {
    val node: ClusterMember
    val command = clusterPublication.command

    if (command == DefaultSyncPublication.Command.COMMAND_DEL_THIS) {
      node = ClusterMember(
        clusterPublication.id, clusterPublication.syncAddresses,
        clusterPublication.isAuthByKey, clusterPublication.authKey, clusterPublication.version,
        withNodes, MemberState.DELETED
      )
      syncContext.updateMember(node)
      sendOkResponse(clusterPublication, out)
    } else {
      node = ClusterMember(
        clusterPublication.id, clusterPublication.syncAddresses,
        clusterPublication.isAuthByKey, clusterPublication.authKey, clusterPublication.version,
        withNodes, MemberState.VALID
      )
      syncContext.updateMember(node)
      sendOkResponse(clusterPublication, out)
    }
  }

  /**
   * Handles the synchronization for an existing node in the cluster.
   *
   * @param clusterPublication the cluster publication message
   * @param subjectNode       the existing clusterMember being synced
   * @param withNodes         the nodes involved in the sync
   * @param out               the output bundle for sending responses
   * @return true if the existing node was handled successfully, false otherwise
   */
  private fun handleExistingNode(
    clusterPublication: ClusterPublication, subjectNode: ClusterMember, withNodes: ClusterIdRegistry,
    out: PublicationBundle
  ): Boolean {
    val command = clusterPublication.command
    var node: ClusterMember

    return when (command) {
      DefaultSyncPublication.Command.COMMAND_GIVE_THIS -> {
        sendNodeDetails(subjectNode, out)
        true
      }

      DefaultSyncPublication.Command.COMMAND_DEL_THIS -> handleDeleteCommand(
        clusterPublication,
        subjectNode,
        withNodes,
        out
      )

      DefaultSyncPublication.Command.COMMAND_TAKE_THIS, DefaultSyncPublication.Command.COMMAND_OK, DefaultSyncPublication.Command.COMMAND_RCPT_THIS -> handleTakeOrOkCommand(
        clusterPublication,
        subjectNode,
        withNodes,
        out
      )
    }
  }

  /**
   * Sends the details of a node in the cluster.
   *
   * @param subjectNode the clusterMember whose details are being sent
   * @param out         the output bundle for sending responses
   */
  private fun sendNodeDetails(subjectNode: ClusterMember, out: PublicationBundle) {
    val command =
      if (subjectNode.isValid) DefaultSyncPublication.Command.COMMAND_TAKE_THIS else DefaultSyncPublication.Command.COMMAND_DEL_THIS
    val outMsg = ClusterPublication(
      subjectNode.id, subjectNode.isAuthByKey,
      subjectNode.key, subjectNode.lastModified, subjectNode.syncAddresses, command
    )
    out.write(outMsg)
  }

  /**
   * Handles the delete command for a subject node.
   *
   * @param clusterPublication the cluster publication message
   * @param subjectNode       the existing clusterMember being synced
   * @param withNodes         the nodes involved in the sync
   * @param out               the output bundle for sending responses
   * @return true if the delete command was handled successfully, false otherwise
   */
  private fun handleDeleteCommand(
    clusterPublication: ClusterPublication, subjectNode: ClusterMember,
    withNodes: ClusterIdRegistry, out: PublicationBundle
  ): Boolean {
    if (subjectNode.isValid) {
      if (clusterPublication.version > subjectNode.lastModified) {
        val node = ClusterMember(
          clusterPublication.id, clusterPublication.syncAddresses,
          clusterPublication.isAuthByKey, clusterPublication.authKey, clusterPublication.version,
          withNodes, MemberState.DELETED
        )
        syncContext.updateMember(node)
        sendOkResponse(clusterPublication, out)
        return true
      } else if (clusterPublication.version < subjectNode.lastModified) {
        sendNodeDetails(subjectNode, out)
        return false
      }
    } else {
      subjectNode.addAwareId(withNodes)
      sendOkResponse(clusterPublication, out)
      return true
    }

    return false
  }

  /**
   * Handles the TAKE or OK commands for a subject node.
   *
   * @param clusterPublication the cluster publication message
   * @param subjectNode       the existing clusterMember being synced
   * @param withNodes         the nodes involved in the sync
   * @param out               the output bundle for sending responses
   * @return true if the commands were handled successfully, false otherwise
   */
  private fun handleTakeOrOkCommand(
    clusterPublication: ClusterPublication, subjectNode: ClusterMember,
    withNodes: ClusterIdRegistry, out: PublicationBundle
  ): Boolean {
    if (clusterPublication.version == subjectNode.lastModified) {
      if (clusterPublication.command == DefaultSyncPublication.Command.COMMAND_TAKE_THIS) {
        subjectNode.isScheduled = false
        subjectNode.addAwareId(withNodes)
        sendOkResponse(clusterPublication, out)
        return true
      } else {
        withNodes.add(clusterPublication.id)
        subjectNode.addAwareId(withNodes)

        if (clusterPublication.command == DefaultSyncPublication.Command.COMMAND_RCPT_THIS) {
          sendNodeDetails(subjectNode, out)
        }
        return true
      }
    } else if (clusterPublication.version > subjectNode.lastModified) {
      if (clusterPublication.command == DefaultSyncPublication.Command.COMMAND_TAKE_THIS) {
        val node = ClusterMember(
          clusterPublication.id, clusterPublication.syncAddresses,
          clusterPublication.isAuthByKey, clusterPublication.authKey, clusterPublication.version,
          withNodes, MemberState.VALID
        )
        syncContext.updateMember(node)
        sendOkResponse(clusterPublication, out)
        return true
      } else {
        requestCompleteInformation(clusterPublication, out)
        return false
      }
    } else {
      sendNodeDetails(subjectNode, out)
    }

    return false
  }

  /**
   * Requests complete information about a node.
   *
   * @param clusterPublication the cluster publication message
   * @param out               the output bundle for sending responses
   */
  private fun requestCompleteInformation(clusterPublication: ClusterPublication, out: PublicationBundle) {
    val outMsg = ClusterPublication(
      clusterPublication.id,
      clusterPublication.isAuthByKey, clusterPublication.authKey,
      clusterPublication.version, null, DefaultSyncPublication.Command.COMMAND_GIVE_THIS
    )
    out.write(outMsg)
  }

  /**
   * Sends an OK response for the given cluster publication message.
   *
   * @param clusterPublication the cluster publication message
   * @param out               the output bundle for sending responses
   */
  private fun sendOkResponse(clusterPublication: ClusterPublication, out: PublicationBundle) {
    val outMsg = ClusterPublication(
      clusterPublication.id,
      clusterPublication.isAuthByKey, clusterPublication.authKey,
      clusterPublication.version, null, DefaultSyncPublication.Command.COMMAND_OK
    )
    out.write(outMsg)
  }

  /**
   * Handles the result of a synchronization feature if needed.
   *
   * @param syncFeature a map of sync results
   */
  override fun result(syncFeature: Map<String?, SyncResult>?) {
    // Handle sync results if needed
  }
}
