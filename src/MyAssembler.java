import java.io.File;
import java.lang.reflect.Method;
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

    private ArrayList<String> nameSpaceConst = new ArrayList<>();
    private ArrayList<String> nameSpaceConstString = new ArrayList<>();
    private ArrayList<String> nameSpaceFunc = new ArrayList<>();
    private HashMap<String, Integer> mapConstNameValue = new HashMap<>();
    private ArrayList<String> nameSpaceLabel = new ArrayList<>();
    private HashMap<String, String> mapConstStringNameValue = new HashMap<>();
    private HashMap<String, Byte> mapAddress = new HashMap<>();
    private Scanner in;
    private ByteBuffer byteBuffer;
    private HashMap<String, String> commands;
    private HashMap<String, String> namespaceProcessing;
    private String fileName;
    private Byte offset;
    private HashMap <String, Byte> mapOffset;


    MyAssembler(String aFileName) throws Exception {
        fileName = aFileName;
        byteBuffer = ByteBuffer.allocate(4096);
        commands = new HashMap<>();
        namespaceProcessing = new HashMap<>();
        namespaceProcessing.put("CONST", "constVar");
        namespaceProcessing.put("LABEL", "label");
        namespaceProcessing.put("CSTRING", "constString");
        namespaceProcessing.put("FUNC", "funcName");
        commands.put("CALL", "call");
        commands.put("MOV", "mov");
        commands.put("INPUT", "input");
        commands.put("OUTPUT", "output");
        commands.put("ADD", "add");
        commands.put("DEDUCT", "deduct");
        commands.put("IFLESS", "ifLess");
        commands.put("JUMP", "jump");
        commands.put("LABEL", "labelInCode");
        commands.put("EXIT", "exitCommand");
        commands.put("RETURN", "returnCommand");

    }


    void makeByteCode() throws Exception {
        formationOfConstAndServiceBlock();
        formationOfCodeBlock();
        Path path = Paths.get(fileName + "_bytecode");
        Files.write(path, byteBuffer.array());
    }


    private void formationOfConstAndServiceBlock() throws Exception {
        in = new Scanner(new File(fileName));
        //связывание имён функций и констант с ячейкой памяти
        while (in.hasNext()) {
            String str = in.next();
            if (namespaceProcessing.containsKey(str)) {
                Method currentMethod = this.getClass().getMethod(namespaceProcessing.get(str));
                currentMethod.invoke(this);
            }
        }

        /*формирование блока служебных ячеек: 0)
        * 0) указатель на вершину стека (0)
        * 1) ячейка для общения функций: сюда помещается ответ при свёртывании стека, также используется как буфер (4)
        * 2) ячейка для указателя на начало блока исполняемой функции в стеке (8)
        * 3) ячейка для указателя на начало main */


        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);
        byteBuffer.putInt(0);

        //связывание ячеек памяти с именами
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
        in.close();
    }

    private void formationOfCodeBlock() throws Exception {
        in = new Scanner(new File(fileName));
        Byte command;
        while (in.hasNext()) {
            String str = in.next();
            switch (str) {
                case "MAIN"://без аргументов!!::
                {
                    byteBuffer.putInt(12, byteBuffer.position());
                    command = 10; //push [ссылка на инт или 0(значит кладём нулевой инт)]
                    str = in.next();
                    mapOffset = new HashMap<>();
                    offset = 0;
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
                    offset = 0;
                    while (in.hasNext()) {
                        str = in.next();
                        if (commands.containsKey(str)) {
                            Method currentMethod = this.getClass().getMethod(commands.get(str));
                            currentMethod.invoke(this);
                            if (str.equals("EXIT")) break;
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
                    offset = 2;
                    mapOffset = new HashMap<>();
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
                    while (in.hasNext()) {
                        str = in.next();
                        if (commands.containsKey(str)) {
                            offset = 2;
                            Method currentMethod = this.getClass().getMethod(commands.get(str));
                            currentMethod.invoke(this);
                            if (str.equals("RETURN")) break;
                        }
                    }
                    break;
                }
            }
        }
        byteBuffer.putInt(8, byteBuffer.position()); //stackPointerOfCurrentFunc
        byteBuffer.putInt(4, 0); //место для ответа, возвращаемого функцией
        byteBuffer.putInt(0, byteBuffer.position());//указатель на top стека
        in.close();

    }



    public void constVar() {
        String name = in.next();
        Integer val = in.nextInt();
        nameSpaceConst.add(name);
        mapConstNameValue.put(name, val);
    }

    public void label() {
        String name = in.next();
        nameSpaceLabel.add(name);
    }

    public void constString() {
        String name = in.next();
        nameSpaceConstString.add(name);
        String value = in.findInLine("\".*.\"").split("\"")[1];
        mapConstStringNameValue.put(name, value);
    }

    public void funcName() {
        String name = in.next();
        nameSpaceFunc.add(name);
    }

    public void call() {
            String nameFoo = in.next();
            Integer addressJump = byteBuffer.getInt(mapAddress.get(nameFoo)); // адресс адреса кода вызываемой функции
            String answer = in.next();
            Byte command = 13;//pushConst
            byteBuffer.put(command);
            if (answer.equals("(")) {
                byteBuffer.putInt(0);//НЕТ ВОЗВРАЩАЕМОГО ЗНАЧЕНИЯ
            } else {
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
            String str = in.next();
            ByteBuffer buf = ByteBuffer.allocate(256);
        offset =  Integer.valueOf(mapOffset.size() + offset).byteValue();
                ++offset;//учли, что после переменных вызывающий функции на стеке лежат три вспомогательных инта
            ++offset;
            Byte address = offset;
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
            byteBuffer.put(Integer.valueOf(address).byteValue());//смещение в стеке относительно относительно начала блока вызывающей функции, по которому должен лежать

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
    }

    public void mov() {
        Byte command = 3;
        byteBuffer.put(command);
        String firstArg = in.next();
        String secondArg = in.next();
        byteBuffer.put(mapOffset.get(firstArg));
        byteBuffer.put(mapOffset.get(secondArg));
        byteBuffer.put(Integer.valueOf(0).byteValue());
    }

    public void input() {
        Byte command = 1;
        byteBuffer.put(command);
        String firstArg = in.next();
        byteBuffer.put(mapOffset.get(firstArg));
        byteBuffer.putShort(Integer.valueOf(0).shortValue());
    }

    public void output() {
        Byte command = 2;
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
    }

    public void add() {
        Byte command = 4;
        byteBuffer.put(command);
        for (int i = 0; i < 3; ++i) {
            String arg = in.next();
            byteBuffer.put(mapOffset.get(arg));
        }
    }

    public void deduct() {
        Byte command = 5;
        byteBuffer.put(command);
        for (int i = 0; i < 3; ++i) {
            String arg = in.next();
            byteBuffer.put(mapOffset.get(arg));
        }
    }

    public void ifLess() {
        Byte command = 7;
        byteBuffer.put(command);
        for (int i = 0; i < 2; ++i) {
            String arg = in.next();
            byteBuffer.put(mapOffset.get(arg));
        }
        String nameOfLabel;
        nameOfLabel = in.next();
        byteBuffer.put(mapAddress.get(nameOfLabel));
    }

    public void jump() {
        Byte command = 6;
        byteBuffer.put(command);
        String arg = in.next();
        byteBuffer.put(mapAddress.get(arg));
    }

    public void labelInCode() {
        byteBuffer.putInt(0);
        Byte command = 9;
        byteBuffer.put(byteBuffer.position() - 4, command);
        String arg = in.next();
        byteBuffer.putInt(mapAddress.get(arg), byteBuffer.position()); //записали значение метки
    }

    public void exitCommand() {
        byteBuffer.putInt(0);
        Byte command = 8;
        byteBuffer.put(byteBuffer.position() - 4, command);
    }

    public void returnCommand() {
        Byte command = 14;
        byteBuffer.put(command);
        String name = in.next();
        if (mapOffset.containsKey(name)) {
            byteBuffer.put(mapOffset.get(name));
        } else {
            byteBuffer.put(Integer.valueOf(0).byteValue());
        }
    }
}
