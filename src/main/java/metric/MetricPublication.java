package metric;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.cakemc.library.cluster.PublicationVersion;
import net.cakemc.library.cluster.codec.Publication;
import net.cakemc.library.cluster.fallback.endpoint.packet.ring.RingBackPacket;

import java.util.Map;

public class MetricPublication extends RingBackPacket implements Publication {

	private short id;
	private double score;

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
	public long getVersion() {
		return PublicationVersion.DEFAULT.ordinal();
	}

	@Override
	public void close() {}

	@Override
	public void configure(Map<String, ?> config) {}

	@Override
	public void deserialize(byte[] data) {
		ByteBuf byteBuf = Unpooled.copiedBuffer(data);

		this.id = byteBuf.readShort();
		this.score = byteBuf.readDouble();
		byteBuf.release();
	}

	@Override
	public byte[] serialize() {
		ByteBuf byteBuf = Unpooled.buffer(Short.BYTES + Double.BYTES);
		byteBuf.writeShort(this.id);
		byteBuf.writeDouble(this.score);

		byte[] data = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(data);

		byteBuf.release();
		return data;
	}

	@Override
	public void writePacket(ByteBuf byteBuf) {
		byteBuf.writeBytes(this.serialize());
	}

	@Override
	public void readPacket(ByteBuf byteBuf) {
		byte[] data = new byte[byteBuf.readInt()];
		byteBuf.readBytes(data);
		this.deserialize(data);
	}

	@Override
	public int id() {
		return -10;
	}

	public short getId() {
		return id;
	}

	public double getScore() {
		return score;
	}

	@Override
	public long getDistributionTime() {
		return super.getDistributionTime();
	}

	@Override
	public int getTargetNode() {
		return super.getTargetNode();
	}

	@Override
	public long getNodeWriteAddress() {
		return super.getNodeWriteAddress();
	}

	@Override
	public long getPacketInstanceAddress() {
		return super.getPacketInstanceAddress();
	}
}
