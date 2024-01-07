import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

//a class composed of entirely static methods to handle tcp communication
public class tcp_transport
{
    //takes in a dataoutputstream and message to send
    public static void sendMessage(DataOutputStream sender,String message) throws IOException
    {
        //send the message and flush the buffer
        sender.writeUTF(message);
        sender.flush();
    }

    //takes in a datainput stream and returns the message read
    public static String rcvMessage(DataInputStream rcvr) throws IOException
    {
        return rcvr.readUTF();
    }

    //takes in a dataoutputstream and a file path
    public static void sendFile(DataOutputStream sender, String filePath) throws IOException
    {
        DataOutputStream outputStream = sender;
        //get file
        File myFile = new File(filePath);
        //send length of file
        outputStream.writeInt(Files.readAllBytes(myFile.toPath()).length);
        outputStream.flush();
        //send data of file
        outputStream.write(Files.readAllBytes(myFile.toPath()));
        outputStream.flush();
    }

    //takes a datainput stream and receives data
    public static byte[] rcvFile(DataInputStream rcvr) throws IOException
    {
        DataInputStream inputStream = rcvr;
        //get size of file being transmitted
        int size = inputStream.readInt();
        //create array of appropriate size
        byte[] data = new byte[size];
        //read the data
        inputStream.read(data);
        //return the data
        return data;
    }
}
