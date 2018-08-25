package com.atypon.training.java.traniningproject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestTemplate;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.atypon.training.java.traniningproject.Utility.sha256;

@JsonIgnoreProperties
public final class Blockchain {

    @JsonIgnore
    public static float minimumTransaction = 0.01f;
    @JsonIgnore
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    @JsonIgnore
    public static PublicKey address;
    @JsonIgnore
    public static PrivateKey privateKey;
    @JsonIgnore
    static int difficulty = 5;
    private ArrayList<Block> chain;
    private int length;
    @JsonIgnore
    private ArrayList<Transaction> mempool = new ArrayList<>();
    @JsonIgnore
    private Set<Node> network = new HashSet<>();

    public Blockchain() {
        this.chain = new ArrayList<>();

        //Create Genesis Block
        String genesisBlockPreviousHash = Utility.repeat("0", 64);
        Transaction genesisBlockCoinbase = new Transaction(address);
        genesisBlockCoinbase.generateSignature(privateKey);
        genesisBlockCoinbase.transactionId = "0";
        genesisBlockCoinbase.outputs.add(new TransactionOutput(genesisBlockCoinbase.recipient, genesisBlockCoinbase.amount, genesisBlockCoinbase.transactionId));
        UTXOs.put(genesisBlockCoinbase.outputs.get(0).getId(), genesisBlockCoinbase.outputs.get(0));
        addTransaction(genesisBlockCoinbase);
        addBlock(genesisBlockPreviousHash);
    }


    public ArrayList<Block> getChain() {
        return chain;
    }

    public int getLength() {
        length = chain.size();
        return length;
    }

    @JsonIgnore
    public Block addBlock(String previousHash) {
        Block block = new Block(previousHash, mempool);
        block.mine(difficulty);
        mempool.clear();
        chain.add(block);
        return block;
    }

    @JsonIgnore
    public Block getPreviousBlock() {
        return chain.get(chain.size() - 1);
    }

    @JsonIgnore
    public String calculateHash(Block block) {
        String hash = sha256(block.toString());
        return hash.toUpperCase();
    }

    @JsonIgnore
    public boolean isChainValid(ArrayList<Block> chain) {
        Block previousBlock;
        Block currentBlock;
        String target = Utility.repeat("0", difficulty);
        for (int i = 1; i < chain.size(); i++) {
            currentBlock = chain.get(i);
            previousBlock = chain.get(i - 1);
            if (!currentBlock.getPreviousBlockHash().equalsIgnoreCase(previousBlock.getHash())) {
                return false;
            }

            boolean proofOfWorkValid = currentBlock.getHash().startsWith(target);
            if (!proofOfWorkValid) {
                return false;
            }
        }
        return true;
    }

    @JsonIgnore
    public void addTransaction(Transaction transaction) {
        this.mempool.add(transaction);
    }

    @JsonIgnore
    public void addNode(Node node) {
        this.network.add(node);
    }

    @JsonIgnore
    public void replaceChain() {
        ArrayList<Block> longestChain = this.chain;
        int maxLength = this.chain.size();
        RestTemplate restTemplate = new RestTemplate();
        for (Node node : network) {
            System.out.println(node.getAddress().toString() + "/get_chain");
            Blockchain nodeBlockchain = restTemplate.getForObject(node.getAddress().toString() + "/get_chain",
                    Blockchain.class);
            System.out.println(nodeBlockchain.getLength());
            if (nodeBlockchain.getLength() > maxLength && isChainValid(nodeBlockchain.chain)) {
                longestChain = nodeBlockchain.chain;
            }
        }
        this.chain = longestChain;
    }
}