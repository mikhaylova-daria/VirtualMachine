/**
 * Created by daria on 08.03.15.
 */
public class Main {
    public static void main(String[] arg) throws Exception {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.read("/home/daria/Загрузки/-Untitled-");
        virtualMachine.execute();

    }
}