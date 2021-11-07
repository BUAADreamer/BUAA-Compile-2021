package backend;

import backend.mips.IRTranslator;
import frontend.IOtool;
import frontend.irgen.IRGenerater;
import frontend.stdir.IRCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Backend {

    private ArrayList<IRCode> irCodes;

    public Backend(ArrayList<IRCode> irCodes) throws IOException {
        this.irCodes = irCodes;
        IOtool iotool = new IOtool();
        IRTranslator irTranslator = new IRTranslator(irCodes);
        iotool.output("mips.txt", irTranslator.getMipsout());
    }

    private ArrayList<String> splitByLine(String str) {
        return new ArrayList<String>(Arrays.asList(str.split("\\n")));
    }
}
