/**
 * Created by daria on 08.03.15.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class VirtualMachine {

    private ByteBuffer[] memoryProgram = new ByteBuffer[6];
    HashMap< Integer, String > commandsList;
    //private int startPointOfMain;
    private short recursionDepth = -1;
    private ArrayList<Byte> callStack = new ArrayList<>();
    private Byte carriagePos;

    VirtualMachine() {
        commandsList = new HashMap<Integer, String>();
        commandsList.put(0, "input");
        commandsList.put(1, "output");
        commandsList.put(2, "add");//ответ, арг, арг
        commandsList.put(3, "move");//ответ арг
        commandsList.put(4, "isEqual");//ответ, арг, арг
        commandsList.put(5, "ifTrueThenElse");
        commandsList.put(6, "enterToNewFunction"); //арг.: startPoint функции, ссылку на место, куда пишем результат, аргумент
                                                   // кладём в callStack позицию, следующую за местом вызова,
                                                   // а затем ссылку вызова+2, т.к. 2 байта - на внутренние парметры()
        commandsList.put(7, "exitFromCurrentFunction");//записать ответ и снять
                                                       // со стека вызовов последний startPoint
        commandsList.put(8, "deduct");




    }

    void output() {
        ++carriagePos;
        byte addressOfResult = memoryProgram[0].get(carriagePos);
        Integer argument = memoryProgram[recursionDepth].getInt(addressOfResult);
        System.out.println(argument);
        ++carriagePos;
    }

    void add() {
        ++carriagePos;
        byte addressOfResult = memoryProgram[0].get(carriagePos);
        ++carriagePos;
        Integer theFirstArg = memoryProgram[recursionDepth].getInt(memoryProgram[recursionDepth].get(carriagePos));
        ++carriagePos;
        Integer theSecondArg = memoryProgram[recursionDepth].getInt(memoryProgram[recursionDepth].get(carriagePos));
        memoryProgram[recursionDepth].putInt(addressOfResult, theFirstArg+theSecondArg);
        ++carriagePos;
    }

    void enterToNewFunction() {
        Integer pointReturn = carriagePos.intValue()+4;
        callStack.add(pointReturn.byteValue());//  позиция по возвращении
        ++carriagePos;
        Byte startPoint = memoryProgram[0].get(carriagePos);
        callStack.add(startPoint); //точка входа новой функции
        ++carriagePos;
      //  Byte argumentAdress =
        //callStack.add()
    }

    void read(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        //считали бинарник в память
        memoryProgram[0] = ByteBuffer.wrap(Files.readAllBytes(path));
        System.out.println(memoryProgram[0].toString());
        //считываем адресс начала действующего кода
        Integer startPointOfMain = memoryProgram[0].getInt();
        //блок памяти, отвечающий за переменные, размножаем до максимальной глубины рекурсии (у нас - 5)
        System.out.println(startPointOfMain);
        byte[] bufOfValueBlock = new byte[startPointOfMain - 4];
        System.out.println(bufOfValueBlock.length);
        memoryProgram[0].get(bufOfValueBlock, 0, startPointOfMain - 4);
        for (int i = 1; i < 6; ++i) {
            memoryProgram[i] = ByteBuffer.allocate(startPointOfMain - 4);
            memoryProgram[i].put(bufOfValueBlock);
        }
    }

    Integer getNumber(int address) {
        //ByteBuffer.getInt(int index) - Reads four bytes at the given index, composing them
        // into a int value according to the current byte order:
        return  memoryProgram[recursionDepth].getInt(address);
    }



    void execute() {
        Integer carriagePos = memoryProgram[0].getInt(0);
        Integer length = memoryProgram[0].array().length;
        while (carriagePos < length) {
            Byte commandCode = memoryProgram[0].get(carriagePos);
            byte[] args = new byte[3];
            switch (commandCode) {
                case 2:
                {
                    add();
                    break;
                }
                case 1:
                {
                    output();
                    break;
                }
                case 6:
                {
                    enterToNewFunction();
                    break;
                }
            }
        }
    }

}
