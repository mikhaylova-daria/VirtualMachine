/**
 * Created by daria on 08.03.15.
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

public class VirtualMachine {

    private ByteBuffer byteCode;
    Integer carriagePos;
    Scanner in;
    HashMap<Byte, String> commands = new HashMap<>();

    VirtualMachine() {
        in = new Scanner(System.in);
        Byte command = 1;
        commands.put(command, "input");
        ++command;
        commands.put(command, "output");
        ++command;
        commands.put(command, "mov");
        ++command;
        commands.put(command, "add");
        ++command;
        commands.put(command, "deduct");
        ++command;
        commands.put(command, "goTo");
        ++command;
        commands.put(command, "ifLess");
        ++command;
        //команда 8 - это exit
        ++command;
        commands.put(command, "label");
        ++command;
        commands.put(command, "push");
        ++command;
        commands.put(command, "movAbs");
        ++command;
        commands.put(command, "putAbs");
        ++command;
        commands.put(command, "pushConst");
        ++command;
        commands.put(command, "ret");
        ++command;
        commands.put(command, "transferControl");
    }


    void execute() throws Exception {
        Byte commandCode = byteCode.get(carriagePos);
        while (commandCode != 8) {
            if (commands.containsKey(commandCode)) {
                Method currentMethod = this.getClass().getMethod(commands.get(commandCode));
                currentMethod.invoke(this);
            }
            commandCode = byteCode.get(carriagePos);
        }
    }

    void read(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        byteCode = ByteBuffer.wrap(Files.readAllBytes(path));
        carriagePos = byteCode.getInt(12);
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

    private Integer getLabel() {
        byte refer = byteCode.get(carriagePos);
        ++carriagePos;
        return 0 + refer;
    }

    public void input() throws Exception {
        ++carriagePos;
        Integer address = getAddress();
        incOffset(2);
        Integer arg = in.nextInt();
        byteCode.putInt(address, arg);
    }

    public void ifLess() {
        ++carriagePos;
        Integer addressFirstArg = getAddress();
        Integer addressSecondArg = getAddress();
        Integer addressOfLabel = getLabel();
        if (byteCode.getInt(addressFirstArg) < byteCode.getInt(addressSecondArg)) {
        } else {
            carriagePos = byteCode.getInt(addressOfLabel); // jump
        }
    }

    public void goTo() {
        ++carriagePos;
        Integer addressOfLabel = getLabel();//jump
        carriagePos = byteCode.getInt(addressOfLabel);
    }

    public void deduct() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfFirstArg = getAddress();
        Integer addressOfSecondArg = getAddress();
        Integer theFirstArg = byteCode.getInt(addressOfFirstArg);
        Integer theSecondArg = byteCode.getInt(addressOfSecondArg);
        byteCode.putInt(addressOfResult, theFirstArg - theSecondArg);
    }

    public void mov() {
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfArgument = getAddress();
        ++carriagePos;
        Integer argument = byteCode.getInt(addressOfArgument);
        byteCode.putInt(addressOfResult, argument);
    }

    public void output() {
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
            addressOfResult += 4;
            for (int i = 0; i < lengthOfString; ++i) {
                System.out.print(byteCode.getChar(addressOfResult));
                addressOfResult += 2;
            }
            System.out.println();
            ++carriagePos;
        }
        carriagePos+=1;
    }

    public void add() {//ответ, арг, арг
        ++carriagePos;
        Integer addressOfResult = getAddress();
        Integer addressOfFirstArg = getAddress();
        Integer addressOfSecondArg = getAddress();
        Integer theFirstArg = byteCode.getInt(addressOfFirstArg);
        Integer theSecondArg = byteCode.getInt(addressOfSecondArg);
        byteCode.putInt(addressOfResult, theFirstArg + theSecondArg);
    }

    public void push() throws  Exception {
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

    public void ret() throws Exception {
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

    public  void movAbs() {
        ++carriagePos;
        Byte addressOfFirstArg = byteCode.get(carriagePos);
        ++carriagePos;
        Byte addressOfSecondArg = byteCode.get(carriagePos);
        byteCode.putInt(addressOfFirstArg, byteCode.getInt(addressOfSecondArg));
        ++carriagePos;
    }

    public void putAbs() {
        ++carriagePos;
        Byte addressOfArg = byteCode.get(carriagePos);
        ++carriagePos;
        Integer addressOfStackPos = byteCode.get(carriagePos) * 4 + byteCode.getInt(Integer.valueOf(8).byteValue());
        ++carriagePos;
        byteCode.putInt(addressOfStackPos, byteCode.getInt(addressOfArg));
    }

    public void pushConst() {
        ++carriagePos;
        Integer arg = byteCode.getInt(carriagePos);
        for (int i = 0; i < 4; ++i) {
            ++carriagePos;
        }
        byteCode.putInt(byteCode.getInt(0), arg);
        byteCode.putInt(0, byteCode.getInt(0) + 4);

    }

    public void transferControl() {
        ++carriagePos;
        carriagePos = byteCode.getInt(carriagePos);
    }

    public void label() {
        carriagePos += 4;
    }
}
