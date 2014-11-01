import java.lang.reflect.Array;
import java.nio.ByteBuffer;



public class Packet {
	private short src_port;
	private short dest_port;
	private int seq_num;
	private int ack_num;
	private byte ack;
	private byte fin;
	private int checksum;
	private boolean corrupted;
	private short data_len;
	private byte[] data;
	private byte[] pkt_bytes;
	
	public short getData_len() {
		return data_len;
	}
	public byte getAck() {
		return ack;
	}

	public void setAck(byte ack) {
		this.ack = ack;
	}

	public byte getFin() {
		return fin;
	}

	public void setFin(byte fin) {
		this.fin = fin;
	}

	public boolean isCorrupted() {
		return corrupted;
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

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getPkt_bytes() {
		return pkt_bytes;
	}
	
	public void setPkt_bytes(byte[] pkt_bytes) {
		this.pkt_bytes = pkt_bytes;
	}
	
	public void packPkt() {
		data_len = (short)Array.getLength(data);
		pkt_bytes = new byte[20 + data_len];
		byte[] tmp;
		
		//source port
		tmp = ByteBuffer.allocate(2).putShort(src_port).array();
		System.arraycopy(tmp, 0, pkt_bytes, 0, 2);
		//dest port
		tmp = ByteBuffer.allocate(2).putShort(dest_port).array();
		System.arraycopy(tmp, 0, pkt_bytes, 2, 2);
		//seq num
		tmp = ByteBuffer.allocate(4).putInt(seq_num).array();
		System.arraycopy(tmp, 0, pkt_bytes, 4, 4);
		//ack num
		tmp = ByteBuffer.allocate(4).putInt(ack_num).array();
		System.arraycopy(tmp, 0, pkt_bytes, 8, 4);
		//flag: ack
		tmp = ByteBuffer.allocate(1).put(ack).array();
		System.arraycopy(tmp, 0, pkt_bytes, 12, 1);
		//fin
		tmp = ByteBuffer.allocate(1).put(fin).array();
		System.arraycopy(tmp, 0, pkt_bytes, 13, 1);
		//data_len
		tmp = ByteBuffer.allocate(2).putShort(data_len).array();
		System.arraycopy(tmp, 0, pkt_bytes, 14, 2);
		//checksum (last 4 bytes)
		tmp = ByteBuffer.allocate(4).putInt(checksum).array();
		System.arraycopy(tmp, 0, pkt_bytes, 16, 4);
		//data
		System.arraycopy(data, 0, pkt_bytes, 20, Array.getLength(data));
	}

	public void updateChecksum() {
		byte[] tmp = ByteBuffer.allocate(4).putInt(checksum).array();
		System.arraycopy(tmp, 0, pkt_bytes, 16, 4);
	}
	
	public void unpackPkt() {
		byte[] tmp1 = new byte[1];
		byte[] tmp2 = new byte[2];
		byte[] tmp4 = new byte[4];
		ByteBuffer buf;
		
		System.arraycopy(pkt_bytes, 0, tmp2, 0, 2);
		buf = ByteBuffer.wrap(tmp2);
		src_port = buf.getShort();
		
		System.arraycopy(pkt_bytes, 2, tmp2, 0, 2);
		buf = ByteBuffer.wrap(tmp2);
		dest_port = buf.getShort();
		
		System.arraycopy(pkt_bytes, 4, tmp4, 0, 4);
		buf = ByteBuffer.wrap(tmp4);
		seq_num = buf.getInt();
		
		System.arraycopy(pkt_bytes, 8, tmp4, 0, 4);
		buf = ByteBuffer.wrap(tmp4);
		ack_num = buf.getInt();
		
		System.arraycopy(pkt_bytes, 12, tmp1, 0, 1);
		buf = ByteBuffer.wrap(tmp1);
		ack = buf.get();
		
		System.arraycopy(pkt_bytes, 13, tmp1, 0, 1);
		buf = ByteBuffer.wrap(tmp1);
		fin = buf.get();
		
		System.arraycopy(pkt_bytes, 14, tmp2, 0, 2);
		buf = ByteBuffer.wrap(tmp2);
		data_len = buf.getShort();
		
		System.arraycopy(pkt_bytes, 16, tmp4, 0, 4);
		buf = ByteBuffer.wrap(tmp4);
		checksum = buf.getInt();
		
		data = new byte[data_len];
		System.arraycopy(pkt_bytes, 20, data, 0, data_len);
	}
	
	public void checkCorrupted() {
		int checksum_rcvd = checksum;
		checksum = 0;
		packPkt();
		computeChecksum();
		corrupted = (checksum != checksum_rcvd) ? true : false;		
	}
	
	public void computeChecksum() {
		int length = pkt_bytes.length;
	    	int i = 0;

	    	int sum = 0;
	    	int data;

	    	while (length > 1) {
	      		data = (((pkt_bytes[i] << 8) & 0xFF00) | ((pkt_bytes[i + 1]) & 0xFF));
	      		sum += data;
	      		//if longer than 16 bits
	      		if ((sum & 0xFFFF0000) > 0) {
	        		sum = sum & 0xFFFF;
	        		sum += 1;
	      		}

	      		i += 2;
	      		length -= 2;
	    	}

	    	// last byte
	    	if (length > 0) {
	      		sum += (pkt_bytes[i] << 8 & 0xFF00);
		      	if ((sum & 0xFFFF0000) > 0) {
		      		sum = sum & 0xFFFF;
		        	sum += 1;
		      	}
	    	}

		sum = ~sum;
		sum = sum & 0xFFFF;
		checksum = sum;
	}
	
}
