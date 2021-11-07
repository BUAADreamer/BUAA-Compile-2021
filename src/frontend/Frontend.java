package frontend;

import frontend.irgen.IRGenerater;
import frontend.preprocess.Lexer;
import frontend.preprocess.Parser;
import frontend.preprocess.Visitor;
import frontend.stdir.IRCode;

import java.io.IOException;
import java.util.ArrayList;

public class Frontend {
    private ArrayList<IRCode> irCodes;

    public Frontend() throws IOException {
        /**
         * lexer analysis
         */
        IOtool iOtool = new IOtool();
        Lexer lexer = new Lexer(iOtool.getInput());
        iOtool.outputAns(lexer.getLexerAns());
        /**
         * Parser analysis
         */
        Parser parser = new Parser(lexer.getWords());
        iOtool.outputAns(parser.getParserAns());
        /**
         * Error process
         */
        Visitor visitor = new Visitor(parser.getAst(), parser.getExcNodes());
        iOtool.outputError(visitor.getExcOutAns());
        if (!visitor.getExcOutAns().equals("")) {
            System.out.println("Source code has wrong.");
            return;
        }
        /**
         *Intermediate code generate
         */
        //Translater translater = new Translater(parser.getAst());
        //iOtool.output("ircode", translater.getIROutput());
        IRGenerater irGenerater = new IRGenerater(parser.getAst());
        this.irCodes = irGenerater.getIrcodes();
        iOtool.output("ircode.txt", irGenerater.getIroutput());
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }
}
