package uk.dsxt.voting.common.nxt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Instant;
import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.nxt.walletapi.*;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.common.utils.web.HttpHelper;
import uk.dsxt.voting.common.utils.web.RequestType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public class NxtWalletManager implements WalletManager {

    private final File workingDir;
    private final String jarPath;
    private final ObjectMapper mapper;
    private final String mainAddress;
    private final String port;
    private final HttpHelper httpHelper;
    private final List<String> javaOptions = new ArrayList<>();

    private final String passphrase;
    private final String name;
    private final String nxtPropertiesPath;
    private final boolean useUncommittedTransactions;

    private String accountId;
    private String selfAccount;
    private Process nxtProcess;
    private boolean isInitialized = false;

    private Set<String> loadedTransactions = new HashSet<>();
    private Set<String> loadedBlocks = new HashSet<>();

    public NxtWalletManager(Properties properties, String nxtPropertiesPath, String name, String mainAddress, String passphrase, 
                            int connectionTimeout, int readTimeout) {
        this.nxtPropertiesPath = nxtPropertiesPath;
        this.name = name;
        this.mainAddress = mainAddress;
        this.passphrase = passphrase;
        this.useUncommittedTransactions = Boolean.valueOf(properties.getProperty("nxt.useUncommittedTransactions", Boolean.FALSE.toString()));
        workingDir = new File(System.getProperty("user.dir"));
        log.info("Working directory (user.dir): {}", workingDir.getAbsolutePath());

        jarPath = properties.getProperty("nxt.jar.path");

        String javaOptionsStr = properties.getProperty("nxt.javaOptions");
        if (javaOptionsStr != null && !javaOptionsStr.isEmpty()) {
            for (String property : javaOptionsStr.split(";")) {
                if (!property.isEmpty())
                    javaOptions.add(property);
            }
        }

        httpHelper = new HttpHelper(connectionTimeout, readTimeout);

        mapper = new ObjectMapper();
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Properties nxtProperties = PropertiesHelper.loadPropertiesFromPath(nxtPropertiesPath);
        port = properties.getProperty("nxt.apiServerPort");
        nxtProperties.setProperty("nxt.peerServerPort", properties.getProperty("nxt.peerServerPort"));
        nxtProperties.setProperty("nxt.apiServerPort", port);
        nxtProperties.setProperty("nxt.dbDir", properties.getProperty("nxt.dbDir"));
        nxtProperties.setProperty("nxt.testDbDir", properties.getProperty("nxt.dbDir"));
        nxtProperties.setProperty("nxt.defaultPeers", properties.getProperty("nxt.defaultPeers"));
        nxtProperties.setProperty("nxt.defaultTestnetPeers", properties.getProperty("nxt.defaultTestnetPeers"));
        nxtProperties.setProperty("nxt.isOffline", properties.getProperty("nxt.isOffline"));
        nxtProperties.setProperty("nxt.isTestnet", properties.getProperty("nxt.isTestnet"));
        nxtProperties.setProperty("nxt.timeMultiplier", properties.getProperty("nxt.timeMultiplier"));
        try (FileOutputStream fos = new FileOutputStream(nxtPropertiesPath)) {
            nxtProperties.store(fos, "");
        } catch (Exception e) {
            String errorMessage = String.format("Can't save wallet. Error: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    private <T> T sendApiRequest(WalletRequestType type, String secretPhrase, Consumer<Map<String, String>> argumentsBuilder, Class<T> tClass) {
        return sendApiRequest(type, keyToValue -> {
            keyToValue.put("secretPhrase", secretPhrase);
            argumentsBuilder.accept(keyToValue);
        }, tClass);
    }

    private <T> T sendApiRequest(WalletRequestType type, Consumer<Map<String, String>> argumentsBuilder, Class<T> tClass) {
        try {
            if (type != WalletRequestType.GET_ACCOUNT_ID)
                waitInitialize();
            Map<String, String> arguments = new LinkedHashMap<>();
            arguments.put("requestType", type.toString());
            argumentsBuilder.accept(arguments);
            String response = httpHelper.request(String.format("http://localhost:%s/nxt", port), arguments, RequestType.POST);
            T result = null;
            try {
                result = mapper.readValue(response, tClass);
            } catch (IOException e) {
                if (isInitialized)
                    log.error("Wallet {}. Can't parse response: {}. Error message: {}", name, response, e.getMessage());
            }
            return result;
        } catch (Exception e) {
            if (isInitialized)
                log.error("Wallet {}. Method {} failed. Error message {}", name, type, e.getMessage());
            return null;
        }
    }

    @Override
    public void start() {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.addAll(javaOptions);
            cmd.add("-jar");
            cmd.add(jarPath);
            cmd.add(nxtPropertiesPath);

            log.debug("Starting nxt wallet process: {}", StringUtils.join(cmd, " "));
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workingDir);
            processBuilder.redirectError(new File(String.format("./logs/wallet_err_%s.log", name)));
            processBuilder.redirectOutput(new File(String.format("./logs/wallet_out_%s.log", name)));
            processBuilder.command(cmd);
            nxtProcess = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            waitInitialize();
            log.info("connector started");
        } catch (Exception e) {
            String errorMessage = String.format("Couldn't run wallet. Error: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    @Override
    public void stop() {
        isInitialized = false;
        try {
            if (nxtProcess.isAlive())
                nxtProcess.destroyForcibly();
            log.info("connector stopped");
        } catch (Exception e) {
            log.error("stop method failed", e);
        }
    }

    @Override
    public String sendMessage(byte[] body) {
        return sendMessage(mainAddress, body, passphrase);
    }

    private String sendMessage(String recipient, byte[] body, String senderPassword) {
        SendTransactionResponse transaction = sendApiRequest(WalletRequestType.SEND_MESSAGE, senderPassword, keyToValue -> {
            keyToValue.put("recipient", recipient);
            keyToValue.put("feeNQT", "0");
            keyToValue.put("message", new String(body, StandardCharsets.UTF_8));
            keyToValue.put("deadline", "60");
        }, SendTransactionResponse.class);
        if (transaction != null)
            return getNxtId(transaction.getTransactionId());
        return null;
    }

    @Override
    public List<Message> getNewMessages(long timestamp) {
        Set<String> resultIds = new HashSet<>();
        List<Message> result = new ArrayList<>();
        List<Message> confirmedMessages = getConfirmedMessages();
        List<Message> unconfirmedMessages = useUncommittedTransactions ? getUnconfirmedMessages() : null;
        log.debug("getNewMessages confirmed={} unconfirmed={}", 
            confirmedMessages == null ? "null" : Integer.toString(confirmedMessages.size()), unconfirmedMessages == null ? "null" : Integer.toString(unconfirmedMessages.size()) );        
        if (confirmedMessages != null) {
            resultIds.addAll(confirmedMessages.stream().map(Message::getId).collect(Collectors.toList()));
            confirmedMessages.stream().forEach(result::add);
        }
        if (unconfirmedMessages != null) {
            unconfirmedMessages.stream().filter(m -> !resultIds.contains(m.getId())).forEach(result::add);
        }
        return result;
    }

    private List<Message> getConfirmedMessages() {
        BlockchainStatusResponse statusResult = sendApiRequest(WalletRequestType.GET_BLOCKCHAIN_STATUS, keyToValue -> {}, BlockchainStatusResponse.class);
        if (statusResult == null)
            return null;
        List<Message> result = new ArrayList<>();
        for(String blockId = statusResult.getLastBlock(); blockId != null && !loadedBlocks.contains(blockId);) {
            final String currentBlock = blockId;
            BlockResponse blockResponse = sendApiRequest(WalletRequestType.GET_BLOCK, keyToValue -> {
                keyToValue.put("block", currentBlock);
                keyToValue.put("timestamp", "0");
            }, BlockResponse.class);
            if (blockResponse == null)
                break;
            boolean breakOnTransaction = false;
            int allCnt = 0, dupCnt = 0, loadedCnt = 0, otherCnt = 0;
            for(String transactionId : blockResponse.getTransactions()) {
                allCnt++;
                if (!loadedTransactions.contains(transactionId)) {
                    Transaction transaction = sendApiRequest(WalletRequestType.GET_TRANSACTION, keyToValue -> {
                        keyToValue.put("transaction", transactionId);
                    }, Transaction.class);
                    if (transaction == null) {
                        breakOnTransaction = true;
                        log.warn("break on transaction {} in block {}", getNxtId(transactionId), getNxtId(blockId));
                        break;
                    }
                    if (transaction.getAttachment() != null && transaction.getAttachment().isMessageIsText()) {
                        result.add(new Message(getNxtId(transactionId), transaction.getAttachment().getMessage().getBytes(StandardCharsets.UTF_8), true));
                        loadedCnt++;
                    } else {
                        log.debug("transaction without message {} in block {} at {} type {}", 
                            getNxtId(transactionId), getNxtId(blockId), new Instant(transaction.getTimestamp()* 1000L), transaction.getType());
                        otherCnt++;
                    }
                    loadedTransactions.add(transactionId);
                } else
                    dupCnt++;
            }
            if (!breakOnTransaction) {
                loadedBlocks.add(blockId);
                log.debug("getConfirmedMessages all {} duplicates {} loaded {} other {} transactions from block {}", allCnt, dupCnt, loadedCnt, otherCnt, getNxtId(blockId));                
            }
            blockId = blockResponse.getPreviousBlock();
        }
        return result;
    }

    private List<Message> getUnconfirmedMessages() {
        UnconfirmedTransactionsResponse result = sendApiRequest(WalletRequestType.GET_UNCONFIRMED_TRANSACTIONS,
            keyToValue -> keyToValue.put("account", mainAddress), UnconfirmedTransactionsResponse.class);
        if (result == null)
            return null;
        try {
            return Arrays.asList(result.getUnconfirmedTransactions()).stream().
                filter(t -> t.getAttachment() != null && t.getAttachment().isMessageIsText() && loadedTransactions.add(t.getTransaction())).
                map(t -> new Message(getNxtId(t.getTransaction()), t.getAttachment().getMessage().getBytes(StandardCharsets.UTF_8), false)).
                collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getUnconfirmedMessages failed. Message: {}", e.getMessage());
            return null;
        }
    }

    private boolean startForging() {
        StartForgingResponse response = sendApiRequest(WalletRequestType.START_FORGING, passphrase, keyToValue -> {
        }, StartForgingResponse.class);
        log.debug("startForging. response={}", response);
        return response != null;
    }

    private void waitInitialize() {
        if (passphrase == null || passphrase.isEmpty()) {
            log.warn("waitInitialize. Start without address. Password is null or empty.");
            return;
        }
        if (selfAccount != null)
            return;
        log.info("waitInitialize. Start wallet initialization...");
        while ((accountId == null || selfAccount == null) && !Thread.currentThread().isInterrupted()) {
            try {
                if (accountId == null || selfAccount == null) {
                    AccountResponse account = sendApiRequest(WalletRequestType.GET_ACCOUNT_ID, passphrase, keyToValue -> {
                    }, AccountResponse.class);
                    if (account != null) {
                        accountId = account.getAccountId();
                        selfAccount = account.getAddress();
                    }
                }
                sleep(100);
            } catch (Exception e) {
                log.error("waitInitialize. waitInitialize. Error: {}", e.getMessage());
            }
        }
        log.info("waitInitialize. selfAccount={}", selfAccount);
        while (!startForging() && !Thread.currentThread().isInterrupted()) {
            sleep(100);
        }
        log.info("waitInitialize. finished.");
        isInitialized = true;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getNxtId(String id) {
        return Long.toString(Long.parseUnsignedLong(id));
    }
}
