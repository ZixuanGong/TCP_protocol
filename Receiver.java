import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Receiver {
	
	public static void main(String[] args) {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String input;
		
		while(true) {
			try {
				input = stdIn.readLine();
				String[] tokens = input.split(" ");
				
				if (!tokens[0].equals("sender") || tokens.length < 6) {
					System.out.println("Error: command not correct");
					break;
				}
				String filename = tokens[1];
				int listening_port = Integer.parseInt(tokens[2]);
				String sender_IP = tokens[3];
				int sender_port = Integer.parseInt(tokens[4]);
				String log_filename = tokens[5];
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
