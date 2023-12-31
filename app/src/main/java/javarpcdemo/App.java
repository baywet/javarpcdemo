/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package javarpcdemo;

import java.io.IOException;

import com.github.arteam.simplejsonrpc.client.JsonRpcClient;

public class App {
    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) throws IOException {
        JsonRpcClient client = new JsonRpcClient(new ProcessTransport("kiota", "rpc"));

        // Make a JSON-RPC call
        String result = client.createRequest()
                            .method("GetVersion")
                            .returnAs(String.class)
                            .execute();
        System.out.println("version" + result);
    }
}
