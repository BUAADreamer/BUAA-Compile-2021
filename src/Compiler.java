import backend.Backend;
import frontend.Frontend;
import frontend.IOtool;

import java.io.IOException;

public class Compiler {

    public static void main(String[] argv) throws IOException {
        /**
         * frontlevel:
         *       1-->lexer
         *       2-->lexer parser
         *       3-->lexer parser error-process
         *       4-->all frontend basic
         *       5-->all frontend basic and frontend optimizer
         */
        int frontlevel = 4;
        Frontend frontend = new Frontend(frontlevel);
        /**
         * backlevel:
         *       0-->backend close
         *       1-->backend open
         *       2-->optimizer version backend
         */
        int backlevel = 1;
        Backend backend = new Backend(frontend.getIrCodes(), backlevel, frontend.getCompileUnit());
    }
}
