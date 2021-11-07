import backend.Backend;
import frontend.Frontend;

import java.io.IOException;

public class Compiler {
    public static void main(String[] argv) throws IOException {
        Frontend frontend = new Frontend();
        Backend backend = new Backend(frontend.getIrCodes());
    }
}
