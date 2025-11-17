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

import java.util.ArrayList;

/**
 * A simple priority queue used for building Huffman trees.
 * Maintains ascending order by frequency with fair tie-breaking.
 * pre: none
 * post: constructs an empty priority queue
 */
public class PriorityQueue314 {

    // internal list used to store TreeNodes in priority order
    private ArrayList<TreeNode> data;

    /**
     * Construct an empty priority queue
     */
    public PriorityQueue314() {
        data = new ArrayList<>();
    }

    /**
     * Add a node to the queue following ascending frequency order and fair tie-breaking.
     * @param node the TreeNode to enqueue
     */
    public void enqueue(TreeNode node) {
        int freq = node.getFrequency();
        int i = 0;
        // skip all nodes with strictly lower frequency to ensure lower frequency goes earlier
        while (i < data.size() && data.get(i).getFrequency() < freq) {
            i++;
        }
        // skip all nodes with equal frequency to ensure adding in a fair way
        while (i < data.size() && data.get(i).getFrequency() == freq) {
            i++;
        }
        // arrived at correct spot
        data.add(i, node);
    }

    /**
     * Remove and return the highest priority (lowest frequency) node.
     * pre: !isEmpty()
     * @return the removed TreeNode
     */
    public TreeNode dequeue() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Violation of precondition: dequeue(). Cannot dequeue" +
                    " from an empty PriorityQueue314.");
        }

        return data.remove(0);
    }

    /**
     * Look at the highest priority node without removing it.
     * pre: !isEmpty()
     * @return the first TreeNode in the queue
     */
    public TreeNode peek() {
        if (data.isEmpty()) {
            throw new IllegalStateException("Violation of precondition: peek(). Cannot peek" +
                    " from an empty PriorityQueue314.");
        }

        return data.get(0);
    }

    /**
     * Determine if the queue contains no elements
     * @return true if queue is empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Determine the number of elements within the queue
     * @return number of items in the queue
     */
    public int size() {
        return data.size();
    }
}
