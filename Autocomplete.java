import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
//Kirpa Chahal
//Oct 28 2021
//Comp 3270 Project

public class Autocomplete {
    /**
     * Uses binary search to find the index of the first Term in the passed in array
     * which is considered equivalent by a comparator to the given key. This method
     * should not call comparator.compare() more than 1+log n times, where n is the
     * size of a.
     * 
     * @param a          - The array of Terms being searched
     * @param key        - The key being searched for.
     * @param comparator - A comparator, used to determine equivalency between the
     *                   values in a and the key.
     * @return The first index i for which comparator considers a[i] and key as
     *         being equal. If no such index exists, return -1 instead.
     */
    public static int firstIndexOf(Term[] a, Term key, Comparator<Term> comparator) {
        // TODO: Implement firstIndexOf
        int starting = 0, finish = a.length - 1;
        int index = -1;
        while (starting <= finish) {
            int center = (starting + finish) / 2;
            Term cur = a[center];
            int differenceComp = comparator.compare(key, cur);
            if (differenceComp == 0)
                index = center;
            if (differenceComp <= 0)
                finish = center - 1;
            else
                starting = center + 1;
        }
        return index;
    }

    /**
     * The same as firstIndexOf, but instead finding the index of the last Term.
     * 
     * @param a          - The array of Terms being searched
     * @param key        - The key being searched for.
     * @param comparator - A comparator, used to determine equivalency between the
     *                   values in a and the key.
     * @return The last index i for which comparator considers a[i] and key as being
     *         equal. If no such index exists, return -1 instead.
     */
    public static int lastIndexOf(Term[] a, Term key, Comparator<Term> comparator) {
        // TODO: Implement lastIndexOf
        int starting = 0, finish = a.length - 1;
        int index = -1;
        while (starting <= finish) {
            int center = (starting + finish) / 2;
            Term cur = a[center];
            int differenceComp = comparator.compare(key, cur);
            if (differenceComp == 0)
                index = center;
            if (differenceComp < 0)
                finish = center - 1;
            else
                starting = center + 1;
        }
        return index;
    }

    public interface Autocompletor {

        /**
         * Returns the top k matching terms in descending order of weight. If there are
         * fewer than k matches, return all matching terms in descending order of
         * weight. If there are no matches, return an empty iterable.
         */
        public Iterable<String> topMatches(String prefix, int k);

        /**
         * Returns the single top matching term, or an empty String if there are no
         * matches.
         */
        public String topMatch(String prefix);

        /**
         * Return the weight of a given term. If term is not in the dictionary, return
         * 0.0
         */
        public double weightOf(String term);
    }

    /**
     * Implements Autocompletor by scanning through the entire array of terms for
     * every topKMatches or topMatch query.
     */
    public static class BruteAutocomplete implements Autocompletor {
        Term[] myTerms;

        public BruteAutocomplete(String[] terms, double[] weights) {
            if (terms == null || weights == null)
                throw new NullPointerException("One or more arguments null");
            if (terms.length != weights.length)
                throw new IllegalArgumentException("terms and weights are not the same length");
            myTerms = new Term[terms.length];
            HashSet<String> words = new HashSet<String>();
            for (int i = 0; i < terms.length; i++) {
                words.add(terms[i]);
                myTerms[i] = new Term(terms[i], weights[i]);
                if (weights[i] < 0)
                    throw new IllegalArgumentException("Negative weight " + weights[i]);
            }
            if (words.size() != terms.length)
                throw new IllegalArgumentException("Duplicate input terms");
        }

        public Iterable<String> topMatches(String prefix, int k) {
            if (k < 0)
                throw new IllegalArgumentException("Illegal value of k:" + k);
            PriorityQueue<Term> priorityQ = new PriorityQueue<Term>(k, new Term.WeightOrder());
            for (Term t : myTerms) {
                if (!t.getWord().startsWith(prefix))
                    continue;
                if (priorityQ.size() < k) {
                    priorityQ.add(t);
                } else if (priorityQ.peek().getWeight() < t.getWeight()) {
                    priorityQ.remove();
                    priorityQ.add(t);
                }
            }
            int numResults = Math.min(k, priorityQ.size());
            LinkedList<String> ret = new LinkedList<String>();
            for (int i = 0; i < numResults; i++) {
                ret.addFirst(priorityQ.remove().getWord());
            }
            return ret;
        }

        public String topMatch(String prefix) {
            String maxTerm = "";
            double maxWeight = -1;
            for (Term t : myTerms) {
                if (t.getWeight() > maxWeight && t.getWord().startsWith(prefix)) {
                    maxWeight = t.getWeight();
                    maxTerm = t.getWord();
                }
            }
            return maxTerm;
        }

        public double weightOf(String term) {
            for (Term t : myTerms) {
                if (t.getWord().equalsIgnoreCase(term))
                    return t.getWeight();
            }
            return 0;
        }
    }

    /**
     * 
     * Using a sorted array of Term objects, this implementation uses binary search
     * to find the top term(s).
     * 
     * @author Austin Lu, adapted from Kevin Wayne
     * @author Jeff Forbes
     */
    public static class BinarySearchAutocomplete implements Autocompletor {

        Term[] myTerms;

        /**
         * Given arrays of words and weights, initialize myTerms to a corresponding
         * array of Terms sorted lexicographically.
         * 
         * This constructor is written for you, but you may make modifications to it.
         * 
         * @param terms   - A list of words to form terms from
         * @param weights - A corresponding list of weights, such that terms[i] has
         *                weight[i].
         * @return a BinarySearchAutocomplete whose myTerms object has myTerms[i] = a
         *         Term with word terms[i] and weight weights[i].
         * @throws a NullPointerException if either argument passed in is null
         */
        public BinarySearchAutocomplete(String[] terms, double[] weights) {
            if (terms == null || weights == null)
                throw new NullPointerException("One or more arguments null");
            myTerms = new Term[terms.length];
            for (int i = 0; i < terms.length; i++) {
                myTerms[i] = new Term(terms[i], weights[i]);
            }
            Arrays.sort(myTerms);
        }

        /**
         * Required by the Autocompletor interface. Returns an array containing the k
         * words in myTerms with the largest weight which match the given prefix, in
         * descending weight order. If less than k words exist matching the given prefix
         * (including if no words exist), then the array instead contains all those
         * words. e.g. If terms is {air:3, bat:2, bell:4, boy:1}, then topKMatches("b",
         * 2) should return {"bell", "bat"}, but topKMatches("a", 2) should return
         * {"air"}
         * 
         * @param prefix - A prefix which all returned words must start with
         * @param k      - The (maximum) number of words to be returned
         * @return An array of the k words with the largest weights among all words
         *         starting with prefix, in descending weight order. If less than k such
         *         words exist, return an array containing all those words If no such
         *         words exist, reutrn an empty array
         * @throws a NullPointerException if prefix is null
         */
        public Iterable<String> topMatches(String prefix, int k) {
            if (prefix == null)
                throw new NullPointerException();
            int f = firstIndexOf(myTerms, new Term(prefix, 0), new Term.PrefixOrder(prefix.length()));
            int l = lastIndexOf(myTerms, new Term(prefix, 0), new Term.PrefixOrder(prefix.length()));
            if (l < 0)
                return new ArrayList<String>();
            PriorityQueue<Term> priorityQ = new PriorityQueue<Term>(k, new Term.WeightOrder());
            for (int i = f; i <= l; i++) {
                Term t = myTerms[i];
                if (priorityQ.size() < k) {
                    priorityQ.add(t);
                } else if (priorityQ.peek().getWeight() < t.getWeight()) {
                    priorityQ.remove();
                    priorityQ.add(t);
                }
            }
            int numResults = Math.min(k, priorityQ.size());
            LinkedList<String> ret = new LinkedList<String>();
            for (int i = 0; i < numResults; i++) {
                ret.addFirst(priorityQ.remove().getWord());
            }
            return ret;
        }

        /**
         * Given a prefix, returns the largest-weight word in myTerms starting with that
         * prefix. e.g. for {air:3, bat:2, bell:4, boy:1}, topMatch("b") would return
         * "bell". If no such word exists, return an empty String.
         * 
         * @param prefix - the prefix the returned word should start with
         * @return The word from myTerms with the largest weight starting with prefix,
         *         or an empty string if none exists
         * @throws a NullPointerException if the prefix is null
         * 
         */
        public String topMatch(String prefix) {
            if (prefix == null)
                throw new NullPointerException();
            int f = firstIndexOf(myTerms, new Term(prefix, 0), new Term.PrefixOrder(prefix.length()));
            int l = lastIndexOf(myTerms, new Term(prefix, 0), new Term.PrefixOrder(prefix.length()));
            ArrayList<Term> found = new ArrayList<Term>();
            if (l < 0)
                return "";
            double maxWeight = myTerms[f].getWeight();
            int maxWeightIndex = f;
            for (int i = f + 1; i <= l; i++) {
                if (myTerms[i].getWeight() > maxWeight) {
                    maxWeight = myTerms[i].getWeight();
                    maxWeightIndex = i;
                }
            }
            return myTerms[maxWeightIndex].getWord();
        }

        /**
         * Return the weight of a given term. If term is not in the dictionary, return
         * 0.0
         */
        public double weightOf(String term) {
            // TODO complete weightOf
            return 0.0;
        }
    }

    public static class TrieAutocomplete implements Autocompletor {

        /**
         * Root of entire trie
         */
        protected Node myRoot;

        /**
         * Constructor method for TrieAutocomplete. Should initialize the trie rooted at
         * myRoot, as well as add all nodes necessary to represent the words in terms.
         * 
         * @param terms   - The words we will autocomplete from
         * @param weights - Their weights, such that terms[i] has weight weights[i].
         * @throws NullPointerException     if either argument is null
         * @throws IllegalArgumentException if terms and weights are different weight
         */
        public TrieAutocomplete(String[] terms, double[] weights) {
            if (terms == null || weights == null)
                throw new NullPointerException("One or more arguments null");
            myRoot = new Node('-', null, 0);

            for (int i = 0; i < terms.length; i++) {
                add(terms[i], weights[i]);
            }
        }

        /**
         * Add the word with given weight to the trie. If word already exists in the
         * trie, no new nodes should be created, but the weight of word should be
         * updated.
         * 
         * In adding a word, this method should do the following: Create any necessary
         * intermediate nodes if they do not exist. Update the subtreeMaxWeight of all
         * nodes in the path from root to the node representing word. Set the value of
         * myWord, myWeight, isWord, and mySubtreeMaxWeight of the node corresponding to
         * the added word to the correct values
         * 
         * @throws a  NullPointerException if word is null
         * @throws an IllegalArgumentException if weight is negative.
         */
        private void add(String word, double weight) {
            if (word == null) {
                throw new NullPointerException("The input is invalid.");
            }
            if (weight < 0) {
                throw new IllegalArgumentException("The weight is invalid");
            }
            Node kunode = myRoot;
            Node kunodeNext;
            char index;
            for (int i = 0; i < word.length(); i++) {
                index = word.charAt(i);
                kunodeNext = kunode.getChild(index);
                if (kunodeNext == null) {
                    kunodeNext = new Node(index, kunode, weight);
                    kunode.children.put(index, kunodeNext);
                }
                if (kunode.mySubtreeMaxWeight < weight) {
                    kunode.mySubtreeMaxWeight = weight;
                }
                kunode = kunodeNext;
            }
            kunode.setWord(word);
            kunode.setWeight(weight);
            kunode.isWord = true;
        }

        /**
         * Required by the Autocompletor interface. Returns an array containing the k
         * words in the trie with the largest weight which match the given prefix, in
         * descending weight order. If less than k words exist matching the given prefix
         * (including if no words exist), then the array instead contains all those
         * words. e.g. If terms is {air:3, bat:2, bell:4, boy:1}, then topKMatches("b",
         * 2) should return {"bell", "bat"}, but topKMatches("a", 2) should return
         * {"air"}
         * 
         * @param prefix - A prefix which all returned words must start with
         * @param k      - The (maximum) number of words to be returned
         * @return An Iterable of the k words with the largest weights among all words
         *         starting with prefix, in descending weight order. If less than k such
         *         words exist, return all those words. If no such words exist, return
         *         an empty Iterable
         * @throws a NullPointerException if prefix is null
         */
        public Iterable<String> topMatches(String prefix, int k) {
            // TODO: Implement topKMatches
            if (prefix == null) {
                throw new NullPointerException("The given prefix is invalid.");
            }
            Node kunode = myRoot;
            char index;
            PriorityQueue<Node> kuQue = new PriorityQueue<Node>(new Node.ReverseSubtreeMaxWeightComparator());
            ArrayList<String> blankL = new ArrayList<String>();
            ArrayList<Node> topMatches = new ArrayList<Node>();
            ArrayList<String> tms = new ArrayList<String>();
            if (k == 0) {
                return blankL;
            }
            for (int i = 0; i < prefix.length(); i++) {
                index = prefix.charAt(i);
                kunode = kunode.getChild(index);
                if (kunode == null) {
                    return blankL;
                }
            }
            if (kunode != null) {
                kuQue.add(kunode);
            }
            do {
                kunode = kuQue.remove();
                for (Node n : kunode.children.values()) {
                    kuQue.add(n);
                }
                if (topMatches.size() >= k) {
                    Collections.sort(topMatches, Collections.reverseOrder());
                    break;
                }
                if (kunode.isWord) {
                    topMatches.add(kunode);
                }
            } while (!kuQue.isEmpty());
            for (Node n : topMatches) {
                tms.add(n.myWord);
            }
            return tms;
        }

        /**
         * Given a prefix, returns the largest-weight word in the trie starting with
         * that prefix.
         * 
         * @param prefix - the prefix the returned word should start with
         * @return The word from with the largest weight starting with prefix, or an
         *         empty string if none exists
         * @throws a NullPointerException if the prefix is null
         */
        public String topMatch(String prefix) {
            // TODO: Implement topMatch
            if (prefix == null) {
                throw new NullPointerException("The given prefix is invalid.");
            }
            PriorityQueue<Node> kuQueue = new PriorityQueue<Node>(new Node.ReverseSubtreeMaxWeightComparator());
            Node kunode = myRoot;
            char index;
            boolean realWrd = true;
            String empty = "";
            String topMatch;
            for (int i = 0; i < prefix.length(); i++) {
                index = prefix.charAt(i);
                if (kunode.children.containsKey(index)) {
                    kunode = kunode.getChild(index);
                } else {
                    realWrd = false;
                    break;
                }
            }
            if (!realWrd) {
                return empty;
            }
            while (!kunode.isWord && kunode.getWeight() < kunode.mySubtreeMaxWeight) {
                for (Node n : kunode.children.values()) {
                    kuQueue.add(n);
                }
                kunode = kuQueue.remove();
            }
            topMatch = kunode.getWord();
            return topMatch;
        }

        /**
         * Return the weight of a given term. If term is not in the dictionary, return
         * 0.0
         */
        public double weightOf(String term) {
            // TODO complete weightOf
            Node kunode = myRoot;
            int nada = 0;
            double weight = 0;
            for (int i = 0; i < term.length(); i++) {
                char index = term.charAt(i);
                kunode = kunode.children.get(index);
            }
            weight = kunode.myWeight;
            if (!kunode.isWord) {
                return nada;
            } else {
                return weight;
            }
        }

        /**
         * Optional: Returns the highest weighted matches within k edit distance of the
         * word. If the word is in the dictionary, then return an empty list.
         * 
         * @param word The word to spell-check
         * @param dist Maximum edit distance to search
         * @param k    Number of results to return
         * @return Iterable in descending weight order of the matches
         */
        public Iterable<String> spellCheck(String word, int dist, int k) {
            return null;
        }
    }
}
