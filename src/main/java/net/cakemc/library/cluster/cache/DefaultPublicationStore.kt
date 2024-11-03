package net.cakemc.library.cluster.cache;

import java.util.HashMap;
import java.util.Map;

public class DefaultPublicationStore implements PublicationStore {

	private final Map<String, SelectedAwareNodes> cache;

	public DefaultPublicationStore() {
		cache = new HashMap<>();
	}

	@Override
	public void updateAwareNodes(String key, long version, short[] awareNodes) {
		SelectedAwareNodes message = cache.get(key);

		if (message == null) {
			message = new SelectedAwareNodes(version, awareNodes);
			cache.put(key, message);
			return;
		}

		if (message.version < version) {
			message.version = version;
			message.awareNodes = awareNodes;
		} else if (message.version == version) {
			short[] combined = new short[awareNodes.length + message.awareNodes.length];
			int index = 0;
			for (int i = 0 ; i < message.awareNodes.length ; i++) {
				combined[i] = message.awareNodes[index++];
			}
			for (short awareNode : awareNodes) {
				combined[index] = awareNode;
				index++;
			}
			message.awareNodes = combined;
		}
	}

	@Override
	public short[] getAwareNodes(String key, long version) {
		SelectedAwareNodes message = cache.get(key);

		if (message == null) {
			return null;
		}

		if (message.version == version) {
			return message.awareNodes;
		}

		return null;
	}

	@Override
	public void shutdown() {
		cache.clear();
	}
}
