import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class client
{
    Socket clientToCache;
    String cacheIP;
    int cachePort;
    String serverIP;
    int serverPort;

    boolean useTCP = true;

    public client(String cacheIP, int cachePort, String serverIP, int serverPort, String transportIndic) throws IOException
    {
        //connects client to cache and stores port and server ip for future connections, determine what transport protocol to use
        clientToCache = new Socket(cacheIP,cachePort);
        this.cacheIP = cacheIP;
        this.cachePort = cachePort;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        if(transportIndic.equals("snw"))
        {
            useTCP = false;
        }
    }


    //send a server a message using tcp
    public void sendServerMessage(String message) throws IOException
    {
        //establish connection to server
        Socket clientToServer = new Socket(serverIP,serverPort);
        DataOutputStream sender = new DataOutputStream(new BufferedOutputStream(clientToServer.getOutputStream()));
        //send message
        tcp_transport.sendMessage(sender,message);
        //terminate connection to server
        clientToServer.close();
    }

    public void sendCacheMessage(String message) throws IOException
    {
        //send message to cache
        DataOutputStream sender = new DataOutputStream(new BufferedOutputStream(clientToCache.getOutputStream()));
        tcp_transport.sendMessage(sender,message);
    }

    public void sendFile(String path) throws IOException
    {
        String appendedPath = "client_files/"+path;
        //establish connection to server
        Socket clientToServer = new Socket(serverIP,serverPort);
        DataOutputStream sender = new DataOutputStream(new BufferedOutputStream(clientToServer.getOutputStream()));
        //send file
        tcp_transport.sendFile(sender,appendedPath);
        DataInputStream rcvr = new DataInputStream(new BufferedInputStream(clientToServer.getInputStream()));
        //rcv confirmation message, print it
        System.out.println(tcp_transport.rcvMessage(rcvr));
        //close connection to server
        clientToServer.close();
    }
    public void rcvFile(String path) throws IOException
    {
        String appendedPath = "src/client_files/"+path;
        //use tcp transport to rcv file data in form of array of bytes
        DataInputStream rcvr = new DataInputStream(new BufferedInputStream(clientToCache.getInputStream()));
        byte[] data = tcp_transport.rcvFile(rcvr);
        //turn that data into a file at the specified path
        writeToFile(data,appendedPath);
    }

    //helper function to write data to a given filepath
    public void writeToFile(byte[] data,String argPath) throws IOException
    {
        Path path = Paths.get(argPath);
        Files.write(path,data);
    }
    public String readMessage() throws IOException
    {
        //reads tcp message from cache and returns it
        DataInputStream tmp = new DataInputStream(new BufferedInputStream(clientToCache.getInputStream()));
        String toRet = tcp_transport.rcvMessage(tmp);
        return toRet;
    }


    public void sendFileSNW(String path) throws IOException
    {
        String appendedPath = "src/client_files/"+path;
        //create socket to server
        Socket clientToServer = new Socket(serverIP,serverPort);
        sendServerMessage(clientToServer.getLocalAddress().toString());
        sendServerMessage(String.valueOf(clientToServer.getLocalPort()));
        DatagramSocket sock = new DatagramSocket();
        //use snw transport to send file
        snw_transport.sendFile(sock,appendedPath,serverIP,serverPort, clientToServer.getLocalPort());
        DataInputStream rcvr = new DataInputStream(new BufferedInputStream(clientToServer.getInputStream()));
        //rcv confirmation message, print it
        System.out.println(tcp_transport.rcvMessage(rcvr));
        //close connection to server
        sock.close();
        clientToServer.close();
    }

    public void rcvFileSNW(String path) throws IOException
    {
        String appendedPath = "src/client_files/"+path;
        //use snw transport to rcv a file and write it to file
        DatagramSocket socket = new DatagramSocket(clientToCache.getLocalPort());
        byte[] data = snw_transport.rcvFile(socket,cacheIP,cachePort);
        writeToFile(data,appendedPath);
        socket.close();
    }

    public static void main(String[] args) throws IOException
    {
        //create a new client using command line arguments
        client testClient = new client(args[0],Integer.valueOf(args[1]),args[2],Integer.valueOf(args[3]),args[4]);
        //scanner for user input
        Scanner userInput = new Scanner(System.in);
        String command = userInput.nextLine();
        //unit user quits
        while(!command.equals("quit"))
        {
            //send file using corresponding protocol, pass the filepath and only the filepath
            if(command.startsWith("put"))
            {
                testClient.sendServerMessage(command);
                if(testClient.useTCP == false)
                {
                    testClient.sendFileSNW(command.substring(4));
                }
                else
                {
                    testClient.sendFile(command.substring(4));
                }



            }
            //rcv a file using corresponding protocol, pass the filepath and only the filepath
            else if(command.startsWith("get"))
            {
                testClient.sendCacheMessage(command);

                if(testClient.useTCP == false)
                {
                    testClient.rcvFileSNW(command.substring(4));
                }
                else
                {
                    testClient.rcvFile(command.substring(4));
                }

                System.out.println(testClient.readMessage());
            }
            command = userInput.nextLine();
        }
        //tell server and cache to quit
        testClient.sendCacheMessage("quit");
        testClient.sendServerMessage("quit");
    }
}
