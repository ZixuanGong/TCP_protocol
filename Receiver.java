import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class Receiver {
	
	private static final int MSS = 576;
	
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
			
			DatagramSocket rcvSock = new DatagramSocket(listening_port);
			System.out.println("Start listening from port " + listening_port);
			
			byte[] tmp = new byte[MSS + 20];
			DatagramPacket pkt_rcvd = new DatagramPacket(tmp, tmp.length);
			
			while(true) {
				rcvSock.receive(pkt_rcvd);
				System.out.println("Received from port " + pkt_rcvd.getPort());
				byte[] pkt_bytes = pkt_rcvd.getData();
				
				Packet pkt = new Packet();
				pkt.setPkt_bytes(pkt_bytes);
				pkt.unpackPkt();
				
				dbg(new String(pkt.getData()));
				dbg("data_len: " + pkt.getData_len());
				
				//checksum check
				pkt.checkCorrupted();
				if (pkt.isCorrupted())
					dbg("corrupted");
				
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void dbg(String s) {
		System.out.println(s);
	}
	
	
}
