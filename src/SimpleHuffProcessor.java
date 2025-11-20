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

    private IHuffViewer myViewer;
    private int[] freqTable;
    private HuffmanTree huffTree;
    private String[] codes;
    private Compressor compressor;
    private int originalBitCount;
    private int projectedCompressedBits;
    private int storedHeaderFormat;
    private boolean readyForCompress;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * 
     * @param in           is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind
     *                     of
     *                     header to use, standard count format, standard tree
     *                     format, or
     *                     possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     *         Note, to determine the number of
     *         bits saved, the number of bits written includes
     *         ALL bits that will be written including the
     *         magic number, the header format number, the header to
     *         reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    @Override
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        if (in == null) {
            throw new IOException("Input stream cannot be null");
        }

        resetState();
        storedHeaderFormat = headerFormat;
        freqTable = new int[IHuffConstants.ALPH_SIZE + 1];

        try (BitInputStream bitIn = new BitInputStream(in)) {
            int value;
            // count every byte while tracking the raw bit total for later comparisons
            while ((value = bitIn.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
                freqTable[value]++;
                originalBitCount += IHuffConstants.BITS_PER_WORD;
            }
        }

        // force the sentinel into the table so the tree always has an EOF leaf
        freqTable[IHuffConstants.PSEUDO_EOF] = 1;
        huffTree = new HuffmanTree(freqTable);
        codes = huffTree.makeCodes();
        // cache a compressor so the actual compress call never has to rebuild
        // structures
        compressor = new Compressor(freqTable, codes, huffTree, storedHeaderFormat);
        projectedCompressedBits = compressor.computeCompressedBits(null);
        readyForCompress = true;

        int bitsSaved = originalBitCount - projectedCompressedBits;
        logFrequencies();
        return bitsSaved;
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br>
     * pre: <code>preprocessCompress</code> must be called before this method
     * 
     * @param in    is the stream being compressed (NOT a BitInputStream)
     * @param out   is bound to a file/stream to which bits are written
     *              for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than
     *              the input file.
     *              If this is false do not create the output file if it is larger
     *              than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    @Override
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        if (!readyForCompress || compressor == null) {
            throw new IOException("Must call preprocessCompress before compress");
        }
        if (in == null || out == null) {
            throw new IOException("Input and output streams must be non-null");
        }

        if (!force && projectedCompressedBits >= originalBitCount) {
            showString("Compression would not reduce size; rerun with force=true to proceed.");
            throw new IOException("Compression rejected because it would not save space");
        }

        int bitsWritten = compressor.writeCompressedFile(in, out);
        showString("Compression complete. Bits written: " + bitsWritten);
        return bitsWritten;
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * 
     * @param in  is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    @Override
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        throw new IOException("uncompress not implemented");
        // return 0;
    }

    @Override
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }

    /**
     * Print a short digest of the recorded frequencies and codes.
     */
    private void logFrequencies() {
        if (myViewer == null || freqTable == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Frequencies (value:count code)\n");
        for (int value = 0; value < IHuffConstants.ALPH_SIZE; value++) {
            if (freqTable[value] > 0) {
                sb.append(formatFreqLine(value, freqTable[value], codes[value]));
            }
        }
        sb.append(formatFreqLine(IHuffConstants.PSEUDO_EOF, freqTable[IHuffConstants.PSEUDO_EOF],
                codes[IHuffConstants.PSEUDO_EOF]));
        myViewer.update(sb.toString());
    }

    /**
     * Format one line in the frequency report.
     */
    private String formatFreqLine(int value, int count, String code) {
        String label = value == IHuffConstants.PSEUDO_EOF
                ? "PEOF"
                : printable(value);
        String codeText = (code == null || code.isEmpty()) ? "-" : code;
        return String.format("%4d (%s): %d [%s]%n", value, label, count, codeText);
    }

    /**
     * Return string character of a int of its ASCII or returns "Non ASCCII "+ value
     */
    private String printable(int value) {
        // Standard ASCII printable range is 32 to 126
        if (value >= 32 && value < 127) {
            return Character.toString((char) value);
        }
        return "Non ASCII " + value;
    }

    private void resetState() {
        freqTable = null;
        huffTree = null;
        codes = null;
        compressor = null;
        originalBitCount = 0;
        projectedCompressedBits = 0;
        storedHeaderFormat = 0;
        readyForCompress = false;
    }
}
