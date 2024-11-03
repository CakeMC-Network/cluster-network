package metric;

import io.netty.buffer.ByteBuf;
import net.cakemc.library.cluster.PublicationVersion;
import net.cakemc.library.cluster.codec.Publication;

import java.util.Map;

public class MetricPublication implements Publication {

	private short id;
	private double score;

	private String channel;

	public MetricPublication(short id, double score) {
		this.id = id;
		this.score = score;
	}

	public MetricPublication() {
	}

	@Override
	public String getKey() {
		return "metric-result";
	}

	@Override
	public void setChannel(String channel) {
		this.channel = channel;
	}

	@Override
	public String getChannel() {
		return channel;
	}

	@Override
	public long getVersion() {
		return PublicationVersion.DEFAULT.ordinal();
	}

	@Override
	public void close() {}

	@Override
	public void configure(Map<String, ?> config) {}

	@Override
	public void deserialize(ByteBuf byteBuf) {
		this.id = byteBuf.readShort();
		this.score = byteBuf.readDouble();
	}

	@Override
	public void serialize(ByteBuf byteBuf) {
		byteBuf.writeShort(this.id);
		byteBuf.writeDouble(this.score);
	}

	public short getId() {
		return id;
	}

	public double getScore() {
		return score;
	}

}
