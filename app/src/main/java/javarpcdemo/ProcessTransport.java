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
		
		String response = null;
		while ((response = stdout.readLine()) != null) {
			if (response.isEmpty() || response.isBlank()) {
				continue;
			}
			System.out.println("Response from server: " + response);
		}
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// process.destroyForcibly();
    	return response;
    }
}
