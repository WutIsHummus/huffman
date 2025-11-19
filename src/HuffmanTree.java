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

/**
 * A Huffman tree built from a frequency table. Used to generate
 * Huffman codes for compression and to store the tree structure.
 *
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
     * Build the Huffman tree using the given frequency table.
     * pre: freq != null && freq.length == IHuffConstants.ALPH_SIZE + 1 && freq[IHuffConstants.PSEUDO_EOF] == 1
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
            // elements at the front of queue are smallest, so make them the children of parent
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
     * or null if v does not appear in the tree
     */
    public String[] makeCodes() {
        if (root == null) {
            throw new IllegalArgumentException(
                    "Violation of precondition: makeCodes(). Root cannot be null.");
        }

        String[] codes = new String[IHuffConstants.ALPH_SIZE + 1];
        buildCodes(root, "", codes);
        return codes;
    }

    /**
     * Helper method to fill the codes array by traversing the Huffman tree.
     * pre: codes != null && codes.length == IHuffConstants.ALPH_SIZE + 1
     * post: codes array contains Huffman encodings for all leaf values
     * @param node the current TreeNode in the traversal
     * @param path the bitstring path taken to reach this node
     * @param codes the array to fill with encodings
     */
    private void buildCodes(TreeNode node, String path, String[] codes) {
        // base case, fell out of tree
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            // we know this is a leaf, so record the completed path
            codes[node.getValue()] = path;
            return;
        }
        // recursive step, move in appropriate dir and append path with 1 or 0 depending on dir
        buildCodes(node.getLeft(), path + "0", codes);
        buildCodes(node.getRight(), path + "1", codes);
    }
}