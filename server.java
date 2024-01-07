import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//the server uses non-persistent connections in order to eliminate ambiguity from listening from client or cache
public class server
{
    Socket workingSocket;
    ServerSocket serverSocket;
    int serverPort;
    boolean useTCP = true;

    //create server
    public server(int port, String transportIndic) throws IOException
    {
        //listen for connection on specified port
        serverSocket = new ServerSocket(port);
        serverPort = port;
        //determine transport protocol to use
        if(transportIndic.equals("snw"))
        {
            useTCP = false;
        }
    }
    public String readMessage() throws IOException
    {
        //accept connection
        workingSocket = serverSocket.accept();
        //get input stream from connection
        DataInputStream tmp = new DataInputStream(new BufferedInputStream(workingSocket.getInputStream()));
        //rcv message from connection
        String toRet = tcp_transport.rcvMessage(tmp);
        //close connection
        workingSocket.close();
        //return message
        return toRet;
    }

    public void rcvFile(String path) throws IOException
    {
        String appendedPath = "src/server_files/"+path;
        //accept connection
        workingSocket = serverSocket.accept();
        DataInputStream tmp = new DataInputStream(new DataInputStream(workingSocket.getInputStream()));
        //rcv file using tcp protocol
        byte[] data = tcp_transport.rcvFile(tmp);
        //write file to disk
        writeToFile(data,appendedPath);
        DataOutputStream outputStream = new DataOutputStream(new DataOutputStream(workingSocket.getOutputStream()));
        //send message to client stating successful upload
        tcp_transport.sendMessage(outputStream,"File successfully uploaded");
        //close connection
        workingSocket.close();

    }
    public void rcvFileSNW(String path) throws IOException
    {
        String appendedPath = "src/server_files/"+path;
        //accept connection from socket
        workingSocket = serverSocket.accept();
        DataOutputStream outputStream = new DataOutputStream(new DataOutputStream(workingSocket.getOutputStream()));
        DatagramSocket socket = new DatagramSocket(serverPort);
        String clientIP = readMessage();
        int clientPort =Integer.valueOf(readMessage());
        //rcv file using snw protocol
        byte[] data = snw_transport.rcvFile(socket,clientIP,clientPort);
        //write file to disk
        writeToFile(data,appendedPath);
        //send message to client stating successful upload
        tcp_transport.sendMessage(outputStream,"File successfully uploaded");
        //close connection
        socket.close();
        workingSocket.close();
    }
    public void sendFile(String path) throws IOException
    {
        String appendedPath = "src/server_files/"+path;
        //accept connection, (don't know why i needed to accept twice but it broke otherwise)
        workingSocket = serverSocket.accept();
        workingSocket = serverSocket.accept();
        DataOutputStream sender = new DataOutputStream(new BufferedOutputStream(workingSocket.getOutputStream()));
        //send file using tcp protocol
        tcp_transport.sendFile(sender,appendedPath);
        //close connection
        workingSocket.close();
    }

    public void sendFileSNW(String path) throws IOException
    {
        //create datagram socket
        String appendedPath = "src/server_files/"+path;
        DatagramSocket sock = new DatagramSocket();
        String ip = String.valueOf(readMessage());
        int port = Integer.valueOf(readMessage());
        //accept connection
        workingSocket = serverSocket.accept();
        //send file using SNW protocol
        snw_transport.sendFile(sock,appendedPath,ip,port,serverPort);
        //close connection
        workingSocket.close();
        sock.close();
    }

    //helper function to write data to disk
    public void writeToFile(byte[] data,String argPath) throws IOException
    {
        Path path = Paths.get(argPath);
        Files.write(path,data);
    }

    public static void main(String[] args) throws IOException
    {
        //create server using command line argument
        server testServer = new server(Integer.valueOf(args[0]),args[1]);
        //put into queue for message
        String command = testServer.readMessage();
        while(!command.equals("quit"))
        {
            //send file using proper protocol
            if(command.startsWith("get"))
            {
                if(testServer.useTCP == false)
                {
                    testServer.sendFileSNW(command.substring(4));
                }
                else
                {
                    testServer.sendFile(command.substring(4));
                }

            }
            //rcv file using proper protocol
            else if(command.startsWith("put"))
            {
                if(testServer.useTCP == false)
                {
                    testServer.rcvFileSNW(command.substring(4));
                }
                else
                {
                    testServer.rcvFile(command.substring(4));
                }
            }
            //read next command
            command = testServer.readMessage();
        }
    }
}
