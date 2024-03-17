public class Transaction {
    private final int transactionID;
    private final double tip;
    private final double amount;

    public Transaction(int transactionID, double tip, double amount) {
        this.transactionID = transactionID;
        this.tip = tip;
        this.amount = amount;
    }

    public void displayTransactionInfo() {
        System.out.println("Transaction " + transactionID + ": Amount = $" + String.format("%.2f", amount) + ", Tip = $" + String.format("%.2f", tip));
    }

    public double getID() {
        return transactionID;
    }

    public double getTip() {
        return tip;
    }

    public double getAmount() {
        return amount;
    }
}
