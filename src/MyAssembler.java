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

    ArrayList<String> nameSpaceConst = new ArrayList<>();
    ArrayList<String> nameSpaceConstString = new ArrayList<>();
    ArrayList<String> nameSpaceFunc = new ArrayList<>();
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
                case "FUNC":
                    String name = in.next();
                    nameSpaceFunc.add(name);
                    break;
                default:
                {
                    break;
                }
            }
        }
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byte position = 4*4;

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


        for (String foo: nameSpaceFunc) {
            mapAddress.put(foo, position);
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

        Path path2 = Paths.get(aFileName + "__");
        Files.write(path2, byteBuffer.array());

        in = new Scanner(new File(aFileName));
        Byte command;
        while (in.hasNext()) {
            String str = in.next();
            switch (str) {
                case "MAIN"://без аргументов!!::
                {
                    byteBuffer.putInt(12, byteBuffer.position());
                    command = 10; //push [ссылка на инт или 0(значит кладём нулевой инт)]
                    str = in.next();
                    HashMap<String, Byte> mapOffset = new HashMap<>();
                    Byte offset = 0;
                    while (!str.equals(":")) {
                        byteBuffer.put(command);
                        mapOffset.put(str, offset);
                        ++offset;
                        if (mapConstNameValue.containsKey(str)) {
                            byteBuffer.put(mapAddress.get(str));
                        } else {
                            byteBuffer.put(Integer.valueOf(0).byteValue());

                        }
                        str = in.next();
                    }
                    boolean flag = true;
                    while (in.hasNext() && flag) {
                        str = in.next();
                        switch (str) {
                            case "CALL":
                            {
                                String name = in.next();

                                Integer addressJump = byteBuffer.getInt(mapAddress.get(name)); // адресс адреса кода вызываемой функции
                                String answer = in.next();

                                command = 13;//pushConst
                                byteBuffer.put(command);
                                if (answer.equals("(")) {
                                    byteBuffer.putInt(0);//НЕТ ВОЗВРАЩАЕМОГО ЗНАЧЕНИЯ
                                } else {
                                    //System.out.println(answer+"!!");
                                    byteBuffer.putInt(mapOffset.get(answer)); // куда в вызвыающей функции положить ответ
                                    in.next();
                                }

                                //копируем в буфер точку начала нового блока в стеке(текущий top)
                                //11 movAbs [абс, абс]
                                command = 11;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(4).byteValue());
                                byteBuffer.put(Integer.valueOf(0).byteValue());
                                //Выписываем код выкладывания на стек аргументов в спомогательный буфер:
                                str = in.next();
                                ByteBuffer buf = ByteBuffer.allocate(256);
                                Integer numberOfArg = 0;
                                ++offset;//учли, что после переменных вызывающий функции на стеке лежат три вспомогательных инта
                                ++offset;
                                ++offset;
                                while (!str.equals(")")) {
                                    command = 10;
                                    buf.put(command);
                                    buf.put(Integer.valueOf(0).byteValue());
                                    command = 3; // mov
                                    buf.put(command);
                                    Byte offsetOfArg = mapOffset.get(str);
                                    buf.put(offset);
                                    buf.put(offsetOfArg);
                                    buf.put(Integer.valueOf(0).byteValue());
                                    ++offset;
                                   // System.out.println(offset + "@@@@@@@@@@@@@@@@");
                                    ++numberOfArg;
                                    str = in.next();
                                }
                                command = 13; //pushConst int
                                byteBuffer.put(command);
                                Integer adr = byteBuffer.position();
                                byteBuffer.putInt(0); // позже напишем сюда точку возврата
                                command = 13; //pushConst int
                                byteBuffer.put(command);
                                byteBuffer.putInt(0); // место для старого смещения
                                //указатель на предшествующий блок берём из соответствующей ячейки
                                //putAbs abs stack
                                command = 12;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(8).byteValue());
                                byteBuffer.put(Integer.valueOf(offset - numberOfArg - 1).byteValue());//смещение в стеке относительно относительно начала блока вызывающей функции, по которому должен лежать

                                //выкладываем код для записи в стек аргументов
                                for (byte i = 0; i < buf.position(); ++i) {
                                    byteBuffer.put(buf.get(i));
                                }

                                //ставим в этой ячейке актуальный указатель (он сейчас в буфере)
                                //11 mov abs abs
                                command = 11;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(8).byteValue());
                                byteBuffer.put(Integer.valueOf(4).byteValue());
                                command = 15;//передаём управление
                                byteBuffer.put(command);
                                byteBuffer.putInt(addressJump);
                                byteBuffer.putInt(adr, byteBuffer.position());

                                break;
                            }
                            case "MOV":
                            {
                                command = 3;
                                byteBuffer.put(command);
                                String firstArg = in.next();
                                String secondArg = in.next();
                                byteBuffer.put(mapOffset.get(firstArg));
                                byteBuffer.put(mapOffset.get(secondArg));
                                byteBuffer.put(Integer.valueOf(0).byteValue());
                                break;
                            }
                            case "INPUT":
                            {
                                command = 1;
                                byteBuffer.put(command);
                                String firstArg = in.next();
                                byteBuffer.put(mapOffset.get(firstArg));
                                byteBuffer.putShort(Integer.valueOf(0).shortValue());
                                break;
                            }
                            case "OUTPUT":
                            {
                                command = 2;
                                byteBuffer.put(command);
                                String f = in.next();
                                if (f.equals("s")) {
                                    String firstArg = in.next();
                                    byteBuffer.put(Integer.valueOf(1).byteValue());
                                    byteBuffer.put(mapAddress.get(firstArg));
                                }
                                if (f.equals("d")) {
                                    String firstArg = in.next();
                                    byteBuffer.put(Integer.valueOf(0).byteValue());
                                    byteBuffer.put(mapOffset.get(firstArg));
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
                                    byteBuffer.put(mapOffset.get(arg));
                                }
                                break;
                            }
                            case "DEDUCT":
                            {
                                command = 5;
                                byteBuffer.put(command);
                                for (int i = 0; i < 3; ++i) {
                                    String arg = in.next();
                                    byteBuffer.put(mapOffset.get(arg));
                                }
                                break;
                            }
                            case "IFLESS":
                            {
                                command = 7;
                                byteBuffer.put(command);
                                for (int i = 0; i < 2; ++i) {
                                    String arg = in.next();
                                    byteBuffer.put(mapOffset.get(arg));
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
                                flag = false;
                                break;
                            }
                            default:
                            {
                                break;
                            }

                        }
                    }
                    break;
                }
                case "FUNC":
                {
                    str = in.next();
                    Byte addressCodeFoo = mapAddress.get(str);//по имени функции получили место,
                        // где хранится адресс на начало её кода
                    byteBuffer.putInt(addressCodeFoo, byteBuffer.position());// записали туда адресс кода, кооторый сейчас пишем
                    str = in.next();
                    Byte offset = 2;
                    HashMap<String, Byte> mapOffset = new HashMap<>();
                    if (str.equals( "(")) { //аргументы записываются до передачи управления данной функции (т.е. они уже записаны)
                        str = in.next();
                        while (!str.equals(")")) {
                            mapOffset.put(str, offset); //заносим их для сопоставления именам переменных смещения
                            ++offset;
                            str = in.next();
                        }
                    }
                    str = in.next();
                    //пишем в стек локальные переменные и константы
                    command = 10;
                    while (!str.equals(":")) {
                        byteBuffer.put(command);
                        mapOffset.put(str, offset);
                        ++offset;
                        if (mapConstNameValue.containsKey(str)) {
                            byteBuffer.put(mapAddress.get(str));
                        } else {
                            byteBuffer.put(Integer.valueOf(0).byteValue());

                        }
                        str = in.next();
                    }
                    boolean flag = true;
                    while (in.hasNext() && flag) {
                        str = in.next();
                        switch (str) {
                            case "MOV":
                            {
                                command = 3;
                                byteBuffer.put(command);
                                String firstArg = in.next();
                                String secondArg = in.next();
                                //  System.out.println(secondArg);
                                byteBuffer.put(mapOffset.get(firstArg));
                                byteBuffer.put(mapOffset.get(secondArg));
                                byteBuffer.put(Integer.valueOf(0).byteValue());
                                break;
                            }
                            case "INPUT":
                            {
                                command = 1;
                                byteBuffer.put(command);
                                String firstArg = in.next();
                                byteBuffer.put(mapOffset.get(firstArg));
                                byteBuffer.putShort(Integer.valueOf(0).shortValue());
                                break;
                            }
                            case "OUTPUT":
                            {
                                command = 2;
                                byteBuffer.put(command);
                                String f = in.next();
                                if (f.equals("s")) {
                                    String firstArg = in.next();
                                    byteBuffer.put(Integer.valueOf(1).byteValue());
                                    byteBuffer.put(mapAddress.get(firstArg));
                                }
                                if (f.equals("d")) {
                                    String firstArg = in.next();
                                    byteBuffer.put(Integer.valueOf(0).byteValue());
                                    byteBuffer.put(mapOffset.get(firstArg));
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
                                    byteBuffer.put(mapOffset.get(arg));
                                }
                                break;
                            }
                            case "DEDUCT":
                            {
                                command = 5;
                                byteBuffer.put(command);
                                for (int i = 0; i < 3; ++i) {
                                    String arg = in.next();
                                    byteBuffer.put(mapOffset.get(arg));
                                }
                                break;
                            }
                            case "IFLESS":
                            {
                                command = 7;
                                byteBuffer.put(command);
                                for (int i = 0; i < 2; ++i) {
                                    String arg = in.next();
                                    System.out.println(arg + mapOffset.get(arg));
                                    byteBuffer.put(mapOffset.get(arg));
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
                            case "CALL":
                            {
                                String name = in.next();

                                Integer addressJump = byteBuffer.getInt(mapAddress.get(name)); // адресс адреса кода вызываемой функции
                                String answer = in.next();

                                command = 13;//pushConst
                                byteBuffer.put(command);
                                if (answer.equals("(")) {
                                    byteBuffer.putInt(0);//НЕТ ВОЗВРАЩАЕМОГО ЗНАЧЕНИЯ
                                } else {
                                    //System.out.println(answer+"!!");
                                    byteBuffer.putInt(mapOffset.get(answer)); // куда в вызвыающей функции положить ответ
                                    in.next();
                                }

                                //копируем в буфер точку начала нового блока в стеке(текущий top)
                                //11 movAbs [абс, абс]
                                command = 11;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(4).byteValue());
                                byteBuffer.put(Integer.valueOf(0).byteValue());
                                //Выписываем код выкладывания на стек аргументов в спомогательный буфер:
                                str = in.next();
                                ByteBuffer buf = ByteBuffer.allocate(256);
                                Integer numberOfArg = 0;
                                ++offset;//учли, что после переменных вызывающий функции на стеке лежат три вспомогательных инта
                                ++offset;
                                ++offset;
                                while (!str.equals(")")) {
                                    command = 10;
                                    buf.put(command);
                                    buf.put(Integer.valueOf(0).byteValue());
                                    command = 3; // mov
                                    buf.put(command);
                                    Byte offsetOfArg = mapOffset.get(str);
                                    buf.put(offset);
                                    buf.put(offsetOfArg);
                                    buf.put(Integer.valueOf(0).byteValue());
                                    ++offset;
                                    // System.out.println(offset + "@@@@@@@@@@@@@@@@");
                                    ++numberOfArg;
                                    str = in.next();
                                }
                                command = 13; //pushConst int
                                byteBuffer.put(command);
                                Integer adr = byteBuffer.position();
                                byteBuffer.putInt(0); // позже напишем сюда точку возврата
                                command = 13; //pushConst int
                                byteBuffer.put(command);
                                byteBuffer.putInt(0); // место для старого смещения
                                //указатель на предшествующий блок берём из соответствующей ячейки
                                //putAbs abs stack
                                command = 12;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(8).byteValue());
                                byteBuffer.put(Integer.valueOf(offset - numberOfArg - 1).byteValue());//смещение в стеке относительно относительно начала блока вызывающей функции, по которому должен лежать

                                //выкладываем код для записи в стек аргументов
                                for (byte i = 0; i < buf.position(); ++i) {
                                    byteBuffer.put(buf.get(i));
                                }

                                //ставим в этой ячейке актуальный указатель (он сейчас в буфере)
                                //11 mov abs abs
                                command = 11;
                                byteBuffer.put(command);
                                byteBuffer.put(Integer.valueOf(8).byteValue());
                                byteBuffer.put(Integer.valueOf(4).byteValue());
                                command = 15;//передаём управление
                                byteBuffer.put(command);
                                byteBuffer.putInt(addressJump);
                                byteBuffer.putInt(adr, byteBuffer.position());

                                break;
                            }
                            case "RETURN":
                            {
                                command = 14;
                                byteBuffer.put(command);
                                String name = in.next();
                                if (mapOffset.containsKey(name)) {
                                    byteBuffer.put(mapOffset.get(name));
                                } else {
                                    byteBuffer.put(Integer.valueOf(0).byteValue());
                                }
                                //скопировать ответ в buf
                                // прочитать указатель на пред блок по смещению 1 и выписать его:
                                // прочитать carriagePos по смещению 0
                                //записать ответ в нужное место
                                flag = false;
                                break;
                            }
                            default:
                            {
                                break;
                            }

                        }
                    }
                    break;
                }
            }
        }
        byteBuffer.putInt(8, byteBuffer.position()); //stackPointerOfCurrentFunc
        byteBuffer.putInt(4, 0); //место для ответа, возвращаемого функцией
        byteBuffer.putInt(0, byteBuffer.position());//указатель на top стека
        Path path = Paths.get(aFileName + "_bytecode");
        Files.write(path, byteBuffer.array());
    }


}