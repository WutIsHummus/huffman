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

/**
 * A Huffman tree built from a frequency table. Used to generate Huffman codes for compression
 * and to store the tree structure.
 * pre: none
 * post: tree is constructed after calling constructor
 */
public class HuffmanTree {
    // the root of the Huffman tree
    private TreeNode root;

    /**
     * Construct a Huffman tree from the given frequency table.
     * pre: freq != null && freq.length == IHuffConstants.ALPH_SIZE + 1
     * post: root references the completed Huffman tree
     * @param freq the frequency array for values 0–255 and PSEUDO_EOF
     */
    public HuffmanTree(int[] freq) {
        if (freq == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: HuffmanTree(int[]). Input array cannot be null.");
        }
        if (freq.length != IHuffConstants.ALPH_SIZE + 1) {
            throw new IllegalArgumentException(
                    "Violation of precondition: HuffmanTree(int[]). Input array is not proper " +
                            "length.");
        }

        buildTree(freq);
    }

    /**
     * Build the Huffman tree using the given frequency table.
     * pre: freq != null && freq.length == IHuffConstants.ALPH_SIZE + 1 &&
     *      freq[IHuffConstants.PSEUDO_EOF] == 1
     * post: root references the completed Huffman tree containing all values with freq > 0
     * @param freq the frequency array used to construct the Huffman tree
     * @throws IllegalArgumentException if freq violates the precondition
     */
    private void buildTree(int[] freq) {
        if (freq == null || freq.length != IHuffConstants.ALPH_SIZE + 1 ||
                freq[IHuffConstants.PSEUDO_EOF] != 1) {
            throw new IllegalArgumentException(
                    "Violation of precondition: buildTree(). Invalid frequency array.");
        }

        PriorityQueue314 nodeQueue = new PriorityQueue314();
        for (int value = 0; value < freq.length; value++) {
            if (freq[value] > 0) {
                // value appears in the file, so create a leaf node
                TreeNode node = new TreeNode(value, freq[value]);
                nodeQueue.enqueue(node);
            }
        }
        while (nodeQueue.size() > 1) {
            // elements at the front of queue are smallest, so make them the children of
            // parent
            TreeNode left = nodeQueue.dequeue();
            TreeNode right = nodeQueue.dequeue();
            TreeNode parent = new TreeNode(left, -1, right);
            // add parent/children pair back into queue to continue building tree
            nodeQueue.enqueue(parent);
        }
        // the last remaining element will be the completed Huffman tree
        root = nodeQueue.dequeue();
    }

    /**
     * Construct a HuffmanTree directly from a given root.
     * pre: root != null
     * post: this.root == root
     * @param root the reconstructed Huffman tree root
     */
    public HuffmanTree(TreeNode root) {
        if (root == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: HuffmanTree(TreeNode). Root cannot be null.");
        }

        this.root = root;
    }

    /**
     * Return the root of the Huffman tree.
     * pre: none
     * post: returns the root TreeNode of this Huffman tree
     * @return the root TreeNode
     */
    public TreeNode getRoot() {
        return root;
    }

    /**
     * Generate the Huffman bitstring codes for each value in the tree.
     * pre: root != null
     * post: returns an array of codes for all values present in the tree
     * @return an array where codes[v] is the Huffman encoding for value v,
     *         or null if v does not appear in the tree
     */
    public String[] makeCodes() {
        if (root == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: makeCodes(). Root cannot be null.");
        }

        String[] codes = new String[IHuffConstants.ALPH_SIZE + 1];
        buildCodes(root, new StringBuilder(), codes);
        return codes;
    }

    /**
     * Helper method to fill the codes array by traversing the Huffman tree.
     * pre: none
     * post: codes array contains Huffman encodings for all leaf values
     * @param node  the current TreeNode in the traversal
     * @param path  the bitstring path taken to reach this node
     * @param codes the array to fill with encodings
     */
    private void buildCodes(TreeNode node, StringBuilder path, String[] codes) {
        // base case, fell out of tree, no path to encode
        if (node != null) {
            if (node.isLeaf()) {
                // we know this is a leaf, so record the completed path
                codes[node.getValue()] = path.toString();
            } else {
                // recursive step, move in appropriate dir and append path w/ 1 or 0
                path.append('0');
                buildCodes(node.getLeft(), path, codes);
                path.deleteCharAt(path.length() - 1);
                path.append('1');
                buildCodes(node.getRight(), path, codes);
                path.deleteCharAt(path.length() - 1);
            }
        }
    }

    /**
     * Returns the number of bits needed to encode this Huffman tree in STF.
     * @return total preorder-encoding size in bits
     */
    public int computeTreeSize() {
        return computeTreeSize(root);
    }

    /**
     * Recursive helper. Computes the number of bits required to store the Huffman tree in STF
     * using a preorder traversal.
     * pre: none
     * post: returns the size, in bits, of the preorder tree encoding
     * @param node the current node in the Huffman tree
     * @return total bits needed to encode the subtree rooted at {@code node}
     */
    public int computeTreeSize(TreeNode node) {
        if (node == null) {
            // base case, reached end of tree, null nodes are not written in headers
            return 0;
        }
        if (node.isLeaf()) {
            // leaf node, 1 bit for the leaf flag + 9 bits for the stored value
            return 1 + (IHuffConstants.BITS_PER_WORD + 1);
        }
        // else internal node, recursively compute the size of left and right subtrees + 1 bit for
        // the internal node
        return 1 + computeTreeSize(node.getLeft()) + computeTreeSize(node.getRight());
    }
}