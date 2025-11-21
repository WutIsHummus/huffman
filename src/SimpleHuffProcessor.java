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

    // viewer for reporting status/errors
    private IHuffViewer myViewer;

    // used for decompression
    private Decompressor decompressor;

    // ===== state saved from preprocess =====
    private int[] freqs;               // frequency table
    private Compressor compressor;     // compressor object
    private int headerFormat;          // SCF or STF
    private int estimatedCompressed;   // estimated compressed size
    private int originalBits;          // uncompressed size in bits


    /**
     * Preprocesses the input:
     *  - builds frequency table
     *  - builds the Huffman tree + codes
     *  - computes estimated compressed size
     *  - returns bits saved (original − estimated)
     */
    @Override
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {

        if (in == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: preprocessCompress(). InputStream cannot be null.");
        }

        this.headerFormat = headerFormat;

        // ============= BUILD FREQUENCY TABLE =============
        freqs = new int[ALPH_SIZE + 1];
        BitInputStream bis = new BitInputStream(in);
        int value = bis.readBits(BITS_PER_WORD);

        originalBits = 0;
        while (value != -1) {
            freqs[value]++;
            originalBits += BITS_PER_WORD;
            value = bis.readBits(BITS_PER_WORD);
        }

        // include EOF always
        freqs[PSEUDO_EOF] = 1;

        // ============= SET UP COMPRESSOR =============
        compressor = new Compressor(freqs);
        compressor.buildTree();
        compressor.buildCodes();

        // ============= ESTIMATE COMPRESSED SIZE =============
        estimatedCompressed = estimateCompressedSize(freqs, compressor, headerFormat);

        return originalBits - estimatedCompressed;
    }


    /**
     * Performs the actual compression using the results from preprocessCompress.
     * Requires a FRESH input stream.
     */
    @Override
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {

        if (in == null || out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: compress(). Streams cannot be null.");
        }

        if (compressor == null || freqs == null) {
            throw new IllegalStateException(
                    "Violation of precondition: preprocessCompress must be called first.");
        }

        // If not forcing AND compression does not save space:
        // (i.e., estimatedCompressed >= originalBits)
        if (!force && estimatedCompressed >= originalBits) {
            if (myViewer != null) {
                myViewer.showError("Compressed file is not smaller. Use force to write anyway.");
            }
            return -1; // per assignment convention
        }

        // Now write actual compressed data
        return compressor.writeCompressed(in, out, headerFormat);
    }


    /**
     * Uncompresses the previous Huffman-compressed input.
     */
    @Override
    public int uncompress(InputStream in, OutputStream out) throws IOException {

        if (in == null || out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: uncompress(). Streams cannot be null.");
        }

        BitInputStream bis = new BitInputStream(in);

        if (!decompressor.readHeader(bis)) {
            return -1;
        }

        return decompressor.decode(bis, out);
    }


    /**
     * Sets the viewer for status/error messages.
     */
    @Override
    public void setViewer(IHuffViewer viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: setViewer(). Viewer cannot be null.");
        }

        myViewer = viewer;
        decompressor = new Decompressor(myViewer);
    }


    // ================================================================
    //                       HELPER METHODS
    // ================================================================

    /**
     * Estimates number of bits in a compressed file using ONLY the frequency table.
     */
    private int estimateCompressedSize(int[] freqs, Compressor c, int format) {

        int bits = 0;

        // MAGIC NUMBER + HEADER FORMAT
        bits += BITS_PER_INT;
        bits += BITS_PER_INT;

        // HEADER COST
        if (format == STORE_COUNTS) {
            bits += ALPH_SIZE * BITS_PER_INT;
        }
        else { // STORE_TREE
            // compute size in exactly the same way as Compressor.writeSTFHeader()
            HuffmanTree tempTree = new HuffmanTree(freqs);
            int size = computeTreeSize(tempTree.getRoot());
            bits += BITS_PER_INT; // size integer
            bits += size;         // preorder bits
        }

        // DATA COST
        String[] codes = compressor.getCodesForEstimate();

// add bits for actual data
        for (int i = 0; i < ALPH_SIZE; i++) {
            if (freqs[i] > 0 && codes[i] != null) {
                bits += freqs[i] * codes[i].length();
            }
        }
        bits += codes[PSEUDO_EOF].length();

        return bits;
    }


    /**
     * Computes tree encoding size (same rules as Compressor.computeTreeSize).
     */
    private int computeTreeSize(TreeNode node) {
        if (node == null) return 0;
        if (node.isLeaf()) {
            return 1 + (BITS_PER_WORD + 1);
        }
        return 1 + computeTreeSize(node.getLeft()) + computeTreeSize(node.getRight());
    }
}
