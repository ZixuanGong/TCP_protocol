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

public class Sender {
	private static final int MSS = 576;
	
	public static void main(String[] args) {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		int total_bytes;
		int len = MSS;
		
		try {
			input = stdIn.readLine();
			String[] tokens = input.split(" ");
			
			if (!tokens[0].equals("sender") || tokens.length < 6) {
				System.out.println("Error: command not correct");
			}

			String filename = tokens[1];
			String remote_IP = tokens[2];
			short remote_port = Short.parseShort(tokens[3]);
			short ack_port_num = Short.parseShort(tokens[4]);
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
			File file = new File(project_path + "/" + filename);
			FileInputStream fileInput = new FileInputStream(file);
			
			byte fileContent[] = new byte[(int)file.length()];
			
			fileInput.read(fileContent);
			
			/* split into packets, add header
			 * 
			 */
			int num_pkt = fileContent.length / MSS;
			int last_pkt_len = fileContent.length % MSS;
			if (last_pkt_len > 0)
				num_pkt++;
			
			int offset = 0;
			while (num_pkt > 0) {
				byte[] pkt_bytes;
				byte[] data;
				int data_len;
				
				data_len = (num_pkt == 1 && last_pkt_len > 0) ? (last_pkt_len) : MSS;
				data = new byte[data_len];
				
				Packet pkt = new Packet();
				pkt.setSrc_port((short)sndSocket.getPort());
				pkt.setDest_port(remote_port);
				pkt.setSeq_num(offset);
				pkt.setAck_num(offset);
				pkt.setAck((byte)0);
				pkt.setFin((byte)0);
				pkt.setChecksum(0);
				System.arraycopy(fileContent, offset, data, 0, data_len);
				pkt.setData(data);
				
				pkt.packPkt();				
				pkt.computeChecksum();
				pkt.packPkt();
				
				pkt_bytes = pkt.getPkt_bytes();
				
				InetAddress rcv_addr = InetAddress.getByName("localhost");
				DatagramPacket datagramPkt = new DatagramPacket(pkt_bytes, pkt_bytes.length, rcv_addr, remote_port);
				sndSocket.send(datagramPkt);

				num_pkt--;
				offset += data_len;
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static byte[] prepareBytesFromObj(Packet pkt) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
			out = new ObjectOutputStream(bos);   
			out.writeObject(pkt);
			byte[] bytes = bos.toByteArray();
		
		    if (out != null) {
		      out.close();
		    }
		    bos.close();
		    
		    return bytes;
		    
	}
	
	public void dbg(String s) {
		System.out.println(s);
	}
	
}
