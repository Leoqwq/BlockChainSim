import eduni.simjava.*;
import eduni.simjava.distributions.*;

import java.util.PriorityQueue;

import java.util.Comparator;

public class BlockChainSim {
    public static void main(String[] args) {
        Sim_system.initialise();

        GenerateBlock generateBlock = new GenerateBlock("GenerateBlock");
        GenerateTransaction generateTransaction = new GenerateTransaction("GenerateTransaction");
        TransactionWaitingList transactionWaitingList = new TransactionWaitingList("TransactionWaitingList");

        Sim_system.link_ports("GenerateBlock", "BlockOut", "TransactionWaitingList", "BlockIn");
        Sim_system.link_ports("GenerateTransaction", "TransactionOut", "TransactionWaitingList", "TransactionIn");
        Sim_system.link_ports("TransactionWaitingList", "TransactionWaitingListOut", "GenerateTransaction", "TransactionWaitingListIn");

        // Generate 10 blocks in total
        Sim_system.set_termination_condition(Sim_system.EVENTS_COMPLETED, "TransactionWaitingList", 0, 30, true);

        // Set Seed
        Sim_system.set_seed(3);

        Sim_system.run();
    }
}

class GenerateBlock extends Sim_entity {
    private final Sim_port out;
    private final Sim_random_obj generateProb;
    private int blockCount = 0;
    public GenerateBlock(String name) {
        super(name);
        out = new Sim_port("BlockOut");
        add_port(out);

        generateProb = new Sim_random_obj("Block's Generation Probability every minute");
        add_generator(generateProb);
    }

    public void body() {
        while(Sim_system.running()) {
            double p = generateProb.sample();

            // 20% of chance to generate a block every minute
            if (p <= 0.20) {
                Block b = new Block(blockCount);

                sim_schedule(out, 0.0, 0, b);

                blockCount++;
            }

            sim_pause(1);
        }
    }
}

class GenerateTransaction extends Sim_entity {
    private final Sim_port out;
    private final Sim_port in;
    private final Sim_random_obj randomDelay;
    // private final Sim_random_obj randomTip;
    private final Sim_random_obj randomAmount;
    private int transactionCount = 0;
    private int declinedTransactionCount = 0;
    private double threshold = 3000.0;

    public GenerateTransaction(String name) {
        super(name);
        in = new Sim_port("TransactionWaitingListIn");
        out = new Sim_port("TransactionOut");
        add_port(in);
        add_port(out);

        randomDelay = new Sim_random_obj("Transaction's Generation Delay");
        // randomTip = new Sim_random_obj("Transaction Tip");
        randomAmount = new Sim_random_obj("Transaction Amount");
        add_generator(randomDelay);
        // add_generator(randomTip);
        add_generator(randomAmount);
    }

    public void body() {
        while (Sim_system.running()) {
            // Random amount ranged from $0 to $10000
            double amount = randomAmount.sample() * 10000.0;

            // Random tip ranged from $0 to 500
            // double tip = randomTip.sample() * 500.0;

            // tip = amount in this phase
            double tip = amount;

            Transaction t = new Transaction(transactionCount, tip, amount);
            transactionCount++;

            // A transaction is declined if and only if its amount/tip is smaller than threshold
            if (t.getTip() < threshold) {
                declinedTransactionCount++;
            } else {
                sim_schedule(out, 0.0, 1, t);
            }

            // Random transaction generation delay ranged from 0s to 0.1s
            double delay = randomDelay.sample() * 0.1;

            sim_pause(delay);
        }

        System.out.println("\n--------------------------------Simulation Report------------------------------");
        System.out.println("The total number of transactions generated: " + transactionCount);
        System.out.println("The total number of declined transactions: " + declinedTransactionCount);

        // Update the threshold when a block is generated
        Sim_event e = new Sim_event();
        sim_process_until(e);
        if (e.get_tag() == 2) {
            threshold = (double)e.get_data();
        }
        sim_completed(e);
    }
}

class TransactionWaitingList extends Sim_entity {
    private final Sim_port blockIn;
    private final Sim_port transactionIn;
    private final Sim_port out;
    private final Comparator<Transaction> comparator = new TransactionTipComparator();
    private final PriorityQueue<Transaction> transactionList = new PriorityQueue<Transaction>(comparator.reversed());
    private double oldThreshold = 3000.0;


    public TransactionWaitingList(String name) {
        super(name);
        blockIn = new Sim_port("BlockIn");
        transactionIn = new Sim_port("TransactionIn");
        out = new Sim_port("TransactionWaitingListOut");
        add_port(blockIn);
        add_port(transactionIn);
        add_port(out);
    }

    public void body() {
        System.out.println("Initial threshold: " + oldThreshold);

        while (Sim_system.running()) {
            Sim_event e = new Sim_event();
            sim_get_next(e);

            if (e.get_tag() == 0) {
                System.out.println("\nBefore block generation list length: " + transactionList.size());
                System.out.println("A Block just generated!");

                int index = 0;
                Block b = (Block)e.get_data();

                while (!transactionList.isEmpty() && index < 100) {
                    b.addTransactions(transactionList.poll(), index);
                    index++;
                }
                System.out.println("The block contains " + b.getNumOfTransactions() + " transactions.");

                // b.listTransactions();
                System.out.println("Post block generation list length: " + transactionList.size());

                sim_completed(e);

                // Calculate the new threshold
                double c = 0.4;
                double fullnessLevel = b.getNumOfTransactions() / 100.0;
                double targetLoad = 0.8;
                double newThreshold = oldThreshold * (1 + c * (fullnessLevel - targetLoad) / targetLoad);

                // Update the new threshold
                oldThreshold = newThreshold;
                sim_schedule(out, 0.0, 2, newThreshold);
                System.out.println("New threshold: "+ newThreshold);
            } else if (e.get_tag() == 1) {
                transactionList.add((Transaction)e.get_data());

                sim_completed(e);
            }
        }
    }
}

// Comparator
class TransactionTipComparator implements Comparator<Transaction> {
    public int compare(Transaction t1, Transaction t2)
    {
        return Double.compare(t1.getTip(), t2.getTip());
    }
}


