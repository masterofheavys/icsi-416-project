import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class snw_transport
{
    //takes in a datagram socket, path of file, ip, port, sends file in 1000 byte chunks
    public static void sendFile(DatagramSocket socket, String path, String ip,int port,int localPort) throws IOException
    {

        byte[] ackByte = "ACK".getBytes();
        DatagramSocket ackSocket = new DatagramSocket(localPort);
        ackSocket.setSoTimeout(1000);
        try {
            //read data from local disk
            byte[] data = Files.readAllBytes(Path.of(path));
            //construct length message
            String lenMessage = "LEN:" + data.length;
            byte[] lenMessageBytes = lenMessage.getBytes();
            //code to handle extra / at front of numeric ip addresses
            String cutIP = ip;
            if(!ip.equals("localhost"))
            {
                cutIP = ip.substring(1);
            }
            //construct datagram packet to send len message
            DatagramPacket packet = new DatagramPacket(lenMessageBytes,lenMessageBytes.length, InetAddress.getByName(cutIP),port);
            //send message
            socket.send(packet);
            byte[] subset;
            while(data.length >= 1000)
            {
                //get first 1000 bytes of data
                subset = Arrays.copyOf(data,1000);
                //remove first 1000 bytes of data from total data
                data = Arrays.copyOfRange(data,1000,data.length);
                //send subset of data
                packet = new DatagramPacket(subset,subset.length,InetAddress.getByName(cutIP),port);
                socket.send(packet);

                byte[] ack = new byte[100];
                DatagramPacket ackPacket = new DatagramPacket(ack, ackByte.length);
                ackSocket.receive(ackPacket);
            }
            //send remaining data
            packet = new DatagramPacket(data,data.length,InetAddress.getByName(cutIP),port);
            socket.send(packet);

            byte[] ack = new byte[100];
            DatagramPacket ackPacket = new DatagramPacket(ack, ackByte.length);
            ackSocket.receive(ackPacket);
            ackSocket.close();
        }
        catch (SocketTimeoutException e)
        {
            System.out.println("transmission terminated prematurely.");
            System.exit(0);
        }

    }
    public static byte[] rcvFile(DatagramSocket socket, String senderIP, int senderPort) throws IOException
    {

        String cutIP = senderIP;
        if(!senderIP.equals("localhost"))
        {
            cutIP = senderIP.substring(1);
        }
        boolean rcvDataAfterLen = false;
        try
        {
            //create storage for len message
            byte[] lenMessage = new byte[100];
            DatagramPacket datagramPacket = new DatagramPacket(lenMessage,lenMessage.length);
            socket.setSoTimeout(1000);
            //get len message
            socket.receive(datagramPacket);
            String lenString = new String(datagramPacket.getData(),0, datagramPacket.getLength());
            lenString = lenString.substring(4);
            //get value from len message

            int lengthFile = Integer.valueOf(lenString);

            byte[] totalData = new byte[0];

            byte[] portionData = new byte[1000];


            while(lengthFile >= 1000)
            {
                //rcv 1000 bytes of data
                datagramPacket = new DatagramPacket(portionData,portionData.length);
                socket.receive(datagramPacket);
                rcvDataAfterLen = true;
                byte[] tmp = totalData;
                //allocate more space in array
                totalData = new byte[tmp.length + portionData.length];
                //copy over existing data into new array
                for(int j = 0; j < tmp.length; j++)
                {
                    totalData[j] = tmp[j];
                }
                //copy in new data to array
                for(int j = tmp.length; j < totalData.length; j++)
                {
                    totalData[j] = portionData[j-tmp.length];
                }

                byte[] ackByte = "ACK".getBytes();
                DatagramSocket sendAckSocket = new DatagramSocket();
                DatagramPacket ackPacket = new DatagramPacket(ackByte,ackByte.length,InetAddress.getByName(cutIP),senderPort);
                sendAckSocket.send(ackPacket);

                lengthFile -= 1000;
            }
            //rcv remaining data
            portionData = new byte[lengthFile];
            datagramPacket = new DatagramPacket(portionData,portionData.length);
            socket.receive(datagramPacket);

            byte[] tmp = totalData;
            totalData = new byte[tmp.length + portionData.length];
            for(int j = 0; j < tmp.length; j++)
            {
                totalData[j] = tmp[j];
            }
            for(int j = tmp.length; j < totalData.length; j++)
            {
                totalData[j] = portionData[j-tmp.length];
            }

            byte[] ackByte = "ACK".getBytes();
            DatagramSocket sendAckSocket = new DatagramSocket();
            DatagramPacket ackPacket = new DatagramPacket(ackByte,ackByte.length,InetAddress.getByName(cutIP),senderPort);
            sendAckSocket.send(ackPacket);
            sendAckSocket.close();
            //return data
            return totalData;

        }
        catch (SocketTimeoutException e)
        {
            if(rcvDataAfterLen == false)
            {
                System.out.println("Did not receive data. Terminating.");
            }
            else
            {
                 System.out.println("Data transmission terminated prematurely.");
            }
            System.exit(0);
        }
        return null;
    }
}
