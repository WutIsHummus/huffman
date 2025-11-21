/*  Student information for assignment:
 *
 *  On OUR honor, Alperen Aydin and Harshal Dhaduk, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1:
 *  UTEID: hd8446
 *  email: hd8446@eid.utexas.edu
 *  TA: Jaxon Dial
 *
 *  Student 2:
 *  UTEID: aa95287
 *  email: aa95287@eid.utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles building the Huffman tree, generating codes, writing headers and encoding
 * the raw input into a Huffman-compressed file.
 * pre: none
 * post: can write properly formatted .hf bitstreams
 */
public class Compressor implements IHuffConstants {
    // frequency table for all symbols incl. pseudo-EOF
    private final int[] freqs;
    // Huffman tree and its code map
    private HuffmanTree tree;
    // Huffman codes for each value generated during preprocessing
    private String[] codes;

    /**
     * Create a compressor storing a defensive copy of the frequency table.
     * pre: f != null && f.length == ALPH_SIZE + 1
     * post: internal frequency array initialized
     */
    public Compressor(int[] freqs) {
        if (freqs == null || freqs.length != ALPH_SIZE + 1) {
            throw new IllegalArgumentException("Violation of precondition: Compressor(). Invalid " +
                    "frequency array.");
        }

        this.freqs = new int[ALPH_SIZE + 1];
        for (int i = 0; i < freqs.length; i++) {
            this.freqs[i] = freqs[i];
        }
        // ensure PSEUDO_EOF always has frequency 1 to indicate end of array
        this.freqs[PSEUDO_EOF] = 1;
    }

    /**
     * Build the Huffman tree.
     * pre: none
     * post: tree initialized
     */
    public void buildTree() {
        tree = new HuffmanTree(freqs);
    }

    /**
     * Build the Huffman codes from the tree.
     * pre: tree != null
     * post: codes array initialized
     */
    public void buildCodes() {
        if (tree == null) {
            throw new IllegalStateException("Violation of precondition: buildCodes(). Tree must " +
                    "be built before codes.");
        }

        codes = tree.makeCodes();
    }

    /**
     * Accessor for codes array during preprocessing estimation.
     * pre: codes != null
     * post: returns internal code map
     */
    public String[] getCodesForEstimate() {
        if (codes == null) {
            throw new IllegalStateException("Violation of precondition: getCodesForEstimate(). " +
                    "Codes not built yet.");
        }

        return codes;
    }

    /**
     * Write the full compressed file: magic, header, encoded data.
     * pre: rawIn != null && rawOut != null && codes != null
     * post: compressed file written
     */
    public int writeCompressed(InputStream rawIn, OutputStream rawOut, int headerFormat)
            throws IOException {
        if (rawIn == null || rawOut == null) {
            throw new IllegalArgumentException("Violation of precondition: writeCompressed" +
                    "(). Input/output streams cannot be null.");
        }
        if (codes == null) {
            throw new IllegalStateException("Violation of precondition: writeCompressed(). Codes " +
                    "must be generated before compressing.");
        }

        BitOutputStream out = new BitOutputStream(rawOut);
        int bits = 0;
        bits += writeMagicAndFormat(out, headerFormat);
        bits += writeHeader(out, headerFormat);
        bits += writeData(rawIn, out);
        bits += writeEOF(out);
        out.close();
        return bits;
    }

    /**
     * Writes the magic number and the header format.
     * pre: out != null
     * post: two 32-bit values written
     */
    private int writeMagicAndFormat(BitOutputStream out, int headerFormat) {
        if (out == null) {
            throw new IllegalArgumentException("Violation of precondition: writeMagicAndFormat" +
                    "(). Output stream cannot be null.");
        }

        out.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        out.writeBits(BITS_PER_INT, headerFormat);
        return BITS_PER_INT * 2;
    }

    /**
     * Writes either SCF or STF header.
     * pre: out != null
     * post: header bits written
     */
    private int writeHeader(BitOutputStream out, int format) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Violation of precondition: writeHeader(). Output " +
                    "stream cannot be null.");
        }

        if (format == STORE_COUNTS) {
            return writeSCF(out);
        }
        else {
            return writeSTF(out);
        }
    }

    /**
     * Standard Count Format header: 256 32-bit frequencies.
     * pre: out != null
     * post: SCF written
     */
    private int writeSCF(BitOutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeSCF(). Output stream cannot be null.");
        }

        int bits = 0;
        for (int i = 0; i < ALPH_SIZE; i++) {
            // store byte i's frequency so the decoder can rebuild the Huffman tree
            out.writeBits(BITS_PER_INT, freqs[i]);
            bits += BITS_PER_INT;
        }
        return bits;
    }

    /**
     * Standard Tree Format header: 32-bit size + preorder bits.
     * pre: out != null && tree != null
     * post: STF written
     */
    private int writeSTF(BitOutputStream out) throws IOException {
        int size = computeTreeSize(tree.getRoot());
        // write the total number of bits in the preorder tree encoding
        out.writeBits(BITS_PER_INT, size);
        return BITS_PER_INT + writeTreeBits(out, tree.getRoot());
    }

    /**
     * Compute preorder tree size in bits.
     * pre: none
     * post: returns size in bits
     */
    private int computeTreeSize(TreeNode node) {
        if (node == null) {
            // null nodes are not written into the header
            return 0;
        }
        if (node.isLeaf()) {
            // leaf node, contributes 1 bit for the leaf flag + 9 bits for the stored value
            return 1 + (BITS_PER_WORD + 1);
        }
        // internal node contributes 1 bit plus its children’s sizes
        return 1 + computeTreeSize(node.getLeft()) + computeTreeSize(node.getRight());
    }

    /**
     * Write preorder structure: 0 for internal, 1 + 9 bits for leaf.
     * pre: out != null
     * post: preorder bits written
     */
    private int writeTreeBits(BitOutputStream out, TreeNode node) throws IOException {
        // base case, leaf node, reached the end of the current path
        if (node.isLeaf()) {
            // write a 1 to mark it as a leaf
            out.writeBits(1, 1);
            // write the leaf's value using 9 bits (ASCII value and 1 extra bit)
            out.writeBits(IHuffConstants.BITS_PER_WORD + 1, node.getValue());
            return 1 + (IHuffConstants.BITS_PER_WORD + 1);
        }
        // else it is an internal node, mark it with a 0
        out.writeBits(1, 0);
        int bits = 1;
        // recursive step, iterate back through the tree to continue accumulating bits
        bits += writeTreeBits(out, node.getLeft());
        bits += writeTreeBits(out, node.getRight());
        return bits;
    }

    /**
     * Write encoded data chunks.
     * pre: rawIn != null && out != null && codes != null
     * post: encodings written to out
     */
    private int writeData(InputStream rawIn, BitOutputStream out) throws IOException {
        BitInputStream in = new BitInputStream(rawIn);
        int bits = 0;
        int value = in.readBits(BITS_PER_WORD);
        while (value != -1) {
            // retrieve the huffman bitstring for the byte value
            String code = codes[value];
            for (int i = 0; i < code.length(); i++) {
                // convert the character into the corresponding numeric bit
                out.writeBits(1, code.charAt(i) == '1' ? 1 : 0);
                bits++;
            }
            value = in.readBits(BITS_PER_WORD);
        }
        return bits;
    }

    /**
     * Writes the pseudo-EOF code at end of data.
     * pre: out != null
     * post: EOF bits written
     */
    private int writeEOF(BitOutputStream out) {
        String eof = codes[PSEUDO_EOF];
        for (int i = 0; i < eof.length(); i++) {
            // convert the character into the corresponding numeric bit
            out.writeBits(1, eof.charAt(i) == '1' ? 1 : 0);
        }
        return eof.length();
    }
}