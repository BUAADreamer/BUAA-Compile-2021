package frontend;

import frontend.irgen.IRGenerater;
import frontend.irgen.Optimizer;
import frontend.irgen.optimize.CompileUnit;
import frontend.preprocess.Lexer;
import frontend.preprocess.Parser;
import frontend.preprocess.Visitor;
import frontend.stdir.IRCode;

import java.io.IOException;
import java.util.ArrayList;

public class Frontend {
    private ArrayList<IRCode> irCodes;
    private CompileUnit compileUnit;

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
        outputIrcodes("ircode0.txt");
//        iOtool.output("ircode.txt", irGenerater.getIroutput());
        if (level <= 4) return;
        Optimizer optimizer = new Optimizer(this.irCodes, irGenerater.getRegNum());
        irCodes = optimizer.getIrCodes();
        outputIrcodes("ircode.txt");
        compileUnit = optimizer.getCompileUnit();
    }

    public ArrayList<IRCode> getIrCodes() {
        return irCodes;
    }

    void outputIrcodes(String name) {
        StringBuilder sb = new StringBuilder("");
        for (IRCode irCode : irCodes) {
            sb.append(irCode.toString());
        }
        IOtool.output(name, sb.toString());
    }

    public CompileUnit getCompileUnit() {
        return compileUnit;
    }
}
