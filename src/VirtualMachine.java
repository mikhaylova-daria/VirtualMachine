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

    private ArrayList<Vector<Integer> > stackMemoryVar = new ArrayList<>();
    private ArrayList<Integer> memoryConst;
    private ByteBuffer byteCode;
    private Byte offsetConstMemory;
    private Byte offsetMain;
    private  Vector<ArrayList<Byte>> garbageCollector = new Vector<>();
    Byte carriagePos;
    Vector<Byte> callStack = new Vector<>();
    Scanner input;

    VirtualMachine() {
        input = new Scanner(System.in);
    }

    private void incOffset(int i) {
        for (int j = 0; j < i; ++j) {
            ++carriagePos;
        }
    }
    void read(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        //считали бинарник
        byteCode = ByteBuffer.wrap(Files.readAllBytes(path));
    }



    private void inputVal() throws Exception {
        ++carriagePos;
        byte address = byteCode.get(carriagePos);
        incOffset(3);
        Integer arg = input.nextInt();
        byteCode.putInt(address, arg);
    }

    private void ifLess() {
        ++carriagePos;
        byte addressFirstArg = byteCode.get(carriagePos);
        ++carriagePos;
        byte addressSecondArg = byteCode.get(carriagePos);
        if (byteCode.getInt(addressFirstArg) < byteCode.getInt(addressSecondArg)) {
            incOffset(2);
        } else {
            ++carriagePos;
            byte addressForJump = byteCode.get(carriagePos);
            carriagePos = addressForJump;
        }
    }

    private void goTo() {
        ++carriagePos;
        byte addressForJump = byteCode.get(carriagePos);
        carriagePos = addressForJump;
    }

    private void deduct() {
        ++carriagePos;
        byte addressOfResult = byteCode.get(carriagePos);
        ++carriagePos;
        Integer theFirstArg = byteCode.getInt(byteCode.get(carriagePos));
        ++carriagePos;
        Integer theSecondArg = byteCode.getInt(byteCode.get(carriagePos));
        byteCode.putInt(addressOfResult, theFirstArg - theSecondArg);
        ++carriagePos;

    }

    void mov() {
        ++carriagePos;
        byte addressOfResult = byteCode.get(carriagePos);
        ++carriagePos;
        byte addressOfArgument = byteCode.get(carriagePos);
        incOffset(2);
        Integer argument = byteCode.getInt(addressOfArgument);
        byteCode.putInt(addressOfResult, argument);
    }

    void output() {
        ++carriagePos;
        byte addressOfResult = byteCode.get(carriagePos);
        Integer argument = byteCode.getInt(addressOfResult);
        System.out.println(argument);
        incOffset(3);
    }

    void add() {//ответ, арг, арг
        ++carriagePos;
        byte addressOfResult = byteCode.get(carriagePos);
        ++carriagePos;
        Integer theFirstArg = byteCode.getInt(byteCode.get(carriagePos));
        ++carriagePos;
        Integer theSecondArg = byteCode.getInt(byteCode.get(carriagePos));
        byteCode.putInt(addressOfResult, theFirstArg + theSecondArg);
        ++carriagePos;
    }
    
    void execute() throws Exception {
        carriagePos = byteCode.get();
        Integer length = byteCode.array().length;
        while (carriagePos < length) {
            Byte commandCode = byteCode.get(carriagePos);
            byte[] args = new byte[3];
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
                default:
                    incOffset(3);
                    break;
            }
        }
    }
}
