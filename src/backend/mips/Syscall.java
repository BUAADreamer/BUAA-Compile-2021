package backend.mips;

public class Syscall extends MipsCode {
    int type; //0 input 1 print int 2 print string
    Assign assign;

    public Syscall(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        if (type == 0) {
            assign = new Assign(new Namespace(2, 0), 5);
            res.append(assign);
        } else if (type == 1) {
            assign = new Assign(new Namespace(2, 0), 1);
            res.append(assign);
        } else if (type == 2) {
            assign = new Assign(new Namespace(2, 0), 4);
            res.append(assign);
        }
        res.append("syscall\n");
        return res.toString();
    }
}
