import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Sender {
	private static final int MSS = 576;
	private static final long INIT_TIMEOUT = 1000;
	private int base = 0;
	private Object baseLock = new Object();
	private HashMap<Integer, Packet> pkts_in_wnd = new HashMap<Integer, Packet>();
	PrintWriter writer_log;
	Timer timer;
	private Object timerLock = new Object();
	private volatile int seq_num;
	DatagramSocket sndSocket;
	Socket ackSock;
	InetAddress rcv_addr;
	short remote_port;
	BufferedReader in;
	long timeout;
	long estimatedRTT;
	int num_pkt;
	boolean stdout = false;
	int count = 0;
	
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
		rcv_addr = InetAddress.getByName(remote_IP);
		remote_port = Short.parseShort(tokens[3]);
		short ack_port_num = Short.parseShort(tokens[4]);
		String log_filename = tokens[5];
		if (log_filename.equals("stdout"))
			stdout = true;
		
		int wnd_size;
		if (tokens.length == 7)
			wnd_size = Integer.parseInt(tokens[6]);
		else 
			wnd_size = 1;
		
		sndSocket = new DatagramSocket(9999);
		ackSock = new Socket(rcv_addr, ack_port_num);
		in = new BufferedReader(new InputStreamReader(ackSock.getInputStream()));		
		
		//logfile
		if (!stdout) {
			writer_log = new PrintWriter(log_filename, "UTF-8");
			writer_log.write("Timestamp Src_port Dest_port Seq# Ack# ACK FIN eRTT\n");
			writer_log.flush();
		}
		
		//read bytes from file
		String project_path = System.getProperty("user.dir");
		File file = new File(project_path + "/" + filename);
		FileInputStream fileInput = new FileInputStream(file);
		byte fileContent[] = new byte[(int)file.length()];
		fileInput.read(fileContent);
		
		//start listening for ack
		new ListeningThread().start();

		// make pkt
		num_pkt = fileContent.length / MSS;
		int last_pkt_len = fileContent.length % MSS;
		if (last_pkt_len > 0)
			num_pkt++;
		
		Date startTime = new Date();
		seq_num = 0;
		synchronized(baseLock) {
			while (base < num_pkt) {
				while (seq_num < base + wnd_size && seq_num < num_pkt) {
					//dbg("enter loop: " + seq_num);
					int offset = seq_num * MSS;
					int data_len = (seq_num == num_pkt-1 && last_pkt_len > 0) ? (last_pkt_len) : MSS;
					byte[] data;	
					data = new byte[data_len];
					System.arraycopy(fileContent, offset, data, 0, data_len);
					
					Packet pkt = makePkt(seq_num, data, (byte)0);
					sendPkt(pkt);
					
					seq_num ++;
				}
				baseLock.wait();
			}
		}
		long diff = new Date().getTime() - startTime.getTime();
		
		byte[] data;	
		data = new byte[1];
		System.arraycopy(fileContent, 0, data, 0, 1);
		sendPkt(makePkt(num_pkt, data, (byte)1));
		dbg("\nDelivery completed successfully");
		dbg("Total bytes sent = " + String.valueOf(Array.getLength(fileContent) + 20*num_pkt));
		dbg("Segments sent = " + count);
		dbg("Segments retransmitted = " + String.valueOf(count-num_pkt));
		dbg("Total time used: " + diff + " ms");
	}
	
	private Packet makePkt(int num, byte[] data, byte fin) throws IOException {
		
		Packet pkt = new Packet();
		pkt.setSrc_port(sndSocket.getLocalPort());
		pkt.setDest_port(remote_port);
		pkt.setSeq_num(num);
		pkt.setAck_num(num + 1);
		pkt.setAck((byte)0);
		pkt.setFin(fin);
		pkt.setChecksum(0);
		pkt.setData(data);
				
		pkt.packPkt();				
		pkt.computeChecksum();
		pkt.updateChecksum();
		
		return pkt;
	}

	private void startTimer(long interval) {
		synchronized (timerLock) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					//time up, update seq_num, resend all pkts in the wnd
					synchronized(baseLock) {
						seq_num = base;
						baseLock.notifyAll();
					}
				}
			},  interval);
		}
	}

	protected void sendPkt(Packet pkt) {
		DatagramPacket datagramPkt = new DatagramPacket(pkt.getPkt_bytes(), pkt.getData_len() + 20, rcv_addr, remote_port);
		try {
			pkt.setRtt(estimatedRTT);
			pkt.setTimestamp(new Date());
			if (pkts_in_wnd.containsKey(seq_num)) {
				count++;
				pkt.setResnt(true);
			}
			
			pkts_in_wnd.put(seq_num, pkt);
			if (!stdout) {
				synchronized (writer_log) {
					pkt.writeToLog_snd(writer_log);
				}
			} else {
				dbg(writeToLog_snd(pkt));
			}
			sndSocket.send(datagramPkt);
			if (seq_num == base) {
				startTimer(timeout);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	public String writeToLog_snd(Packet p) {
		//timestamp, source, destination, Sequence #, ACK #, and the flags
		String s = "SND>>> " + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(p.getTimestamp()) 
				+ " " + p.getSrc_port() + " " + remote_port + " "
				+ p.getSeq_num() + " " + p.getAck_num() + " " + p.getAck() + " "
				+ p.getFin() + " " + p.getRtt() + " ";
		if (p.isResnt())
			s += "RESEND";
		
		s += "\n";
		return s;
	}

	public static void main(String[] args) {
		
		try {
			Sender sender = new Sender();
		} catch (IOException e) {
			dbg("File not found");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private class ListeningThread extends Thread {
		int ack;
		Packet pkt;
		long devRTT;
		
		public ListeningThread() throws IOException {
					
			estimatedRTT = 40;
			devRTT = 0;
			System.out.print("Downloading: 0%\r");
		}
		
		private void log_ack(int ack_num) {
			String s = "RCV<<< " + new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:sss").format(new Date()) 
					+ " " + remote_port + " " + sndSocket.getLocalPort() + " "
					+ String.valueOf(ack_num-1) + " " + String.valueOf(ack_num-1) + " " 
					+ 1 + " " + 0 + " " + estimatedRTT + "\n";
			if (stdout)
				dbg(s);
			else 
				synchronized (writer_log) {
					writer_log.write(s);
					writer_log.flush();
				}
		} 
		
		@Override
		public void run() {
			while (true) {
				try {
					ack = Integer.parseInt(in.readLine());
					synchronized (timerLock) {
						timer.cancel();
						timer.purge();
					}
					
					log_ack(ack);
				} catch (NumberFormatException e) {
					System.exit(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				String s1 = "=";
				int m = (int)Math.round((double)ack/(double)num_pkt * 20);
				String bar = new String(new char[m]).replace("\0", s1);
//				String s2 = " ";
//				int n = 20 - m;
//				String spaces = new String(new char[n]).replace("\0", s2);
//				
				System.out.print("Downloading: " + Math.round((double)ack/(double)num_pkt * 100) + "% "
						+ bar + "\r");
				
				pkt = pkts_in_wnd.get(ack - 1);
				if (!pkt.isResnt())
					calculateTimeout();

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

		private void calculateTimeout() {
			Date rcvTime = new Date();
			long diff = rcvTime.getTime() - pkt.getTimestamp().getTime();
			
			estimatedRTT = (long)((1- 0.125)*estimatedRTT + 0.125*diff);
			devRTT = (long)((1-0.25)*devRTT + 0.25*Math.abs(diff-estimatedRTT));
			timeout = estimatedRTT + 4*devRTT;
		}
	}
	
	public static void dbg(String s) {
		System.out.println(s);
	}
	
}
