package frontend.irgen.optimize;

import frontend.stdir.IRCode;

import java.util.ArrayList;

public class HoleOptimizer {
    ArrayList<IRCode> irCodes;

    public HoleOptimizer(ArrayList<IRCode> irCodes) {
        this.irCodes = irCodes;
    }
}
