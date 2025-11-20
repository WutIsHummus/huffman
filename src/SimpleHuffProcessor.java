/*  Student information for assignment:
 *
 *  On OUR honor, Alperen Aydin and Harshal Dhaduk, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID: hd8446
 *  email address: hd8446@eid.utexas.edu
 *  TA name: Jaxon Dial
 *
 *  Student 2
 *  UTEID: aa95287
 *  email address: aa95287@eid.utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleHuffProcessor implements IHuffProcessor {
    // viewer for error reporting
    private IHuffViewer myViewer;
    // helper used for handling decompression operations
    private Decompressor decompressor;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * @param in is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     * header to use, standard count format, standard tree format, or
     * possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        showString("Not working yet");
        myViewer.update("Still not working");
        throw new IOException("preprocess not implemented");
        //return 0;
    }

    /**
	 * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @param out is bound to a file/stream to which bits are written
     * for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     * If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        throw new IOException("compress is not implemented");
        //return 0;
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return number of bits written to out, or -1 if the compressed stream is invalid
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: uncompress(). InputStream & OutputStream cannot " +
                            "be null.");
        }

        BitInputStream bis = new BitInputStream(in);
        // reset to the beginning of decoding phase only after we know the tree is valid
        if (!decompressor.readHeader(bis)) {
            return -1;
        }
        return decompressor.decode(bis, out);
    }

    /**
     * Sets the viewer used for status and error messages, and initializes
     * the decompressor helper that depends on the viewer.
     * pre: viewer != null
     * post: internal viewer and decompressor references set
     * @param viewer the UI viewer implementation
     */
    public void setViewer(IHuffViewer viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: setViewer(). Viewer cannot be null.");
        }

        myViewer = viewer;
        decompressor = new Decompressor(myViewer);   // add this line
    }

    /**
     * Sends a message through the viewer, if present.
     * @param phrase the string to show
     */
    private void showString(String phrase){
        if (myViewer != null) {
            myViewer.update(phrase);
        }
    }
}
