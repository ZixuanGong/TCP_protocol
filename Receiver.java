import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Receiver {
	
	private static final int MSS = 100;
	
	public static void main(String[] args) {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
	
		try {
			input = stdIn.readLine();
			String[] tokens = input.split(" ");
			
			if (!tokens[0].equals("receiver") || tokens.length < 6) {
				System.out.println("Error: command not correct");
			}
			String filename = tokens[1];
			int listening_port = Integer.parseInt(tokens[2]);
			String sender_IP = tokens[3];
			int sender_port = Integer.parseInt(tokens[4]);
			String log_filename = tokens[5];
			
			//prepare file to write to
			PrintWriter writer_file = new PrintWriter(filename, "UTF-8");
			PrintWriter writer_log = new PrintWriter(log_filename, "UTF-8");
			writer_log.write("Timestamp Src_port Dest_port Seq_num Ack_num ACK FIN Corrupted\n");
			writer_log.flush();
			
			//create sockets
			DatagramSocket rcvSock = new DatagramSocket(listening_port);
			System.out.println("Start listening from port " + listening_port);
			ServerSocket sock = new ServerSocket(sender_port);
			Socket connSock = sock.accept();
			PrintWriter out = new PrintWriter(connSock.getOutputStream(), true);
			
			byte[] tmp = new byte[MSS + 20];
			DatagramPacket pkt_rcvd = new DatagramPacket(tmp, tmp.length);
			
			int expect = 0;
			while(true) {
				rcvSock.receive(pkt_rcvd);
//				String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(new Date());
				Date timestamp = new Date();
				
				System.out.println("Received from port " + pkt_rcvd.getPort());
				byte[] pkt_bytes = pkt_rcvd.getData();
				
				Packet pkt = new Packet();
				pkt.setPkt_bytes(pkt_bytes);
				pkt.unpackPkt();
				pkt.setTimestamp(timestamp);
				pkt.setDest_port(listening_port);
				dbg("after trim, pkt_len=" + Array.getLength(pkt.getPkt_bytes()));
				dbg("data_len=" + String.valueOf(pkt.getData_len()));
				
//				dbg(new String(pkt.getData()));
				//checksum check
				if (pkt.checkCorrupted()) {
					pkt.initCorruptedPkt();
					pkt.writeToLog_rcv(writer_log);
					
					continue;
				}
				
				//check seq_num
				if (pkt.getSeq_num() == expect) {
					expect++;
					out.println(expect);
					
					String s = new String(pkt.getData());
					writer_file.print(s);
					writer_file.flush();
				}
				
				//write to log
				pkt.writeToLog_rcv(writer_log);
				writer_log.flush();

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void dbg(String s) {
		System.out.println(s);
	}
	
	
}
