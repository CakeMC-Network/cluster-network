package net.cakemc.library.cluster.handler;

import net.cakemc.library.cluster.ClusterMember;
import net.cakemc.library.cluster.ClusterSnapshot;
import net.cakemc.library.cluster.Context;
import net.cakemc.library.cluster.ClusterMember.MemberState;
import net.cakemc.library.cluster.Session;
import net.cakemc.library.cluster.codec.ClusterPublication;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.codec.PublicationBundle;
import net.cakemc.library.cluster.codec.DefaultSyncPublication.Command;
import net.cakemc.library.cluster.address.ClusterIdRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles the synchronization callbacks for cluster nodes.
 *
 * <p>This class implements the APIPublicationHandler interface and manages the processing
 * of synchronization messages between cluster nodes. It handles startup scenarios,
 * new and existing nodes, and various synchronization commands.</p>
 */
public class ClusterPublicationHandler implements PublicationHandler {

	private final Context syncContext;

	/**
	 * Constructs a ClusterPublicationHandler with the specified sync context.
	 *
	 * @param syncContext the synchronization context for handling cluster sync operations
	 */
	public ClusterPublicationHandler(Context syncContext) {
		this.syncContext = syncContext;
	}

	/**
	 * Processes the synchronization callback for a given session and message.
	 *
	 * @param session   the session for the synchronization
	 * @param message   the publication message received
	 * @param withNodes the nodes involved in the sync
	 * @param out       the output bundle for sending responses
	 * @return true if the callback was successfully handled, false otherwise
	 */
	@Override
	public boolean callBack(Session session, Publication message, ClusterIdRegistry withNodes, PublicationBundle out) {
		ClusterPublication clusterPublication = (ClusterPublication) message;

		if (clusterPublication.getId() == -1) {
			return handleStartup(out);
		}

		return handleMessage(clusterPublication, withNodes, out);
	}

	/**
	 * Handles the startup synchronization for the cluster.
	 *
	 * @param out               the output bundle for sending responses
	 * @return true if startup handling was successful, false otherwise
	 */
	private boolean handleStartup(PublicationBundle out) {
		ClusterSnapshot snapshot = syncContext.getSnapshot();
		if (snapshot == null || snapshot.cluster == null || snapshot.cluster.isEmpty()) {
			return false;
		}

		List<Publication> responses = new ArrayList<>();
		for (ClusterMember node : snapshot.cluster) {
			Command command = node.isValid() ? Command.COMMAND_TAKE_THIS : Command.COMMAND_DEL_THIS;
			ClusterPublication response = new ClusterPublication(node.getId(), node.isAuthByKey(), node.getKey(),
			                                                     node.getLastModified(), node.getSyncAddresses(), command);
			responses.add(response);
		}
		out.write(responses);
		return true;
	}

	/**
	 * Handles incoming synchronization messages based on the state of the node.
	 *
	 * @param clusterPublication the cluster publication message
	 * @param withNodes         the nodes involved in the sync
	 * @param out               the output bundle for sending responses
	 * @return true if the message was handled, false otherwise
	 */
	private boolean handleMessage(ClusterPublication clusterPublication, ClusterIdRegistry withNodes, PublicationBundle out) {
		ClusterMember subjectNode = syncContext.getMemberById(clusterPublication.getId());

		if (subjectNode == null) {
			handleNewNode(clusterPublication, withNodes, out);
			return true;
		} else {
			return handleExistingNode(clusterPublication, subjectNode, withNodes, out);
		}
	}

	/**
	 * Handles the synchronization for a new node in the cluster.
	 *
	 * @param clusterPublication the cluster publication message
	 * @param withNodes         the nodes involved in the sync
	 * @param out               the output bundle for sending responses
	 */
	private void handleNewNode(ClusterPublication clusterPublication, ClusterIdRegistry withNodes, PublicationBundle out) {
		ClusterMember node;
		Command command = clusterPublication.getCommand();

		if (command == Command.COMMAND_DEL_THIS) {
			node = new ClusterMember(clusterPublication.getId(), clusterPublication.getSyncAddresses(),
			                         clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(), clusterPublication.getVersion(),
			                         withNodes, MemberState.DELETED);
			syncContext.updateMember(node);
			sendOkResponse(clusterPublication, out);
		} else {
			node = new ClusterMember(clusterPublication.getId(), clusterPublication.getSyncAddresses(),
			                         clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(), clusterPublication.getVersion(),
			                         withNodes, MemberState.VALID);
			syncContext.updateMember(node);
			sendOkResponse(clusterPublication, out);
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
	private boolean handleExistingNode(
		 ClusterPublication clusterPublication, ClusterMember subjectNode, ClusterIdRegistry withNodes,
		 PublicationBundle out) {
		Command command = clusterPublication.getCommand();
		ClusterMember node;

		return switch (command) {
			case COMMAND_GIVE_THIS -> {
				sendNodeDetails(subjectNode, out);
				yield true;
			}
			case COMMAND_DEL_THIS -> handleDeleteCommand(clusterPublication, subjectNode, withNodes, out);
			case COMMAND_TAKE_THIS, COMMAND_OK, COMMAND_RCPT_THIS -> handleTakeOrOkCommand(clusterPublication, subjectNode, withNodes, out);
		};

	}

	/**
	 * Sends the details of a node in the cluster.
	 *
	 * @param subjectNode the clusterMember whose details are being sent
	 * @param out         the output bundle for sending responses
	 */
	private void sendNodeDetails(ClusterMember subjectNode, PublicationBundle out) {
		Command command = subjectNode.isValid() ? Command.COMMAND_TAKE_THIS : Command.COMMAND_DEL_THIS;
		ClusterPublication outMsg = new ClusterPublication(subjectNode.getId(), subjectNode.isAuthByKey(),
		                                                   subjectNode.getKey(), subjectNode.getLastModified(), subjectNode.getSyncAddresses(), command);
		out.write(outMsg);
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
	private boolean handleDeleteCommand(
		 ClusterPublication clusterPublication, ClusterMember subjectNode,
		 ClusterIdRegistry withNodes, PublicationBundle out) {
		if (subjectNode.isValid()) {
			if (clusterPublication.getVersion() > subjectNode.getLastModified()) {
				ClusterMember node = new ClusterMember(clusterPublication.getId(), clusterPublication.getSyncAddresses(),
				                                       clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(), clusterPublication.getVersion(),
				                                       withNodes, MemberState.DELETED);
				syncContext.updateMember(node);
				sendOkResponse(clusterPublication, out);
				return true;
			} else if (clusterPublication.getVersion() < subjectNode.getLastModified()) {
				sendNodeDetails(subjectNode, out);
				return false;
			}
		} else {
			subjectNode.addAwareId(withNodes);
			sendOkResponse(clusterPublication, out);
			return true;
		}

		return false;
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
	private boolean handleTakeOrOkCommand(
		 ClusterPublication clusterPublication, ClusterMember subjectNode,
		 ClusterIdRegistry withNodes, PublicationBundle out) {
		if (clusterPublication.getVersion() == subjectNode.getLastModified()) {
			if (clusterPublication.getCommand() == Command.COMMAND_TAKE_THIS) {
				subjectNode.setScheduled(false);
				subjectNode.addAwareId(withNodes);
				sendOkResponse(clusterPublication, out);
				return true;
			} else {
				withNodes.add(clusterPublication.getId());
				subjectNode.addAwareId(withNodes);

				if (clusterPublication.getCommand() == Command.COMMAND_RCPT_THIS) {
					sendNodeDetails(subjectNode, out);
				}
				return true;
			}
		} else if (clusterPublication.getVersion() > subjectNode.getLastModified()) {
			if (clusterPublication.getCommand() == Command.COMMAND_TAKE_THIS) {
				ClusterMember node = new ClusterMember(clusterPublication.getId(), clusterPublication.getSyncAddresses(),
				                                       clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(), clusterPublication.getVersion(),
				                                       withNodes, MemberState.VALID);
				syncContext.updateMember(node);
				sendOkResponse(clusterPublication, out);
				return true;
			} else {
				requestCompleteInformation(clusterPublication, out);
				return false;
			}
		} else {
			sendNodeDetails(subjectNode, out);
		}

		return false;
	}

	/**
	 * Requests complete information about a node.
	 *
	 * @param clusterPublication the cluster publication message
	 * @param out               the output bundle for sending responses
	 */
	private void requestCompleteInformation(ClusterPublication clusterPublication, PublicationBundle out) {
		ClusterPublication outMsg = new ClusterPublication(clusterPublication.getId(),
		                                                   clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(),
		                                                   clusterPublication.getVersion(), null, Command.COMMAND_GIVE_THIS);
		out.write(outMsg);
	}

	/**
	 * Sends an OK response for the given cluster publication message.
	 *
	 * @param clusterPublication the cluster publication message
	 * @param out               the output bundle for sending responses
	 */
	private void sendOkResponse(ClusterPublication clusterPublication, PublicationBundle out) {
		ClusterPublication outMsg = new ClusterPublication(clusterPublication.getId(),
		                                                   clusterPublication.isAuthByKey(), clusterPublication.getAuthKey(),
		                                                   clusterPublication.getVersion(), null, Command.COMMAND_OK);
		out.write(outMsg);
	}

	/**
	 * Handles the result of a synchronization feature if needed.
	 *
	 * @param syncFeature a map of sync results
	 */
	@Override
	public void result(Map<String, SyncResult> syncFeature) {
		// Handle sync results if needed
	}
}
