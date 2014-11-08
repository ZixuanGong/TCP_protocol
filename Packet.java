import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Packet {
	private static final int MSS = 576;

	private int src_port;
	private int dest_port;
	private int seq_num;
	private int ack_num;
	private byte ack;
	private byte fin;
	private int checksum;
	private boolean corrupted;
	private Date timestamp;
	private short data_len;
	private long rtt;
	private byte[] data;
	private byte[] pkt_bytes;
	private boolean resnt = false;
	private boolean in_order = false;
	
	
	public boolean isIn_order() {
		return in_order;
	}
	public void setIn_order(boolean in_order) {
		this.in_order = in_order;
	}
	public boolean isResnt() {
		return resnt;
	}
	public void setResnt(boolean resnt) {
		this.resnt = resnt;
	}
	public void setDest_port(int port) {
		dest_port = port;
	}
	public long getRtt() {
		return rtt;
	}
	public void setRtt(long rtt) {
		this.rtt = rtt;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
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

	public int getSrc_port() {
		return src_port;
	}

	public void setSrc_port(int src_port) {
		this.src_port = src_port;
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
		tmp = ByteBuffer.allocate(4).putInt(src_port).array();
		System.arraycopy(tmp, 0, pkt_bytes, 0, 4);		
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
		System.arraycopy(data, 0, pkt_bytes, 20, data_len);
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
		
		System.arraycopy(pkt_bytes, 0, tmp4, 0, 4);
		buf = ByteBuffer.wrap(tmp4);
		src_port = buf.getInt();
		
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

		if (data_len < MSS) {
			byte[] pkt_bytes_trimmed = new byte[data_len + 20];
			System.arraycopy(pkt_bytes, 0, pkt_bytes_trimmed, 0, data_len+20);
			pkt_bytes = pkt_bytes_trimmed;
		}
	}
	
	public boolean checkCorrupted() {
		int checksum_rcvd = checksum;
		checksum = 0;
		updateChecksum();
		computeChecksum();
		corrupted = (checksum != checksum_rcvd) ? true : false;
		return corrupted;
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
	
	public void writeToLog_snd(PrintWriter writer) {
		//timestamp, source, destination, Sequence #, ACK #, and the flags
		writer.write("SND>>> ");
		writer.write(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(timestamp) + " ");
		writer.write(src_port + " ");
		writer.write(dest_port + " ");
		writer.write(seq_num + " ");
		writer.write(ack_num + " ");
		writer.write(ack + " ");
		writer.write(fin + " ");
		writer.write(rtt + " ");
		if (resnt)
			writer.write("RESEND");
		writer.write("\n");
		writer.flush();
	}
	
	public void writeToLog_rcv(PrintWriter writer) {
		//timestamp, source, destination, Sequence #, ACK #, and the flags
		writer.write("RCV<<< ");
		writer.write(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(timestamp) + " ");
		writer.write(src_port + " ");
		writer.write(dest_port + " ");
		writer.write(seq_num + " ");
		writer.write(ack_num + " ");
		writer.write(ack + " ");
		writer.write(fin + " ");
		if (corrupted)
			writer.write("corrupted ");
		if (!in_order)
			writer.write("not in order ");
		writer.write("\n");
		writer.flush();
	}
	
	public void initCorruptedPkt() {
		seq_num = -1;
		ack_num = -1;
		ack = (byte) -1;
		fin = (byte) -1;
	}
}
