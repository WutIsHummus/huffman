/*  Student information for assignment:
 *
 *  On OUR honor, Alperen Aydin and Harshal Dhaduk, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 1
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
 * Implements IHuffProcessor by coordinating preprocessing, compression, and decompression for
 * Huffman-coded files.
 * pre: none
 * post: supports compression and decompression as defined in IHuffProcessor
 */
public class SimpleHuffProcessor implements IHuffProcessor {
    // viewer for reporting status/errors
    private IHuffViewer myViewer;
    // object used for decompression
    private Decompressor decompressor;
    // frequency table to keep track of how many times each byte value appears in the input
    private int[] freqs;
    // object used for compression
    private Compressor compressor;
    // stores whether the format is SCF or STF
    private int headerFormat;
    // estimated compressed size
    private int estimatedCompressed;
    // uncompressed size in bits
    private int originalBits;

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
        if (in == null) {
            throw new IllegalArgumentException("Violation of precondition: " +
                    "preprocessCompress(). InputStream cannot be null.");
        }

        // Reset previous state to avoid stale values if preprocess is called again
        this.freqs = null;
        this.compressor = null;
        this.estimatedCompressed = 0;
        this.originalBits = 0;

        this.headerFormat = headerFormat;
        // create frequency table to send as input to buildTree()
        freqs = new int[ALPH_SIZE + 1];
        BitInputStream bis = new BitInputStream(in);
        int value = bis.readBits(BITS_PER_WORD);
        originalBits = 0;
        while (value != -1) {
            // haven't reached end of input yet, update frequency array and total bits in og file
            freqs[value]++;
            originalBits += BITS_PER_WORD;
            value = bis.readBits(BITS_PER_WORD);
        }
        // include EOF so the decompressor knows when to stop
        freqs[PSEUDO_EOF] = 1;
        compressor = new Compressor(freqs);
        compressor.buildTree();
        compressor.buildCodes();
        estimatedCompressed = estimateCompressedSize(freqs, headerFormat);
        // estimate compressed b/c you don't know actual compressedBits while still in preprocess
        return originalBits - estimatedCompressed;
    }

    /**
     * Estimate the total number of bits that would be written in the compressed file using the
     * frequency table and the generated Huffman codes.
     * pre: freqs != null
     * post: returns the estimated number of bits in the final compressed file
     * @param freqs the frequency table from preprocessCompress()
     * @param format the desired header format (SCF or STF)
     * @return estimated total number of bits in the compressed output
     * @throws IllegalArgumentException if freqs is null
     */
    private int estimateCompressedSize(int[] freqs, int format) {
        if (freqs == null) {
            throw new IllegalArgumentException("Violation of precondition: " +
                    "estimateCompressedSize(). Frequency table cannot be null.");
        }

        int bits = 0;
        // skip over magic number and header format
        bits += BITS_PER_INT;
        bits += BITS_PER_INT;
        if (format == STORE_COUNTS) {
            // SCT format, header cost is 256 * 32
            bits += ALPH_SIZE * BITS_PER_INT;
        }
        else {
            // STF format, compute size in preorder representation
            int size = compressor.getTree().computeTreeSize();
            bits += BITS_PER_INT;
            bits += size;
        }
        // retrieve codes generated during before to estimate how many bits actual data will cost
        String[] codes = compressor.getCodesForEstimate();
        // add bits for actual data
        for (int i = 0; i < ALPH_SIZE; i++) {
            if (freqs[i] > 0 && codes[i] != null) {
                bits += freqs[i] * codes[i].length();
            }
        }
        // don't forget to factor in EOF's bits!
        bits += codes[PSEUDO_EOF].length();
        return bits;
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method && in != null
     *           && out != null && internal fields (freqs and compressor) initialized
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
        if (in == null || out == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: compress(). Streams cannot be null.");
        }
        if (compressor == null || freqs == null) {
            throw new IllegalStateException(
                    "Violation of precondition: preprocessCompress must be called first.");
        }

        if (!force && estimatedCompressed >= originalBits) {
            // compression would not save space and user did not force it, abort the write
            if (myViewer != null) {
                myViewer.showError("Compressed file is not smaller. Use force to write anyway.");
            }
            return -1;
        }
        // safe to compress because preprocessing guarantees valid codes and tree
        return compressor.writeCompressed(in, out, headerFormat);
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
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
     * Sets the viewer used for status and error messages, and initializes the decompressor
     * helper that depends on the viewer.
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
        decompressor = new Decompressor(myViewer);
    }
}
