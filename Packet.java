import java.lang.reflect.Array;
import java.nio.ByteBuffer;



public class Packet {
	private short src_port;
	private short dest_port;
	private int seq_num;
	private int ack_num;
	private short ack;
	private short fin;
	private int checksum;
	private int offset;
	private byte[] data;
	private byte[] pkt_bytes;
	
	
	public short getAck() {
		return ack;
	}

	public void setAck(short ack) {
		this.ack = ack;
	}

	public short getFin() {
		return fin;
	}

	public void setFin(short fin) {
		this.fin = fin;
	}

	public short getSrc_port() {
		return src_port;
	}

	public void setSrc_port(short src_port) {
		this.src_port = src_port;
	}

	public short getDest_port() {
		return dest_port;
	}

	public void setDest_port(short dest_port) {
		this.dest_port = dest_port;
	}

	public int getSeq_num() {
		return seq_num;
	}

	public void setSeq_num(int seq_num) {
		this.seq_num = seq_num;
	}

	public int getAck_num() {
		return ack_num;
	}

	public void setAck_num(int ack_num) {
		this.ack_num = ack_num;
	}

	public int getChecksum() {
		return checksum;
	}

	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getPkt_bytes() {
		return pkt_bytes;
	}
	
	public void packPkt() {
		byte[] tmp;
		
		//source port
		tmp = ByteBuffer.allocate(2).putInt(src_port).array();
		System.arraycopy(tmp, 0, pkt_bytes, 0, 2);
		//dest port
		tmp = ByteBuffer.allocate(2).putInt(dest_port).array();
		System.arraycopy(tmp, 0, pkt_bytes, 2, 2);
		//seq num
		tmp = ByteBuffer.allocate(4).putInt(offset).array();
		System.arraycopy(tmp, 0, pkt_bytes, 4, 4);
		//ack num
		tmp = ByteBuffer.allocate(4).putInt(offset).array();
		System.arraycopy(tmp, 0, pkt_bytes, 8, 4);
		//flag: ack
		tmp = ByteBuffer.allocate(2).putInt(ack).array();
		System.arraycopy(tmp, 0, pkt_bytes, 12, 2);
		//fin
		tmp = ByteBuffer.allocate(2).putInt(fin).array();
		System.arraycopy(tmp, 0, pkt_bytes, 14, 2);
		//checksum (last 4 bytes)
		tmp = ByteBuffer.allocate(4).putInt(checksum).array();
		System.arraycopy(tmp, 0, pkt_bytes, 16, 4);
		//data
		System.arraycopy(data, 0, pkt_bytes, 20, Array.getLength(data));
	}
	
}
