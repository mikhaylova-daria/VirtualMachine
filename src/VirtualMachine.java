/**
 * Created by daria on 08.03.15.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

public class VirtualMachine {

    private ByteBuffer byteCode;
    private  Vector<ArrayList<Byte>> garbageCollector = new Vector<>();
    Integer carriagePos;
    Scanner input;
    Integer stackPointer;
    Integer varMemoryPointer;

    VirtualMachine() {
        input = new Scanner(System.in);
    }

    private void incOffset(int i) {
        for (int j = 0; j < i; ++j) {
            ++carriagePos;
        }
    }


    private Integer getAddress() {
        byte refer = byteCode.get(carriagePos); //читаем сcылку на ячейку, в которой лежит значение смещения в стеке
        // для последнего значения переменной
        //если ссылка принадлежит области храниения констант, то она и является адрессом
        if (refer < varMemoryPointer) {
            ++carriagePos;
            return 0 + refer;
        }
        byte offsetInStack = byteCode.get(refer); // чиатем смещение
        ++carriagePos;
        return offsetInStack * 5 + stackPointer + 1; //вычисляем абсолютный адресс значения
    }

    void read(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        //считали бинарник
        byteCode = ByteBuffer.wrap(Files.readAllBytes(path));
        stackPointer = byteCode.getInt();
        carriagePos = Integer.valueOf(byteCode.getShort());
        varMemoryPointer = Integer.valueOf(byteCode.getShort());
        //выделяем место для переменных на стеке
        for (int i = varMemoryPointer; i < carriagePos; ++i) {
            byteCode.put(i, Integer.valueOf(i - varMemoryPointer).byteValue());
        }

    }



    private void inputVal() throws Exception {
        ++carriagePos;
        Integer address = getAddress();
        incOffset(2);
        Integer arg = input.nextInt();
        byteCode.putInt(address, arg);
    }

    private void ifLess() {
        ++carriagePos;
        Integer addressFirstArg = getAddress();
        Integer addressSecondArg = getAddress();
        if (byteCode.getInt(addressFirstArg) < byteCode.getInt(addressSecondArg)) {
            ++carriagePos;
        } else {
            carriagePos = Integer.valueOf(byteCode.get(carriagePos)); // jump
        }
    }

    private void goTo() {
        ++carriagePos;
        carriagePos = Integer.valueOf(byteCode.get(carriagePos));//jump
    }

    private void deduct() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfFirstArg = getAddress();
        Integer addressOfSecondArg = getAddress();
        Integer theFirstArg = byteCode.getInt(addressOfFirstArg);
        Integer theSecondArg = byteCode.getInt(addressOfSecondArg);
        byteCode.putInt(addressOfResult, theFirstArg - theSecondArg);
    }

    void mov() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfArgument = getAddress();
        ++carriagePos;
        Integer argument = byteCode.getInt(addressOfArgument);
        byteCode.putInt(addressOfResult, argument);
    }

    void output() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer argument = byteCode.getInt(addressOfResult);
        System.out.println(argument);
        carriagePos+=2;
    }

    void add() {//ответ, арг, арг
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfFirstArg = getAddress();
        Integer addressOfSecondArg = getAddress();
        Integer theFirstArg = byteCode.getInt(addressOfFirstArg);
        Integer theSecondArg = byteCode.getInt(addressOfSecondArg);
        byteCode.putInt(addressOfResult, theFirstArg + theSecondArg);
    }

    void execute() throws Exception {
        Integer length = byteCode.array().length;
        while (carriagePos < length) {
            Byte commandCode = byteCode.get(carriagePos);
        //    System.out.println(commandCode);
            switch (commandCode) {
                case 1:
                {
                    inputVal();
                    break;
                }
                case 2:
                {
                    output();
                    break;
                }
                case 3:
                {
                    mov();
                    break;
                }
                case 4:
                {
                    add();
                    break;
                }
                case 5:
                {
                    deduct();
                    break;
                }
                case 6:
                {
                    goTo();
                    break;
                }
                case 7:
                {
                    ifLess();
                    break;
                }
                case 8:
                    System.exit(0);
                    break;
            }
        }
    }
}
