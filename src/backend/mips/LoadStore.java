package backend.mips;

import frontend.stdir.Label;

public class LoadStore extends MipsCode {
    private Namespace reg1;
    private Namespace reg2;
    private Namespace offset;
    private int type;
    private Namespace label;
    private int base = 0x7fff0000;

    /**
     * constructor
     *
     * @param reg1  first reg
     * @param reg2  second reg
     * @param param address offset or label
     * @param type  type==0 lw $1 0($4)
     *              type==1 lw $1 100
     *              type==2 lw $1 label($0)
     *              type==3 sw $1 0($4)
     *              type==4 sw $1 100
     *              type==5 sw $1 label($0)
     *              type==6 la $1 label
     */
    public LoadStore(Namespace reg1, Namespace reg2, Namespace param, int type) {
        this.reg1 = reg1;
        this.reg2 = reg2;
        if (param != null && (param.getType() == 0 || param.getType() == 1 || param.getType() == 3 || param.getType() == 4)) {
            this.offset = param;
        } else {
            this.label = param;
        }
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        if (type < 3) {
            res.append("lw " + reg1);
        } else if (type < 6) {
            res.append("sw " + reg1);
        } else if (type == 6) {
            res.append("la " + reg1);
        }
        if (offset != null && offset.getValue() < 0) {
            if (type == 0 || type == 3) {
                res.append(String.format(" %d($sp)", -offset.getValue()));
            } else if (type == 1 || type == 4) {
                res.append(" " + String.valueOf(offset));
            } else if (type == 2 || type == 5) {
                res.append(String.format(" %d($sp)", -offset.getValue()));
            } else if (type == 6) {
                res.append(String.format(" %s", label));
            }
            res.append("\n");
            return res.toString();
        } else if (offset != null && offset.getValue() >= base) {
            if (type == 0 || type == 3) {
                res.append(String.format(" %d($sp)", base - offset.getValue()));
            } else if (type == 1 || type == 4) {
                res.append(" " + String.valueOf(reg2));
            } else if (type == 2 || type == 5) {
                res.append(String.format(" %d($sp)", base - offset.getValue()));
            } else if (type == 6) {
                res.append(String.format(" %s", label));
            }
            res.append("\n");
            return res.toString();
        }
        if (type == 0 || type == 3) {
            res.append(String.format(" %s(%s)", offset, reg2));
        } else if (type == 1 || type == 4) {
            res.append(" " + String.valueOf(offset));
        } else if (type == 2 || type == 5) {
            res.append(String.format(" %s(%s)", label, reg2));
        } else if (type == 6) {
            res.append(String.format(" %s", label));
        }
        res.append("\n");
        return res.toString();
    }

    public Namespace getReg1() {
        return reg1;
    }

    public Namespace getReg2() {
        return reg2;
    }

    public Namespace getOffset() {
        return offset;
    }

    public int getType() {
        return type;
    }

    public Namespace getLabel() {
        return label;
    }

    public int getBase() {
        return base;
    }

    public boolean isload() {
        return type == 0 || type == 1 || type == 2 || type == 6;
    }
}
