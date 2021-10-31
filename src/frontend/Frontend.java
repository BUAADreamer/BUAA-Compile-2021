package frontend;

import java.io.IOException;

public class Frontend {
    public Frontend() throws IOException {
        IOtool iOtool = new IOtool();
        Lexer lexer = new Lexer(iOtool.getInput());
        iOtool.outputAns(lexer.getLexerAns());
        Parser parser = new Parser(lexer.getWords());
        iOtool.outputAns(parser.getParserAns());
        Visitor visitor = new Visitor(parser.getAst(), parser.getExcNodes());
        iOtool.outputError(visitor.getExcOutAns());
        if (!visitor.getExcOutAns().equals("")) {
            System.out.println("Source code has wrong.");
            return;
        }
        Translater translater = new Translater(parser.getAst());
        iOtool.output("ircode", translater.getIROutput());
    }
}
