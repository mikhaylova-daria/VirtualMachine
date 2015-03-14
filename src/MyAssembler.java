import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by daria on 12.03.15.
 */
public class MyAssembler {

    ArrayList<String> nameSpaceVar = new ArrayList<>();
    ArrayList<String> nameSpaceConst = new ArrayList<>();
    ArrayList<String> nameSpaceConstString = new ArrayList<>();
    HashMap<String, Integer> mapConstNameValue = new HashMap<>();
    ArrayList<String> nameSpaceLabel = new ArrayList<>();
    HashMap<String, String> mapConstStringNameValue = new HashMap<>();
    HashMap<String, Byte> mapAddress = new HashMap<>();
    Scanner in;
    ByteBuffer byteBuffer;


    MyAssembler(String aFileName) throws IOException {
        byteBuffer = ByteBuffer.allocate(1024);
        makeByteCode(aFileName);

    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    void makeByteCode(String aFileName) throws IOException{
        in = new Scanner(new File(aFileName));
        //считали переменные и константы
        Integer numberOfByteForConstString = 0;
        while (in.hasNext()) {
            String str = in.next();
            switch (str) {
                case "CONST":
                {
                    String name = in.next();
                    Integer val = in.nextInt();
                    nameSpaceConst.add(name);
                    mapConstNameValue.put(name, val);
                    break;
                }
                case "VAR":
                {
                    String name = in.next();
                    nameSpaceVar.add(name);
                    break;
                }
                case "LABEL":
                {
                    String name = in.next();
                    nameSpaceLabel.add(name);
                    break;
                }
                case "CSTRING":
                {
                    String name = in.next();
                    nameSpaceConstString.add(name);
                    String value = in.findInLine("\".*.\"").split("\"")[1];
                    //System.out.println(value);
                    mapConstStringNameValue.put(name, value);
                    numberOfByteForConstString += (4 + 2 * value.length());
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
        byteBuffer.putInt(0); //stackPointer - временное значение
        byteBuffer.putShort(Integer.valueOf(numberOfByteForConstString + (nameSpaceConst.size() + nameSpaceLabel.size()
                + 2) * 4 + nameSpaceVar.size()).shortValue());//начало кода
        byteBuffer.putShort(Integer.valueOf((nameSpaceConst.size() + nameSpaceLabel.size() + 2) * 4
                + numberOfByteForConstString).shortValue()); //начало декларации переменных

        byte position = 8;
        for (String name: nameSpaceConst) {
            byteBuffer.putInt(mapConstNameValue.get(name));
            mapAddress.put(name, position);
            position += 4;
        }


        for (String label: nameSpaceLabel) {
            mapAddress.put(label, position);
            byteBuffer.putInt(0);
            position += 4;
        }


        for (String constString: nameSpaceConstString) {
            mapAddress.put(constString, position);
            String str = mapConstStringNameValue.get(constString);
            byteBuffer.putInt(str.length());
            position += 4;
            for (int i = 0; i < str.length(); ++i) {
                byteBuffer.putChar(str.charAt(i));
                position += 2;
            }

        }

        for (String name: nameSpaceVar) {
            byteBuffer.put(Integer.valueOf(0).byteValue());
            mapAddress.put(name, position);
            ++position;
        }
        in = new Scanner(new File(aFileName));
        Byte command;
        while (in.hasNext()) {
            String str = in.next();
            switch (str) {
                case "MOV":
                {
                    command = 3;
                    byteBuffer.put(command);
                    String firstArg = in.next();
                    String secondArg = in.next();
                    byteBuffer.put(mapAddress.get(firstArg));
                    byteBuffer.put(mapAddress.get(secondArg));
                    byteBuffer.put(Integer.valueOf(0).byteValue());
                    break;
                }
                case "INPUT":
                {
                    command = 1;
                    byteBuffer.put(command);
                    String firstArg = in.next();
                    byteBuffer.put(mapAddress.get(firstArg));
                    byteBuffer.putShort(Integer.valueOf(0).shortValue());
                    break;
                }
                case "OUTPUT":
                {
                    command = 2;
                    byteBuffer.put(command);
                    String firstArg = in.next();
                    String flag = in.next();
                    byteBuffer.put(mapAddress.get(firstArg));
                    if (flag.equals("s")) {
                        byteBuffer.put(Integer.valueOf(1).byteValue());
                    }
                    if (flag.equals("d")) {
                        byteBuffer.put(Integer.valueOf(0).byteValue());
                    }
                    byteBuffer.put(Integer.valueOf(0).byteValue());
                    break;
                }
                case "ADD":
                {
                    command = 4;
                    byteBuffer.put(command);
                    for (int i = 0; i < 3; ++i) {
                        String arg = in.next();
                        byteBuffer.put(mapAddress.get(arg));
                    }
                    break;
                }
                case "DEDUCT":
                {
                    command = 5;
                    byteBuffer.put(command);
                    for (int i = 0; i < 3; ++i) {
                        String arg = in.next();
                        byteBuffer.put(mapAddress.get(arg));
                    }
                    break;
                }
                case "IFLESS":
                {
                    command = 7;
                    byteBuffer.put(command);
                    for (int i = 0; i < 2; ++i) {
                        String arg = in.next();
                        byteBuffer.put(mapAddress.get(arg));
                    }
                    String nameOfLabel;
                    nameOfLabel = in.next();
                    byteBuffer.put(mapAddress.get(nameOfLabel));
                    break;
                }
                case "JUMP":
                {
                    command = 6;
                    byteBuffer.put(command);
                    String arg = in.next();
                    byteBuffer.put(mapAddress.get(arg));
                    break;
                }
                case "LABEL":
                {
                    byteBuffer.putInt(0);
                    command = 9;
                    byteBuffer.put(byteBuffer.position() - 4, command);
                    String arg = in.next();
                    byteBuffer.putInt(mapAddress.get(arg), byteBuffer.position()); //записали значение метки
                    break;
                }
                case "EXIT":
                {
                    byteBuffer.putInt(0);
                    command = 8;
                    byteBuffer.put(byteBuffer.position() - 4, command);
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
       // System.out.println(byteBuffer.position());
        byteBuffer.putInt(0, byteBuffer.position());
        Path path = Paths.get(aFileName + "_bytecode");
        Files.write(path, byteBuffer.array());
    }


}