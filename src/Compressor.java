/*  Student information for assignment:
 *
 *  On OUR honor, Alperen Aydin and Harshal Dhaduk, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 1
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
import java.util.Arrays;

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
     * Constructs a Compressor by storing a defensive copy of the frequency table.
     * pre: freqs != null && freqs.length == ALPH_SIZE + 1
     * post: internal frequency table initialized
     * @param freqs the frequency table including the pseudo-EOF slot
     */
    public Compressor(int[] freqs) {
        if (freqs == null || freqs.length != ALPH_SIZE + 1) {
            throw new IllegalArgumentException("Violation of precondition: Compressor(). Invalid " +
                    "frequency array.");
        }

        this.freqs = new int[ALPH_SIZE + 1];
        System.arraycopy(freqs, 0, this.freqs, 0, freqs.length);
        // ensure PSEUDO_EOF always has frequency 1 to indicate end of array
        this.freqs[PSEUDO_EOF] = 1;
    }

    /**
     * Builds the Huffman tree using the stored frequency table.
     * pre: none
     * post: tree is initialized
     */
    public void buildTree() {
        tree = new HuffmanTree(freqs);
    }

    /**
     * Generates Huffman codes by traversing the constructed Huffman tree.
     * pre: tree != null
     * post: codes[] initialized with a bitstring for each symbol
     */
    public void buildCodes() {
        if (tree == null) {
            throw new IllegalStateException("Violation of precondition: buildCodes(). Tree must " +
                    "be built before codes.");
        }

        codes = tree.makeCodes();
    }

    /**
     * Returns the internal Huffman code map for preprocessing estimation.
     * pre: codes != null
     * post: returns reference to internal code array
     * @return the array of Huffman codes
     */
    public String[] getCodesForEstimate() {
        if (codes == null) {
            throw new IllegalStateException("Violation of precondition: getCodesForEstimate(). " +
                    "Codes not built yet.");
        }

        return Arrays.copyOf(codes, codes.length);
    }

    /**
     * Returns the Huffman tree built during preprocessing.
     * pre: tree != null
     * post: returns the internal HuffmanTree used for encoding
     * @return the constructed Huffman tree
     */
    public HuffmanTree getTree() {
        return tree;
    }

    /**
     * Writes the full Huffman-compressed output file: magic number, header, encoded data, and
     * pseudo-EOF.
     * pre: rawIn != null && rawOut != null && codes != null
     * post: compressed output written to rawOut
     * @param rawIn the raw uncompressed input stream
     * @param rawOut the output stream to receive compressed bits
     * @param headerFormat STORE_COUNTS or STORE_TREE
     * @return number of bits written
     * @throws IOException if an I/O error occurs while writing
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
        // reset internal state so future operations must call preprocess again
        tree = null;
        codes = null;
        return bits;
    }

    /**
     * Writes the magic number and the header-format constant.
     * pre: out != null
     * post: two 32-bit ints written
     * @param out the BitOutputStream to write into
     * @param headerFormat STORE_COUNTS or STORE_TREE
     * @return number of bits written (always 64)
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
     * Writes either the SCF or STF header.
     * pre: out != null
     * post: header bits written
     * @param out BitOutputStream receiving header bits
     * @param format STORE_COUNTS or STORE_TREE
     * @return number of bits written
     */
    private int writeHeader(BitOutputStream out, int format) {
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
     * Writes Standard Count Format: 256 integer frequencies.
     * pre: out != null
     * post: SCF header written
     * @param out BitOutputStream for writing counts
     * @return total bits written (256 * 32)
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
     * Writes Standard Tree Format: 32-bit tree size + preorder tree encoding.
     * pre: out != null && tree != null
     * post: STF header written
     * @param out BitOutputStream for writing the STF header
     * @return number of bits written
     */
    private int writeSTF(BitOutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeSTF(). Output stream cannot be null.");
        }
        if (tree == null || tree.getRoot() == null) {
            throw new IllegalStateException(
                    "Violation of precondition: writeSTF(). Huffman tree must be built first.");
        }

        int size = tree.computeTreeSize();
        // write the total number of bits in the preorder tree encoding
        out.writeBits(BITS_PER_INT, size);
        return BITS_PER_INT + writeTreeBits(out, tree.getRoot());
    }

    /**
     * Writes a preorder representation of the Huffman tree: 0 for internal nodes, 1 + 9 bits for
     * leaf nodes.
     * pre: out != null && node != null
     * post: preorder structure written to out
     * @param out BitOutputStream receiving preorder bits
     * @param node current node in the tree
     * @return number of bits written
     */
    private int writeTreeBits(BitOutputStream out, TreeNode node) {
        if (out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeTreeBits(). Output stream cannot be null.");
        }
        if (node == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeTreeBits(). Node cannot be null.");
        }

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
     * Writes the encoded Huffman bitstrings for each byte in the input stream.
     * pre: rawIn != null && out != null && codes != null
     * post: encoded data bits written
     * @param rawIn the uncompressed input stream
     * @param out BitOutputStream receiving encoded data
     * @return number of data bits written
     * @throws IOException if input or output fails
     */
    private int writeData(InputStream rawIn, BitOutputStream out) throws IOException {
        if (rawIn == null || out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeData(). Input and output streams" +
                            " cannot be null.");
        }
        if (codes == null) {
            throw new IllegalStateException(
                    "Violation of precondition: writeData(). Codes must be built first.");
        }

        BitInputStream in = new BitInputStream(rawIn);
        int bits = 0;
        int value = in.readBits(BITS_PER_WORD);
        while (value != -1) {
            // retrieve the huffman bitstring for the byte value
            bits += writeCode(out, codes[value]);
            value = in.readBits(BITS_PER_WORD);
        }
        return bits;
    }

    /**
     * Writes the Huffman code for the pseudo-EOF marker.
     * pre: out != null && codes != null
     * post: EOF code written
     * @param out BitOutputStream receiving EOF bits
     * @return number of bits written
     */
    private int writeEOF(BitOutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeEOF(). Output stream cannot be null.");
        }
        if (codes == null) {
            throw new IllegalStateException(
                    "Violation of precondition: writeEOF(). Codes must be built first.");
        }

        return writeCode(out, codes[PSEUDO_EOF]);
    }

    /**
     * Write a Huffman code bit-by-bit to the BitOutputStream.
     * pre: out != null && code != null
     * post: all bits of code written, returns number of bits written
     * @param out the BitOutputStream to write to
     * @param code the string of '0'/'1' characters representing the Huffman code
     * @return number of bits written
     */
    private int writeCode(BitOutputStream out, String code) {
        if (out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeCode(). Output stream cannot be null.");
        }
        if (code == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: writeCode(). Code string cannot be null.");
        }

        int bits = 0;
        for (int i = 0; i < code.length(); i++) {
            out.writeBits(1, code.charAt(i) == '1' ? 1 : 0);
            bits++;
        }
        return bits;
    }
}