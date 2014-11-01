import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

public class Sender {
	private static final int MSS = 576;
	
	public static void main(String[] args) {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		int total_bytes;
		int len = MSS;
		byte[] data;
		
		try {
			input = stdIn.readLine();
			String[] tokens = input.split(" ");
			
			if (!tokens[0].equals("sender") || tokens.length < 6) {
				System.out.println("Error: command not correct");
			}

			String filename = tokens[1];
			String remote_IP = tokens[2];
			int remote_port = Integer.parseInt(tokens[3]);
			int ack_port_num = Integer.parseInt(tokens[4]);
			String log_filename = tokens[5];
			
			int wnd_size;
			if (tokens.length == 7)
				wnd_size = Integer.parseInt(tokens[6]);
			else 
				wnd_size = 1;
			
			DatagramSocket sndSocket = new DatagramSocket();
			ServerSocket ackSocket = new ServerSocket(ack_port_num);
			
			/* read bytes from file
			 * 
			 */
			String project_path = System.getProperty("user.dir");
			File file = new File(project_path + "/file");
			FileInputStream fileInput = new FileInputStream(file);
			
			byte fileContent[] = new byte[(int)file.length()];
			
			fileInput.read(fileContent);
//			String s = new String(fileContent);
//			System.out.println("File content: " + s + "\n" + file.length());
			
			/* split into packets, add header
			 * 
			 */
			int num_pkt = fileContent.length / MSS;
			int last_pkt_len = fileContent.length % MSS;
			if (last_pkt_len > 0)
				num_pkt++;
			
			int offset = 0;
			while (num_pkt > 0) {
				byte[] bytes;
				byte[] tmp;
				int pkt_len;
				
				pkt_len = (num_pkt == 1) ? (last_pkt_len + 20) : (MSS + 20);
				bytes = new byte[pkt_len];
				
				Header header = new Header();
				header.setSrc_port(sndSocket.getPort());
				header.setDest_port(remote_port);
				header.setSeq_num(offset);
				header.setAck_num(offset);
				header.setHeader_len(20);
				header.setAck(false);
				header.setFin(false);
				header.setRcv_wnd(0);
				header.setChecksum(0);
				
				bytes = prepareBytesFromObj(header);
				System.out.println(bytes.length);
//				long checksum = computeChecksum(bytes);
				
//				InetAddress rcv_addr = InetAddress.getByAddress(remote_IP.getBytes());
//				DatagramPacket datagramPkt = new DatagramPacket(bytes, pkt_len, rcv_addr, remote_port);
//				sndSocket.send(datagramPkt);
				/*
				//source port
				tmp = ByteBuffer.allocate(2).putInt(sndSocket.getPort()).array();
				System.arraycopy(tmp, 0, header, 0, 2);
				//dest port
				tmp = ByteBuffer.allocate(2).putInt(remote_port).array();
				System.arraycopy(tmp, 0, header, 2, 2);
				//seq num
				tmp = ByteBuffer.allocate(4).putInt(offset).array();
				System.arraycopy(tmp, 0, header, 4, 4);
				//ack num
				tmp = ByteBuffer.allocate(4).putInt(offset).array();
				System.arraycopy(tmp, 0, header, 8, 4);
				//header len + unused (let the whole byte represent header len)
				tmp = ByteBuffer.allocate(1).putInt(20).array();
				System.arraycopy(tmp, 0, header, 12, 1);
				//flag: ack fin (the last 2 bits of this byte is ack and fin)
				tmp = ByteBuffer.allocate(1).putInt(0).array();
				System.arraycopy(tmp, 0, header, 13, 1);
				//rcv wnd
				tmp = ByteBuffer.allocate(2).putInt(0).array();
				System.arraycopy(tmp, 0, header, 14, 2);
				//checksum (last 4 bytes)
				tmp = ByteBuffer.allocate(4).putInt(0).array();
				System.arraycopy(tmp, 0, header, 16, 4);
				//data
				System.arraycopy(fileContent, offset, header, 20, MSS);
				
				//calculate checksum
				long checksum = computeChecksum(header);
				tmp = ByteBuffer.allocate(4).putLong(checksum).array();
				System.arraycopy(tmp, 0, header, 16, 4);
				
				InetAddress rcv_addr = InetAddress.getByAddress(remote_IP.getBytes());
				DatagramPacket datagramPkt = new DatagramPacket(header, pkt_len, rcv_addr, remote_port);
				sndSocket.send(datagramPkt);
				*/
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static byte[] prepareBytesFromObj(Header header) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		
		out = new ObjectOutputStream(bos);   
		out.writeObject(header);
		byte[] bytes = bos.toByteArray();
	
	    if (out != null)
	    	out.close();
	    bos.close();
	    
	    return bytes;
	
	}

	private static long computeChecksum(byte[] buf) {
		int length = buf.length;
	    int i = 0;

	    long sum = 0;
	    long data;

	    while (length > 1) {
	      data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
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
	      sum += (buf[i] << 8 & 0xFF00);
	      if ((sum & 0xFFFF0000) > 0) {
	        sum = sum & 0xFFFF;
	        sum += 1;
	      }
	    }

	    sum = ~sum;
	    sum = sum & 0xFFFF;
	    return sum;
	}
	
	public void dbg(String s) {
		System.out.println(s);
	}
	
}
