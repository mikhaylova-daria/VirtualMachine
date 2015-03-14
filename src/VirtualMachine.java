/**
 * Created by daria on 08.03.15.
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class VirtualMachine {

    private ByteBuffer byteCode;
    Integer carriagePos;
    Scanner input;
    Integer stackPointer;
    Integer stackSize;
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
        stackSize = (carriagePos - varMemoryPointer) * 5;
    }

    private void callFunction() {
        ++carriagePos;
        Integer addressOfNumberOfArgument = getAddress();
        Integer numberOfArgument = byteCode.getInt(addressOfNumberOfArgument);
        byte addressOfReturnValue = byteCode.get(carriagePos);
        ++carriagePos;
        byteCode.put(stackSize + stackPointer, addressOfReturnValue); //адресс возвращаемого значения
        stackSize += 1;
        byteCode.putInt(stackSize + stackPointer, carriagePos + numberOfArgument + 2); //адресс возврата
        stackSize += 4;
        for (int i = 0; i < numberOfArgument - 1; ++i) {
            Integer addressOfArgument = getAddress(); //аргументы
            Integer argument = byteCode.getInt(addressOfArgument);
            byteCode.putInt(stackSize + stackPointer + 1, argument);
            stackSize += 5;
        }
        Integer addressOfFunction = getAddress();
        carriagePos = byteCode.getInt(addressOfFunction);//jump
        byteCode.put(stackSize + stackPointer, Integer.valueOf(0).byteValue());//
        ++stackSize;
        byteCode.putInt(stackSize + stackPointer, numberOfArgument); //число аргументов
        stackSize += 4;
//        byteCode.put(stackSize + stackPointer, Integer.valueOf(0).byteValue());//
//        ++stackSize;
//        byteCode.putInt(stackSize + stackPointer, carriagePos); //start point функции
//        stackSize += 4;
    }
//callstack: адрес возвращаемого значения (byte), адрес возврата(int), аргументы - по 5 byte на каждый, 00 byte,
// число аргументов - int , byte 00, int - startPoint текущей функции
    private void reinit() {
        Integer currentStartPoint = carriagePos;
        ++carriagePos;
        byte offsetOfCode = byteCode.get(carriagePos);
        ++carriagePos;
        //обрабатываем аргументы
        Integer numberOfArgument = byteCode.getInt(stackSize + stackPointer - 9);
        for (byte i = 0; i < numberOfArgument; ++i) {
            byte lastAddress = byteCode.get(carriagePos);
            byteCode.put(stackPointer + stackSize - (i+2) * 5, lastAddress); // сохраняем рядом с новым значением
            // ссылку на место в стеке предыдущего
            byteCode.put(carriagePos, Integer.valueOf(stackSize / 5 - (i + 2)).byteValue()); // в рабочие перменые
            // записываем ссылку на место в стеке актуальных значений
        }
        for (int i= numberOfArgument; i < offsetOfCode; ++i) {
            byte lastAddress = byteCode.get(carriagePos);
            byteCode.put(stackPointer + stackSize, lastAddress); // сохраняем рядом с новым значением
            // ссылку на место в стеке предыдущего
            ++stackSize;
            byteCode.put(carriagePos, Integer.valueOf(stackSize / 5).byteValue()); // в рабочие перменые
            // записываем ссылку на место в стеке актуальных значений
            stackSize += 4;//выделили место на стеке для новой переменной
        }
        byteCode.put(stackSize + stackPointer, Integer.valueOf(0).byteValue());//
        ++stackSize;
        byteCode.putInt(stackSize + stackPointer, currentStartPoint); //startPoint данной функции
        stackSize += 4;

    }

    private void returnFunc() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer result = byteCode.getInt(addressOfResult);
        carriagePos = byteCode.getInt(stackPointer + stackSize - 4);//вернулись в начало функции, чтобы освободить память
        byteCode.putInt(stackPointer + stackSize - 4, 0);//сняли со стека startPoint
        stackSize -= 5;
        ++carriagePos;
        byte offsetOfCode = byteCode.get(carriagePos);
        ++carriagePos;
        for (int i = 0; i < offsetOfCode; ++i) {
            byte lastAddress = byteCode.get(stackPointer + stackSize - 5);
            byteCode.putInt(stackPointer + stackSize - 4, 0);//снимаем со стека перемнную
            byteCode.put(stackPointer + stackSize - 5, Integer.valueOf(0).byteValue());//снимаем со стека lastAdress
            stackSize -= 5;
            byteCode.put(carriagePos, lastAddress); //разместили в ячейки, соответствующей переменной, ссылку на актуальное значение в стеке
        }
        carriagePos = byteCode.getInt(stackPointer + stackSize - 4);//вернули управление в исходную функцию
        byteCode.putInt(stackPointer + stackSize - 4, 0);//сняли со стека точку возврата
        stackSize -= 4;
        int buf = carriagePos;
        carriagePos = stackPointer + stackSize - 1;
        Integer addressOfReturnValue = getAddress();
        byteCode.putInt(addressOfReturnValue, result);//записали результат
        byteCode.putInt(stackPointer + stackSize - 1, 0);//сняли со стека возвращаемое значение
        stackSize -= 1;
        carriagePos = buf;//вернули управление в исходную функцию
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
        Integer addressOfLabel = getAddress();
        if (byteCode.getInt(addressFirstArg) < byteCode.getInt(addressSecondArg)) {
        } else {
            carriagePos = byteCode.getInt(addressOfLabel); // jump
        }
    }

    private void goTo() {
        ++carriagePos;
        Integer addressOfLabel = getAddress();//jump
        carriagePos = byteCode.getInt(addressOfLabel);
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
       // System.out.println("address" + addressOfResult + " arg "+ argument);
        byteCode.putInt(addressOfResult, argument);
    }

    void output() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        byte flag = byteCode.get(carriagePos);
        if (flag == 0) {
            Integer argument = byteCode.getInt(addressOfResult);
            System.out.println(argument);
        } else {
            Integer lengthOfString = byteCode.getInt(addressOfResult);
            addressOfResult += 4;
            for (int i = 0; i < lengthOfString; ++i) {
                System.out.print(byteCode.getChar(addressOfResult));
                addressOfResult += 2;
            }
        }
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
          //  System.out.println(commandCode);
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
                {
                    System.exit(0);
                    break;
                }
                case 9:
                {
                    carriagePos += 4;
                    break;
                }
            }
        }
    }
}
