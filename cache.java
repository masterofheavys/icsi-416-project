import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class cache
{
    int cachePort;
    String serverIp;
    int serverPort;
    Socket clientToCache;
    ServerSocket cacheSocket;
    boolean useTCP = true;



    public cache(int cachePort, String serverIp, int serverPort, String transportIndic) throws IOException
    {
        //establish connection to client, save server ip and port, determine transport protocol to use
        this.cachePort = cachePort;
        cacheSocket = new ServerSocket(cachePort);
        clientToCache = cacheSocket.accept();
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        if(transportIndic.equals("snw"))
        {
            useTCP = false;
        }
    }

    //read message from client
    public String readMessage() throws IOException
    {
        //use tcp protocol to read and return message
        DataInputStream tmp = new DataInputStream(new BufferedInputStream(clientToCache.getInputStream()));
        String toRet = tcp_transport.rcvMessage(tmp);
        return toRet;
    }

    public void sendMessage(String message) throws IOException
    {
        //establish connection to server
        Socket cacheToServer = new Socket(serverIp,serverPort);
        DataOutputStream sender = new DataOutputStream(new BufferedOutputStream(cacheToServer.getOutputStream()));
        //use tcp protocol to send message
        tcp_transport.sendMessage(sender,message);
        //end connection to server
        cacheToServer.close();
    }

    public void sendFile(String path) throws IOException
    {
        String appendedPath = "src/cache_files/"+path;
        //if file exists locally, send it to client else get it from server first
        if(new File(appendedPath).exists())
        {
            //send file to client
            DataOutputStream tmp = new DataOutputStream(new BufferedOutputStream(clientToCache.getOutputStream()));
            tcp_transport.sendFile(tmp,path);
            //send that file was rcv from cache
            tcp_transport.sendMessage(tmp,"File received from cache.");
        }
        else
        {
            //send request to server
            sendMessage("get " + path);
            //establish connection to server
            Socket cacheToServer = new Socket(serverIp,serverPort);
            DataOutputStream tmp = new DataOutputStream(new BufferedOutputStream(cacheToServer.getOutputStream()));
            //rcv file from server, store for future use
            rcvFile(path);
            //send file to client
            tmp = new DataOutputStream(new BufferedOutputStream(clientToCache.getOutputStream()));
            tcp_transport.sendFile(tmp,path);
            //send message to client stating that it was rcved from server
            tcp_transport.sendMessage(tmp,"“File delivered from origin.");
            //close connection to server
            cacheToServer.close();
        }
    }




    public void sendFileSNW(String path) throws IOException
    {
        String appendedPath = "src/cache_files/"+path;
        //if the file exists locally, send it from cache else get it from server
        if(new File(appendedPath).exists())
        {
            //make datagram socket to client
            DataOutputStream tmp = new DataOutputStream(new BufferedOutputStream(clientToCache.getOutputStream()));
            DatagramSocket sock = new DatagramSocket();
            String ip = String.valueOf(clientToCache.getInetAddress());
            //send file
            snw_transport.sendFile(sock,appendedPath,ip,clientToCache.getPort(),cachePort);
            //send message saying rcved from cache
            tcp_transport.sendMessage(tmp,"File delivered from cache.");
            sock.close();
        }
        else
        {
            //send request to server
            sendMessage("get " + path);
            sendMessage(String.valueOf(clientToCache.getLocalAddress()));
            sendMessage(String.valueOf(cachePort));
            //connect to server
            Socket cacheToServer = new Socket(serverIp,serverPort);
            DataOutputStream tmp = new DataOutputStream(new BufferedOutputStream(cacheToServer.getOutputStream()));
            //rcv file
            rcvFileSNW(appendedPath);
            //send file to client
            tmp = new DataOutputStream(new BufferedOutputStream(clientToCache.getOutputStream()));
            DatagramSocket sock = new DatagramSocket();
            String ip = String.valueOf(clientToCache.getInetAddress());
            snw_transport.sendFile(sock,appendedPath,ip,clientToCache.getPort(),cachePort);
            //send message stating rcved from server
            tcp_transport.sendMessage(tmp,"File delivered from origin.");
            sock.close();
            cacheToServer.close();
        }
    }

    public void rcvFile(String path) throws IOException
    {
        String appendedPath = "src/cache_files/"+path;
        //estalbish connection to server
        Socket cacheToServer = new Socket(serverIp,serverPort);
        DataInputStream tmp = new DataInputStream(new DataInputStream(cacheToServer.getInputStream()));
        //use tcp transport to rcv file
        byte[] data = tcp_transport.rcvFile(tmp);
        //write file to disk
        System.out.println("here");
        writeToFile(data,appendedPath);
        //close connection to server
        cacheToServer.close();

    }

    public void rcvFileSNW(String path) throws IOException
    {
        //create datagram socket and rcv file using snw transport, write file to disk
        DatagramSocket socket = new DatagramSocket(cachePort);
        byte[] data = snw_transport.rcvFile(socket,serverIp,serverPort);
        socket.close();
        writeToFile(data,path);
    }
    public void writeToFile(byte[] data,String argpath) throws IOException
    {
        //helper method to write array of byte to disk
        Path path = Paths.get(argpath);
        Files.write(path,data);
    }

    public static void main(String[] args) throws IOException
    {
        //create cache using command line arguments
        cache testCache = new cache(Integer.valueOf(args[0]),(args[1]),Integer.valueOf(args[2]),args[3]);
        //wait for command
        String command = testCache.readMessage();
        while(!command.equals("quit"))
        {
            //realistically only one valid command but this format makes it easier to add on new commands
            if((command.startsWith("get")))
            {
                //use corresponding protocol
                if(testCache.useTCP == false)
                {
                    testCache.sendFileSNW(command.substring(4));
                }
                else
                {
                    testCache.sendFile(command.substring(4));
                }

            }
            //read new message
            command = testCache.readMessage();
        }
        
    }
}