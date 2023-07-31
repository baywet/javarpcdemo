package javarpcdemo;

import com.github.arteam.simplejsonrpc.client.Transport;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.IIOException;

public class ProcessTransport implements Transport {
	private final String[] processCommandsAndArgs;

	public ProcessTransport(String... processCommandsAndArgs) {
		if (processCommandsAndArgs.length == 0) {
			throw new IllegalArgumentException("processCommandsAndArgs must not be empty");
		}
		this.processCommandsAndArgs = processCommandsAndArgs;
	}
	@Override
    public String pass(String request) throws IOException {
		final ProcessBuilder builder = new ProcessBuilder(processCommandsAndArgs);
		System.out.println("Starting process: " + String.join(" ", processCommandsAndArgs));
		final Process process = builder.start();
		process.onExit().thenAccept((exitCode) -> {
			System.out.println("Process exited with code: " + exitCode);
		});
		final BufferedReader stdout = process.inputReader(StandardCharsets.UTF_8);
		final OutputStream stdin = process.getOutputStream();
		// get the standard error of the process and write it to the console
		// new Thread(() -> {
		// 	final Scanner stderr = new Scanner(process.getErrorStream());
		// 	while (stderr.hasNextLine()) {
		// 		System.err.println(stderr.nextLine());
		// 	}
		// 	stderr.close();
		// }).start();
		String idToInsert = "\"id\":1,";
		String payload = "Content-Length: " + (request.length() + idToInsert.length()) + "\r\n" +
						"\r\n" +
						new StringBuilder(request).insert(1, idToInsert).toString();
		System.out.println("Sending Payload: \n" + payload);
        stdin.write(payload.getBytes(StandardCharsets.UTF_8));
		stdin.flush();
		
		final Map<String, String> headers = readResponseHeaders(stdout);
		final int contentLength = Integer.parseInt(headers.get("Content-Length"));
		StringBuilder responseBuilder = new StringBuilder();
		int character;
		for(int i = 0; i < contentLength; i++) {
			character = stdout.read();
			char c = (char) character;
			responseBuilder.append(c);
			System.out.print(c); // Print each character as it's read from the output
		}
		final String response = responseBuilder.toString();
		System.out.println("Response from server: " + response);
		stdout.close();
    	stdin.close();
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		process.destroyForcibly();
    	return response;
    }
	private Map<String, String> readResponseHeaders(BufferedReader stdOut) throws IOException {
		StringBuilder headerKey = new StringBuilder();
		StringBuilder headerValue = new StringBuilder();
		boolean readingKey = true;
		final HashMap<String, String> headers = new HashMap<>();
		int character;
		int nCount = 0;
		while ((character = stdOut.read()) != -1) {
			char c = (char) character;
			if (c == '\r') {
				continue;
			} else if (c == '\n') {
				if (!readingKey) {
					readingKey = true;
					headers.put(headerKey.toString(), headerValue.toString());
				}
				nCount++;
				if (nCount == 2) {
					break;
				}
			} else if (c == ':' && readingKey) {
				readingKey = false;
				stdOut.read(); // Consume the space after the colon
				continue;
			} else {
				nCount = 0;
			}
			if (readingKey) {
				headerKey.append(c);
			} else {
				headerValue.append(c);
			}
		}
		return headers;
	}
}
