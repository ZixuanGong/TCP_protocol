import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.util.Arrays;


public class Sender {
	private static final int MSS = 576;
	
	public static void main(String[] args) {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		int offset = 0;
		int total_bytes;
		int len = MSS;
		byte[] data;
		
		try {
//				input = stdIn.readLine();
//				String[] tokens = input.split(" ");
//				
//				if (!tokens[0].equals("sender") || tokens.length < 7) {
//					System.out.println("Error: command not correct");
//					break;
//				}
//				String filename = tokens[1];
//				String remote_IP = tokens[2];
//				int remote_port = Integer.parseInt(tokens[3]);
//				int ack_port_num = Integer.parseInt(tokens[4]);
//				String log_filename = tokens[5];
//				int wnd_size = Integer.parseInt(tokens[6]);
//				
//				DatagramSocket sndSocket = new DatagramSocket();
			
			String project_path = System.getProperty("user.dir");
			File file = new File(project_path + "/file");
			FileInputStream fileInput = new FileInputStream(file);
			
			byte fileContent[] = new byte[(int)file.length()];
			
			fileInput.read(fileContent);
			//create string from byte array
			String s = new String(fileContent);
			System.out.println("File content: " + s);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static byte[] readMSSBytesFromFile(FileInputStream input, int offset, int len) {
		
		byte[] data = new byte[len];
		
		try {
			input.read(data);			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return data;
		
	}
}
