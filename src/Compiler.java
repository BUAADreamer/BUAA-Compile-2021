import frontend.IOtool;
import frontend.Lexer;
import frontend.Parser;

import java.io.IOException;

public class Compiler {
    public static void main(String[] argv) throws IOException {
        IOtool iOtool = new IOtool();
        Lexer lexer = new Lexer(iOtool.getInput());
        iOtool.outputAns(lexer.getLexerAns());
        Parser parser = new Parser(lexer.getWords());
        iOtool.outputAns(parser.getParserAns());
    }
}
