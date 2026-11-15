package dk.stream.android;
import java.io.*;

/** Keeps AtomicFile's descriptor open while preserving efficient block writes. */
final class NonClosingOutputStream extends FilterOutputStream {
 NonClosingOutputStream(OutputStream output){super(output);}
 @Override public void write(byte[] bytes,int offset,int length)throws IOException{out.write(bytes,offset,length);}
 @Override public void write(byte[] bytes)throws IOException{out.write(bytes);}
 @Override public void close()throws IOException{flush();}
}
