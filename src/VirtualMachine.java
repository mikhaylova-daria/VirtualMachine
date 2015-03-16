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

    VirtualMachine() {
        input = new Scanner(System.in);
    }

    private void incOffset(int i) {
        for (int j = 0; j < i; ++j) {
            ++carriagePos;
        }
    }


    private Integer getAddress() {
        byte refer = byteCode.get(carriagePos);
        ++carriagePos;
        return byteCode.getInt(8) + refer * 4;
    }

    private  Integer getLabel() {
        byte refer = byteCode.get(carriagePos);
        ++carriagePos;
        return 0 + refer;
    }

    void read(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        //считали бинарник
        byteCode = ByteBuffer.wrap(Files.readAllBytes(path));
        carriagePos = byteCode.getInt(12);
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
        Integer addressOfLabel = getLabel();
        if (byteCode.getInt(addressFirstArg) < byteCode.getInt(addressSecondArg)) {
        } else {
            carriagePos = byteCode.getInt(addressOfLabel); // jump
        }
    }

    private void goTo() {
        ++carriagePos;
        Integer addressOfLabel = getLabel();//jump
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
        byteCode.putInt(addressOfResult, argument);
    }

    void output() {
        ++carriagePos;
        byte flag = byteCode.get(carriagePos);
        ++carriagePos;
        if (flag == 0) {
            Integer addressOfResult = getAddress();
            Integer argument = byteCode.getInt(addressOfResult);
            System.out.println(argument);
        } else {
            Integer addressOfResult = byteCode.get(carriagePos) + 0;
            Integer lengthOfString = byteCode.getInt(addressOfResult);
            //System.out.println(lengthOfString);
            addressOfResult += 4;
            for (int i = 0; i < lengthOfString; ++i) {
                System.out.print(byteCode.getChar(addressOfResult));
                addressOfResult += 2;
            }
            ++carriagePos;
        }
        carriagePos+=1;
    }

    void add() {//ответ, арг, арг
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfFirstArg = getAddress();
        Integer addressOfSecondArg = getAddress();
        Integer theFirstArg = byteCode.getInt(addressOfFirstArg);
        Integer theSecondArg = byteCode.getInt(addressOfSecondArg);
 //       System.out.println("!"+addressOfResult+" "+ theFirstArg + theSecondArg);
        byteCode.putInt(addressOfResult, theFirstArg + theSecondArg);
    }

    private void push() throws  Exception {
        ++carriagePos;
        Byte offset = byteCode.get(carriagePos);
        Integer stackTopPointer = byteCode.getInt(0);
        if (offset == 0) {
            byteCode.putInt(stackTopPointer, 0);
            ++carriagePos;
        } else {
            Integer arg = byteCode.getInt(offset);
            byteCode.putInt(stackTopPointer, arg);
            ++carriagePos;
        }
        byteCode.putInt(0, byteCode.getInt(0) + 4);
    }

    private void ret() {
        //скопировать ответ в buf
        // прочитать указатель на пред блок по смещению 1 и выписать его:
        // прочитать carriagePos по смещению 0
        //записать ответ в нужное место
        ++carriagePos;
        Integer addressOfResult = byteCode.get(carriagePos)*4 + byteCode.getInt(8);
        byteCode.putInt(4, byteCode.getInt(addressOfResult)); //записали в регистр ответ
        carriagePos = byteCode.getInt(byteCode.getInt(8)); //вернули управление в вызывающую функцию
        byteCode.putInt(0, byteCode.getInt(8));//свернули стек
        Integer addressOfPreviousBlock = byteCode.getInt(byteCode.getInt(8) + 4);
        byteCode.putInt(8, addressOfPreviousBlock); //установили актуальное значение начала блока
        Integer offsetOfAnswer = byteCode.getInt(byteCode.getInt(0) - 4); // в конце стека лежало смещение на место для ответа
        Integer addressOfAnswer = byteCode.getInt(8) + offsetOfAnswer * 4;
        byteCode.putInt(addressOfAnswer, byteCode.getInt(4));//из буфера записали результат на стек
        byteCode.putInt(0, byteCode.getInt(0) - 4); //сняли со стека смещение для ответа
    }

    private void movAbs() {
        ++carriagePos;
        Byte addressOfFirstArg = byteCode.get(carriagePos);
        ++carriagePos;
        Byte addressOfSecondArg = byteCode.get(carriagePos);
        byteCode.putInt(addressOfFirstArg, byteCode.getInt(addressOfSecondArg));
        ++carriagePos;
    }

    private void putAbs() {
        ++carriagePos;
        Byte addressOfArg = byteCode.get(carriagePos);
        ++carriagePos;
        Integer addressOfStackPos = byteCode.get(carriagePos) * 4 + byteCode.getInt(Integer.valueOf(8).byteValue());
        ++carriagePos;
        byteCode.putInt(addressOfStackPos, byteCode.getInt(addressOfArg));
    }

    private void pushConst() {
        ++carriagePos;
        Integer arg = byteCode.getInt(carriagePos);
        for (int i = 0; i < 4; ++i) {
            ++carriagePos;
        }
        byteCode.putInt(byteCode.getInt(0), arg);
        byteCode.putInt(0, byteCode.getInt(0) + 4);

    }

    void execute() throws Exception {
        Integer length = byteCode.array().length;
        while (carriagePos < length) {
            Path path = Paths.get("./debug");
            Files.write(path, byteCode.array());
            Byte commandCode = byteCode.get(carriagePos);
           // System.out.println(commandCode+ " "+carriagePos) ;
            //input.next();
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
                case 10:
                {
                    push();
                    break;
                }
                case 11:
                {
                    movAbs();
                    break;
                }
                case 12: {
                    putAbs();
                    break;
                }
                case 13: {
                    pushConst();
                    break;
                }
                case 14:
                {
                    ret();
                    break;
                }
                case 15:
                {
                    ++carriagePos;
                    carriagePos = byteCode.getInt(carriagePos);
                    break;
                }
            }
        }
    }
}
