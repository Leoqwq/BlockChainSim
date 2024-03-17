public class Block {
    private final int blockID;
    private final Transaction[] transactions = new Transaction[100];
    private int numOfTransactions = 0;

    public Block(int blockID) {
        this.blockID = blockID;
    }

    public void addTransactions(Transaction transaction, int index) {
        transactions[index] = transaction;
        numOfTransactions++;
    }

    public void listTransactions() {
        System.out.println("Block " + blockID + " contains " + numOfTransactions + " transactions: ");

        for (int i = 0; i < numOfTransactions; i++) {
            transactions[i].displayTransactionInfo();
        }
    }

    public int getID() {
        return blockID;
    }

    public int getNumOfTransactions() {
        return numOfTransactions;
    }
}
