/**
 * Created by daria on 08.03.15.
 */
public class Main {
    public static void main(String[] arg) throws Exception {
        MyAssembler a = new MyAssembler("/home/daria/VirtualMachine/src/recursFib");
        a.makeByteCode();
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.read("/home/daria/VirtualMachine/src/recursFib_bytecode");
        virtualMachine.execute();
    }
}
