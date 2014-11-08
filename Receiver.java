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
	
	private static final int MSS = 576;
	private PrintWriter writer_log;
	String sender_IP;
	int sender_port;
	Socket connSock;
	
	public Receiver() {
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
			sender_IP = tokens[3];
			sender_port = Integer.parseInt(tokens[4]);
			String log_filename = tokens[5];
			
			//prepare file to write to
			PrintWriter writer_file = new PrintWriter(filename, "UTF-8");
			writer_log = new PrintWriter(log_filename, "UTF-8");
			writer_log.write("Timestamp Src_port Dest_port Seq_num Ack_num ACK FIN Corrupted\n");
			writer_log.flush();
			
			//create sockets
			DatagramSocket rcvSock = new DatagramSocket(listening_port);
			System.out.println("Start listening from port " + listening_port);
			ServerSocket sock = new ServerSocket(sender_port);
			connSock = sock.accept();
			dbg("ack sock connected");
			
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
				dbg("rcv pkt " + pkt.getSeq_num());
				
				if (pkt.getFin() == (byte)1) {
					dbg("Delivery completed successfully");
					System.exit(0);
				}
				
				//corrupted pkt
				if (pkt.checkCorrupted()) {
					//out.println(expect);
					pkt.initCorruptedPkt();
					pkt.writeToLog_rcv(writer_log);	
					continue;
				}
				
				//checksum is correct
				if (pkt.getSeq_num() == expect) {
					pkt.setIn_order(true);
					pkt.writeToLog_rcv(writer_log);
					
					expect++;
					out.println(expect);
					log_ack(expect);
					String s = new String(pkt.getData());
					writer_file.print(s);
					writer_file.flush();
					continue;
				}
				
				//write to log
				pkt.writeToLog_rcv(writer_log);
				writer_log.flush();

			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		Receiver rcver = new Receiver();
	}
	
	private void log_ack(int ack_num) {
		writer_log.write("SND>>> ");
		writer_log.write(new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(new Date()) + " ");
		writer_log.write(connSock.getLocalPort() + " ");
		writer_log.write(sender_port + " ");
		writer_log.write(ack_num + " ");
		writer_log.write(ack_num + " ");
		writer_log.write(1 + " ");
		writer_log.write(0 + " ");
		writer_log.write("\n");
		writer_log.flush();

	} 

	public static void dbg(String s) {
		System.out.println(s);
	}
	
	
}
