import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Sender {
	private static final int MSS = 100;
	int base = 0;
	private HashMap<Integer, Packet> pkts_in_wnd = new HashMap<Integer, Packet>();
	PrintWriter writer_log;
	
	public Sender() throws IOException {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		int total_bytes;
		int len = MSS;
		
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
		Socket ackSock = new Socket(remote_IP, ack_port_num);
		if(ackSock.isBound())
			dbg("Ack port connected");
		
		//logfile
		writer_log = new PrintWriter(log_filename, "UTF-8");
		writer_log.write("Timestamp Src_port Dest_port Seq_num Ack_num ACK FIN EstimatedRTT\n");
		writer_log.flush();
		
		//read bytes from file
		String project_path = System.getProperty("user.dir");
		File file = new File(project_path + "/" + filename);
		FileInputStream fileInput = new FileInputStream(file);
		byte fileContent[] = new byte[(int)file.length()];
		fileInput.read(fileContent);
		
		//start listening for ack
		new ListeningThread(ackSock).start();
		
		// make pkt
		int num_pkt = fileContent.length / MSS;
		int last_pkt_len = fileContent.length % MSS;
		if (last_pkt_len > 0)
			num_pkt++;
		
		int seq_num = 0;
		while (seq_num < base + wnd_size && seq_num < num_pkt) {
			byte[] data;
			int data_len;
			int offset;
			
			data_len = (seq_num == num_pkt-1 && last_pkt_len > 0) ? (last_pkt_len) : MSS;
			data = new byte[data_len];
			offset = seq_num*MSS;
			
			Packet pkt = new Packet();
			pkt.setSrc_port(sndSocket.getLocalPort());
			pkt.setDest_port(remote_port);
			pkt.setSeq_num(seq_num);
			pkt.setAck_num(seq_num + 1);
			pkt.setAck((byte)0);
			pkt.setFin((byte)0);
			pkt.setChecksum(0);
			System.arraycopy(fileContent, offset, data, 0, data_len);
			pkt.setData(data);
			
			pkt.packPkt();				
			pkt.computeChecksum();
			pkt.updateChecksum();

			InetAddress rcv_addr = InetAddress.getByName(remote_IP);
			DatagramPacket datagramPkt = new DatagramPacket(pkt.getPkt_bytes(), data_len + 20, rcv_addr, remote_port);
			//set timestamp
//			String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(new Date());
			Date timestamp = new Date();
			sndSocket.send(datagramPkt);
//			startTimer();
			
			pkt.setTimestamp(timestamp);
			pkts_in_wnd.put(seq_num, pkt);
			
			seq_num ++;
		}
		dbg("finish");
			
	}
	
//	private void startTimer() {
//		Timer timer = new Timer();
//		timer.schedule(new TimerTask() {
//			
//			@Override
//			public void run() {
//				long diff = (new Date()).getTime() - users.get(username).getActiveTime().getTime();
//				if (diff > TIME_OUT * 1000) {
//					out.println(">>SERVER: Time out, please log in again");
//					out.println("TIME_OUT");
//				}	
//			}
//		},  TIME_OUT * 1000);
//	}

	public static void main(String[] args) {
		
		try {
			Sender sender = new Sender();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private class ListeningThread extends Thread {
		BufferedReader in;
		int ack;
		Packet pkt;
		
		public ListeningThread(Socket sock) throws IOException {
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));		
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					ack = Integer.parseInt(in.readLine());
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				pkt = pkts_in_wnd.get(ack - 1);
				
				Date rcvTime = new Date();
				long diff = rcvTime.getTime() - pkt.getTimestamp().getTime();
				pkt.setRtt(diff);
				
				pkt.writeToLog_snd(writer_log);
				pkts_in_wnd.remove(ack - 1);
				
				base = ack;
				
			}
		}
		
	}
	
	public static void dbg(String s) {
		System.out.println(s);
	}
	
}
