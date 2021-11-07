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

    public Frontend(int level) throws IOException {
        /**
         * lexer analysis
         */
        IOtool iOtool = new IOtool();
        Lexer lexer = new Lexer(iOtool.getInput());
        iOtool.outputAns(lexer.getLexerAns());
        if (level <= 1) return;
        /**
         * Parser analysis
         */
        Parser parser = new Parser(lexer.getWords());
        iOtool.outputAns(parser.getParserAns());
        if (level <= 2) return;
        /**
         * Error process
         */
        Visitor visitor = new Visitor(parser.getAst(), parser.getExcNodes());
        iOtool.outputError(visitor.getExcOutAns());
        if (level <= 3) return;
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
        if (level <= 4) return;
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }
}
