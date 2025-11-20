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

import java.io.OutputStream;
import java.io.IOException;

/**
 * A helper class used by SimpleHuffProcessor to perform file decompression. Handles reading
 * header information, reconstructing the Huffman tree, and decoding the compressed bitstream.
 * pre: none
 * post: provides methods for decoding Huffman-compressed data
 */
public class Decompressor {
    // invariant: tree is null before header is read and non-null only after successful header read
    // the Huffman tree reconstructed from header data
    private HuffmanTree tree;
    // viewer for error reporting
    private final IHuffViewer viewer;

    /**
     * Construct a decompressor that can report errors through the given viewer.
     * pre: viewer != null
     * post: new decompressor created
     * @param viewer the IHuffViewer used to display messages and errors
     */
    public Decompressor(IHuffViewer viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: Decompressor(). Viewer cannot be null.");
        }

        this.viewer = viewer;
    }

    /**
     * Read header information from the compressed file and rebuild the Huffman tree used to
     * decode data.
     * pre: bis != null
     * post: tree is reconstructed from header data
     * @param bis the BitInputStream bound to the compressed file
     * @return true if header was valid and tree was built, false otherwise
     * @throws IOException if error occurs while reading header
     */
    public boolean readHeader(BitInputStream bis) throws IOException {
        if (bis == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: readHeader(). BitInputStream cannot be null.");
        }

        // check if file is a huff compressed file
        int input = bis.readBits(IHuffConstants.BITS_PER_INT);
        if (input != IHuffConstants.MAGIC_NUMBER) {
            viewer.showError("Error reading compressed file. File did not start with the " +
                    "huff magic number.");
            return false;
        }
        int format = bis.readBits(IHuffConstants.BITS_PER_INT);
        // check whether to use SCF or STF
        if (format == IHuffConstants.STORE_COUNTS) {
            return readSCF(bis);
        }
        else if (format == IHuffConstants.STORE_TREE) {
            return readSTF(bis);
        }
        else {
            viewer.showError("Error reading compressed file. Header format value invalid.");
            return false;
        }
    }

    /**
     * Helper to process SCF header format and rebuild the Huffman tree.
     * pre: bis != null
     * post: tree reconstructed from SCF frequency table
     * @param bis the BitInputStream providing header data
     * @return true if SCF header parsed successfully, false otherwise
     * @throws IOException if an error occurs while reading
     */
    private boolean readSCF(BitInputStream bis) throws IOException {
        if (bis == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: decode(). Input BitInputStream cannot be null.");
        }

        // SCF format, create a frequency array to use in creating a tree
        int[] freqs = new int[IHuffConstants.ALPH_SIZE + 1];
        for (int i = 0; i < IHuffConstants.ALPH_SIZE; i++) {
            // read next 32 bits each time as per pattern to determine frequency for each int
            int freq = bis.readBits(IHuffConstants.BITS_PER_INT);
            if (freq < 0) {
                // early end
                viewer.showError("Error reading compressed file. Unexpected end of input.");
                return false;
            }
            freqs[i] = freq;
        }
        // indicate the end of the compressed data, build tree
        freqs[IHuffConstants.PSEUDO_EOF] = 1;
        tree = new HuffmanTree(freqs);
        return true;
    }

    /**
     * Helper to process STF header format and rebuild the Huffman tree.
     * pre: bis != null
     * post: tree reconstructed from STF preorder representation
     * @param bis the BitInputStream providing header data
     * @return true if STF header parsed successfully, false otherwise
     * @throws IOException if an error occurs while reading
     */
    public boolean readSTF(BitInputStream bis) throws IOException {
        if (bis == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: decode(). Input BitInputStream cannot be null.");
        }

        // STF format, determine the size of the tree
        int size = bis.readBits(IHuffConstants.BITS_PER_INT);
        if (size <= 0) {
            viewer.showError("Error reading compressed file. Invalid tree size.");
            return false;
        }
        // gather the tree builder bits in a separate array to access index
        int[] bits = new int[size];
        for (int i = 0; i < size; i++) {
            int bit = bis.readBits(1);
            if (bit < 0) {
                viewer.showError("Error reading compressed file. Tree header is incomplete.");
                return false;
            }
            bits[i] = bit;
        }
        // use a one-element array to keep track of the index in the recursive helper
        int[] index = new int[1];
        // tree must be rebuilt because decoding requires the original structure
        TreeNode rebuilt = rebuildTree(bits, index);
        if (rebuilt == null) {
            return false;
        }
        // make sure all bits were used
        if (index[0] != bits.length) {
            viewer.showError("Error reading compressed file. Malformed tree header.");
            return false;
        }
        // needed because the decoder uses the instance variable to read the bits
        tree = new HuffmanTree(rebuilt);
        return true;
    }

    /**
     * Helper to rebuild a Huffman tree from preorder bit encoding.
     * @param bits array of bits describing the tree
     * @param index single-element array used as mutable index
     * @return root of rebuilt tree
     */
    private TreeNode rebuildTree(int[] bits, int[] index) {
        // ensure we do not read past end of header
        if (index[0] >= bits.length) {
            viewer.showError("Tree header ended unexpectedly.");
            return null;
        }
        int bit = bits[index[0]];
        index[0]++;
        if (bit == 0) {
            // recursive step, internal node, create children and repeat construction for them
            TreeNode left = rebuildTree(bits, index);
            if (left == null) {
                return null;
            }
            TreeNode right = rebuildTree(bits, index);
            if (right == null) {
                return null;
            }
            return new TreeNode(left, -1, right);
        }
        else {
            // leaf node, read next 9 bits for the value
            int value = 0;
            for (int i = 0; i < IHuffConstants.BITS_PER_WORD + 1; i++) {
                // ensure no out-of-bounds read
                if (index[0] >= bits.length) {
                    viewer.showError("Tree header ended inside leaf value.");
                    return null;
                }
                // shift current value left and add the next bit to build the 9-bit leaf value
                value = (value << 1) | bits[index[0]];
                index[0]++;
            }
            return new TreeNode(value, 0);
        }
    }

    /**
     * Decode all compressed bits from the input stream and write the corresponding uncompressed
     * bytes to the output stream.
     * pre: bis != null && out != null
     * post: uncompressed data written to out
     * @param bis the BitInputStream providing compressed bits
     * @param out the OutputStream to which uncompressed bytes are written
     * @return the number of bits written to the output
     * @throws IOException if an error occurs during reading or writing
     */
    public int decode(BitInputStream bis, OutputStream out) throws IOException {
        if (bis == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: decode(). Input BitInputStream cannot be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("Violation of pre: decode(). OutputStream is null.");
        }
        if (tree == null || tree.getRoot() == null) {
            viewer.showError("Error. No Huffman tree found.");
            return -1;
        }

        TreeNode current = tree.getRoot();
        int bitsWritten = 0;
        // decode must run until PSEUDO_EOF because compressed files do not store total bit length
        while (true) {
            int bit = bis.readBits(1);
            if (bit == -1) {
                viewer.showError("Error reading compressed file. No PSEUDO_EOF value.");
                return -1;
            }
            current = (bit == 0) ? current.getLeft() : current.getRight();
            if (current.isLeaf()) {
                // reached leaf, decode compressed value and write uncompressed ver to output
                int value = current.getValue();
                if (value == IHuffConstants.PSEUDO_EOF) {
                    // end of compressed stream, done writing bits
                    out.flush();
                    return bitsWritten;
                }
                out.write(value);
                bitsWritten += IHuffConstants.BITS_PER_WORD;
                // go back to root for the next symbol
                current = tree.getRoot();
            }
        }
    }

    /**
     * Return the Huffman tree reconstructed from header data.
     * pre: none
     * post: returns the tree used for decoding
     * @return the reconstructed Huffman tree
     */
    public HuffmanTree getTree() {
        return tree;
    }
}