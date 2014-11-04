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
	private static final long INIT_TIMEOUT = 1000;
	private int base = 0;
	private Object baseLock = new Object();
	private HashMap<Integer, Packet> pkts_in_wnd = new HashMap<Integer, Packet>();
	PrintWriter writer_log;
	Timer timer;
	private volatile int seq_num;
	
	public Sender() throws IOException, InterruptedException {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		
		input = stdIn.readLine();
		String[] tokens = input.split(" ");
		
		if (!tokens[0].equals("sender") || tokens.length < 6) {
			System.out.println("Error: command not correct");
		}

		String filename = tokens[1];
		String remote_IP = tokens[2];
		InetAddress rcv_addr = InetAddress.getByName(remote_IP);
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
		
		seq_num = 0;
		synchronized(baseLock) {
			while (seq_num < num_pkt) {
				while (seq_num < base + wnd_size) {
					byte[] data;
					int data_len;
					int offset;
					
					dbg(String.valueOf(base));
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
					
					dbg("data_len=" + String.valueOf(pkt.getData_len()));
					
					pkt.packPkt();				
					pkt.computeChecksum();
					pkt.updateChecksum();
		
					DatagramPacket datagramPkt = new DatagramPacket(pkt.getPkt_bytes(), data_len + 20, rcv_addr, remote_port);
					//set timestamp
					Date timestamp = new Date();
					sndSocket.send(datagramPkt);
					//set timer for pkt with the smallest seq_num in the wnd
					if (seq_num == 0)
						startTimer(INIT_TIMEOUT);
					
					pkt.setTimestamp(timestamp);
					pkts_in_wnd.put(seq_num, pkt);
					
					seq_num ++;
				}
				baseLock.wait();
			}
			
		}
		dbg("finish");
			
	}
	
	private void startTimer(long interval) {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				//time up, resend all pkts in the wnd
				seq_num = base;
			}
		},  interval);
	}

	public static void main(String[] args) {
		
		try {
			Sender sender = new Sender();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private class ListeningThread extends Thread {
		BufferedReader in;
		int ack;
		Packet pkt;
		long estimatedRTT;
		long devRTT;
		long timeout;
		
		public ListeningThread(Socket sock) throws IOException {
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));		
			estimatedRTT = 0;
			devRTT = 0;
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
				timer.cancel();
				timer.purge();
				
				pkt = pkts_in_wnd.get(ack - 1);
				
				Date rcvTime = new Date();
				long diff = rcvTime.getTime() - pkt.getTimestamp().getTime();
				//if it's the 1st pkt
				if (estimatedRTT == 0)
					estimatedRTT = diff;
				else
					estimatedRTT = (long)((1- 0.125)*estimatedRTT + 0.125*diff);
				
				pkt.setRtt(diff);
				
				//calc timeout interval
				if (devRTT == 0)
					devRTT = diff-estimatedRTT;
				else
					devRTT = (long)((1-0.25)*devRTT + 0.25*Math.abs(diff-estimatedRTT));
				
				timeout = estimatedRTT + 4*devRTT;
				
				pkt.writeToLog_snd(writer_log);
				pkts_in_wnd.remove(ack - 1);
	
				synchronized(baseLock) {
					base = ack;
					baseLock.notifyAll();
				}
				
				//check if there's unack'd pkts in wnd, if so restart timer
				if (base < seq_num)
					startTimer(timeout);
				
			}
		}
		
	}
	
	public static void dbg(String s) {
		System.out.println(s);
	}
	
}
