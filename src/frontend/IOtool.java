package frontend;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class IOtool {
    public IOtool() {
    }

    public String getInput() throws IOException {
        InputStream in = new FileInputStream("testfile.txt");
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append((char) reader.read());
        }
        reader.close();
        in.close();
        return sb.toString();
    }

    public void outputAns(String outAns) throws IOException {
        OutputStream out = new FileOutputStream("output.txt");
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        outAns = outAns.trim();
        writer.append(outAns);
        writer.close();
    }

    public void output(String name, String outAns) {
        String filename = name + ".txt";
        OutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            outAns = outAns.trim();
            writer.append(outAns);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outputError(String outError) throws IOException {
        OutputStream out = new FileOutputStream("error.txt");
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        outError = outError.trim();
        writer.append(outError);
        writer.close();
    }
}
