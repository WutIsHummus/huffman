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

/**
 * Helper to handle compression details and bit writing
 */
public class Compressor {

    // frequency table generated during preprocessing
    private final int[] freq;

    // bitstring encodings for all values
    private final String[] codes;

    // the Huffman tree built during preprocessing
    private final HuffmanTree tree;

    // the header format chosen by the user (SCF or STF)
    private final int headerFormat;

    /**
     * Store derived data for use in compression
     * @param freq counts for every symbol plus PSEUDO_EOF
     * @param codes Huffman codes indexed by value
     * @param tree Huffman tree built from the counts
     * @param headerFormat header selection constant
     */
    public Compressor(int[] freq, String[] codes, HuffmanTree tree, int headerFormat) {
        this.freq = freq;
        this.codes = codes;
        this.tree = tree;
        this.headerFormat = headerFormat;
    }

    /**
     * Calculate total bits needed for the compressed file
     * @param in unused, kept for interface compatibility
     * @return total bits to be written
     * @throws IOException if reading or writing fails
     */
    public int computeCompressedBits(InputStream in) throws IOException {
        return computeHeaderBits() + computeDataBits();
    }

    /**
     * Write the header and compressed data to output
     * @param in source of original bytes
     * @param out target for compressed bits
     * @return number of bits written
     * @throws IOException if reading or writing fails
     */
    public int writeCompressedFile(InputStream in, OutputStream out) throws IOException {
        BitOutputStream bitOut = new BitOutputStream(out);
        BitInputStream bitIn = new BitInputStream(in);
        int bitsWritten = 0;

        try {
            // lead with the header so a decoder knows how to rebuild the tree
            bitsWritten += writeHeader(bitOut);
            int byteRead;
            // encode each original byte using the precomputed table
            while ((byteRead = bitIn.readBits(IHuffConstants.BITS_PER_WORD)) != -1) {
                bitsWritten += writeCode(bitOut, codes[byteRead]);
            }
            // append the pseudo EOF so the decoder knows when to stop
            bitsWritten += writeCode(bitOut, codes[IHuffConstants.PSEUDO_EOF]);
        } finally {
            // close in reverse order so downstream streams flush cleanly
            bitIn.close();
            bitOut.close();
        }
        return bitsWritten;
    }

    /**
     * Calculate bits needed for the header
     * @return total header bits
     */
    private int computeHeaderBits() {
        // magic + header-kind ints always lead, regardless of format choice
        int bits = IHuffConstants.BITS_PER_INT * 2;
        bits += headerPayloadBitCount();
        return bits;
    }

    /**
     * Calculate bits needed to store the tree
     * @param node current subtree root
     * @return bits for this subtree
     */
    private int computeTreeBits(TreeNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1 + (IHuffConstants.BITS_PER_WORD + 1);
        }
        return 1 + computeTreeBits(node.getLeft()) + computeTreeBits(node.getRight());
    }

    /**
     * Calculate bits needed for the compressed data body
     * @return bits for the encoded payload
     */
    private int computeDataBits() {
        int bits = 0;
        // each symbol contributes freq * code length bits to the output
        for (int value = 0; value < IHuffConstants.ALPH_SIZE; value++) {
            if (freq[value] > 0 && codes[value] != null) {
                bits += freq[value] * codes[value].length();
            }
        }
        // pseudo EOF always shows up once regardless of the input
        bits += codes[IHuffConstants.PSEUDO_EOF].length();
        return bits;
    }

    /**
     * Write the magic number and header info
     * @param bitOut destination stream
     * @return header bits written
     * @throws IOException if write fails
     */
    private int writeHeader(BitOutputStream bitOut) throws IOException {
        int bits = 0;
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, IHuffConstants.MAGIC_NUMBER);
        bitOut.writeBits(IHuffConstants.BITS_PER_INT, headerFormat);
        bits += IHuffConstants.BITS_PER_INT * 2;

        // payload differs between count and tree headers, so hand off to helper
        bits += writeHeaderPayload(bitOut);
        return bits;
    }

    /**
     * Write a single Huffman code to output
     * @param bitOut destination stream
     * @param code string of '0'/'1'
     * @return bits written
     */
    private int writeCode(BitOutputStream bitOut, String code) {
        if (code == null) {
            throw new IllegalStateException("Missing Huffman code during compression");
        }
        for (int i = 0; i < code.length(); i++) {
            bitOut.writeBits(1, code.charAt(i) - '0');
        }
        return code.length();
    }

    /**
     * Get size of the header payload (counts or tree)
     * @return bits for the header payload
     */
    private int headerPayloadBitCount() {
        if (headerFormat == IHuffConstants.STORE_TREE) {
            // tree headers carry an int with the tree size followed by the preorder bits
            return IHuffConstants.BITS_PER_INT + computeTreeBits(tree.getRoot());
        }
        if (headerFormat == IHuffConstants.STORE_COUNTS) {
            // count headers list every byte frequency explicitly (no entry for pseudo EOF)
            return IHuffConstants.ALPH_SIZE * IHuffConstants.BITS_PER_INT; // 32 bits per int
        }
        throw new IllegalStateException("Invalid header format: " + headerFormat);
    }

    /**
     * Write the header payload (counts or tree)
     * @param bitOut destination stream
     * @return bits written
     * @throws IOException if write fails
     */
    private int writeHeaderPayload(BitOutputStream bitOut) throws IOException {
        if (headerFormat == IHuffConstants.STORE_TREE) {
            int treeBits = computeTreeBits(tree.getRoot());
            bitOut.writeBits(IHuffConstants.BITS_PER_INT, treeBits);
            tree.writeTree(bitOut);
            return IHuffConstants.BITS_PER_INT + treeBits;
        }
        if (headerFormat == IHuffConstants.STORE_COUNTS) {
            for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
                bitOut.writeBits(IHuffConstants.BITS_PER_INT, freq[i]);
            }
            return IHuffConstants.ALPH_SIZE * IHuffConstants.BITS_PER_INT;
        }
        throw new IOException("Invalid header format: " + headerFormat);
    }
}