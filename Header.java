public class Header {
	private int src_port;
	private int dest_port;
	private long seq_num;
	private long ack_num;
	private int header_len;
	private boolean ack;
	private boolean fin;
	private int rcv_wnd;
	private int checksum;
	private byte[] data;
	
	public int getSrc_port() {
		return src_port;
	}
	public void setSrc_port(int src_port) {
		this.src_port = src_port;
	}
	public int getDest_port() {
		return dest_port;
	}
	public void setDest_port(int dest_port) {
		this.dest_port = dest_port;
	}
	public long getSeq_num() {
		return seq_num;
	}
	public void setSeq_num(long seq_num) {
		this.seq_num = seq_num;
	}
	public long getAck_num() {
		return ack_num;
	}
	public void setAck_num(long ack_num) {
		this.ack_num = ack_num;
	}
	public int getHeader_len() {
		return header_len;
	}
	public void setHeader_len(int header_len) {
		this.header_len = header_len;
	}
	public boolean isAck() {
		return ack;
	}
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	public boolean isFin() {
		return fin;
	}
	public void setFin(boolean fin) {
		this.fin = fin;
	}
	public int getRcv_wnd() {
		return rcv_wnd;
	}
	public void setRcv_wnd(int rcv_wnd) {
		this.rcv_wnd = rcv_wnd;
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
	
	
}
