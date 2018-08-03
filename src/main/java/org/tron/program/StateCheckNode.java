package org.tron.program;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.StringUtils;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.overlay.client.DatabaseGrpcClient;
import org.tron.common.overlay.discover.DiscoverServer;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.capsule.utils.MerkleTree;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadBlockException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.BadNumberBlockException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.NonCommonBlockException;
import org.tron.core.exception.ReceiptException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TransactionExpirationException;
import org.tron.core.exception.TransactionTraceException;
import org.tron.core.exception.UnLinkedBlockException;
import org.tron.core.exception.ValidateScheduleException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.http.solidity.SolidityNodeHttpApiService;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class StateCheckNode {
  private HashMap<String, Boolean> accountAddressMap = new HashMap<>();

  private DatabaseGrpcClient databaseGrpcClient;
  private Manager dbManager;

  private ScheduledExecutorService syncExecutor = Executors.newSingleThreadScheduledExecutor();

  public void setDbManager(Manager dbManager) {
    this.dbManager = dbManager;
  }

  private void initGrpcClient(String addr) {
    try {
      databaseGrpcClient = new DatabaseGrpcClient(addr);
    } catch (Exception e) {
      logger.error("Failed to create database grpc client {}", addr);
      System.exit(0);
    }
  }

  private void shutdownGrpcClient() {
    if (databaseGrpcClient != null) {
      databaseGrpcClient.shutdown();
    }
  }

  private void syncLoop(Args args) {
//    while (true) {
//      try {
//        initGrpcClient(args.getTrustNodeAddr());
//        syncSolidityBlock();
//        shutdownGrpcClient();
//      } catch (Exception e) {
//        logger.error("Error in sync solidity block " + e.getMessage(), e);
//      }
//      try {
//        Thread.sleep(5000);
//      } catch (InterruptedException e) {
//        Thread.currentThread().interrupt();
//        e.printStackTrace();
//      }
//    }
  }

  private void syncSolidityBlock() throws BadBlockException {

    accountAddressMap.put(ByteArray.toHexString(dbManager.getAccountStore().getBlackhole().getAddress().toByteArray()), true);
    accountAddressMap.put(ByteArray.toHexString(dbManager.getAccountStore().getSun().getAddress().toByteArray()), true);
    accountAddressMap.put(ByteArray.toHexString(dbManager.getAccountStore().getZion().getAddress().toByteArray()), true);

    DynamicProperties remoteDynamicProperties = databaseGrpcClient.getDynamicProperties();
    long remoteLastSolidityBlockNum = remoteDynamicProperties.getLastSolidityBlockNum();

    long startTime = System.currentTimeMillis();
    long startBlockNumber = dbManager.getDynamicPropertiesStore()
        .getLatestSolidifiedBlockNum();

    while (true) {

//      try {
//        Thread.sleep(10000);
//      } catch (Exception e) {
//
//      }
      long lastSolidityBlockNum = dbManager.getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum();
      long syncNumber = Args.getInstance().getSyncNumber();
      logger.info("sync solidity block, lastSolidityBlockNum:{}, remoteLastSolidityBlockNum:{}, syncNumber:{}",
          lastSolidityBlockNum, remoteLastSolidityBlockNum, syncNumber);

      if (syncNumber < lastSolidityBlockNum) {
        logger.warn("sync number < last solidity block number, please delete database and restart");
        System.exit(0);
      }

      if (syncNumber > remoteLastSolidityBlockNum) {
        logger.warn("sync number > remote last solidity block number, please decrease sync number and restart");
        System.exit(0);
      }

      ConsolePrint consolePrint = new ConsolePrint(lastSolidityBlockNum, syncNumber);

      if (lastSolidityBlockNum < syncNumber) {
        Block block = databaseGrpcClient.getBlock(lastSolidityBlockNum + 1);
        try {
          BlockCapsule blockCapsule = new BlockCapsule(block);
          dbManager.pushBlock(blockCapsule);
          for (TransactionCapsule trx : blockCapsule.getTransactions()) {

            byte[] owner = TransactionCapsule
                .getOwner(trx.getInstance().getRawData().getContract(0));

            byte[] toAddress = TransactionCapsule
                .getToAddress(trx.getInstance().getRawData().getContract(0));

            accountAddressMap.put(ByteArray.toHexString(owner), true);
            accountAddressMap.put(ByteArray.toHexString(toAddress), true);

            TransactionInfoCapsule ret;
            try {
              ret = dbManager.getTransactionHistoryStore().get(trx.getTransactionId().getBytes());
            } catch (BadItemException ex) {
              logger.warn("", ex);
              continue;
            }
            ret.setBlockNumber(blockCapsule.getNum());
            ret.setBlockTimeStamp(blockCapsule.getTimeStamp());
            dbManager.getTransactionHistoryStore().put(trx.getTransactionId().getBytes(), ret);
          }
          dbManager.getDynamicPropertiesStore()
              .saveLatestSolidifiedBlockNum(lastSolidityBlockNum + 1);

          long currentTime = System.currentTimeMillis();

          long remainTime = (long)((currentTime - startTime) * 1.0 / (lastSolidityBlockNum - startBlockNumber) * (syncNumber - lastSolidityBlockNum));

          consolePrint.show(lastSolidityBlockNum + 1, syncNumber, currentTime - startTime, remainTime);

        } catch (AccountResourceInsufficientException e) {
          throw new BadBlockException("validate AccountResource exception");
        } catch (ValidateScheduleException e) {
          throw new BadBlockException("validate schedule exception");
        } catch (ValidateSignatureException e) {
          throw new BadBlockException("validate signature exception");
        } catch (ContractValidateException e) {
          throw new BadBlockException("ContractValidate exception");
        } catch (ContractExeException | UnLinkedBlockException e) {
          throw new BadBlockException("Contract Execute exception");
        } catch (TaposException e) {
          throw new BadBlockException("tapos exception");
        } catch (DupTransactionException e) {
          throw new BadBlockException("dup exception");
        } catch (TooBigTransactionException e) {
          throw new BadBlockException("too big exception");
        } catch (TransactionExpirationException e) {
          throw new BadBlockException("expiration exception");
        } catch (BadNumberBlockException e) {
          throw new BadBlockException("bad number exception");
        } catch (ReceiptException e) {
          throw new BadBlockException("Receipt exception");
        } catch (NonCommonBlockException e) {
          throw new BadBlockException("non common exception");
        } catch (TransactionTraceException e) {
          throw new BadBlockException("TransactionTrace Exception");
        }

      } else {
        break;
      }
    }
    logger.info("Sync with trust node completed!!!");
  }

  private void start(Args cfgArgs) {
    try {
      initGrpcClient(cfgArgs.getTrustNodeAddr());
      syncSolidityBlock();
      // 计算AccountState

      List<String> addressList = new ArrayList<>();
      accountAddressMap.entrySet().stream().forEach((v) -> {
        if (!StringUtils.isEmpty(v.getKey())) {
          addressList.add(v.getKey());
        }
      });

      Collections.sort(addressList,new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
          if(o1 == null || o2 == null){
            return -1;
          }
          if(o1.length() > o2.length()){
            return 1;
          }
          if(o1.length() < o2.length()){
            return -1;
          }
          if(o1.compareTo(o2) > 0){
            return 1;
          }
          if(o1.compareTo(o2) < 0){
            return -1;
          }
          if(o1.compareTo(o2) == 0){
            return 0;
          }
          return 0;
        }
      });

      List<Long> balanceList = new ArrayList<>();

      addressList.forEach(v -> {
        balanceList.add(dbManager.getAccountStore().get(ByteArray.fromHexString(v)).getBalance());
      });

      Vector<Sha256Hash> ids = new Vector<>();
     balanceList.stream()
          .forEach(v -> {
            ids.add(Sha256Hash.of(ByteArray.fromLong(v)));
          });

      System.out.println("Result: " + MerkleTree.getInstance().createTree(ids).getRoot().getHash());

    } catch (BadBlockException e) {
      e.printStackTrace();
    }
    shutdownGrpcClient();
    System.exit(0);
  }

  /**
   * Start the SolidityNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Solidity node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (StringUtils.isEmpty(cfgArgs.getTrustNodeAddr())) {
      logger.error("Trust node not set.");
      return;
    }
    cfgArgs.setSolidityNode(true);

    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    Application appT = ApplicationFactory.create(context);
    FullNode.shutdown(appT);

    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    //http
    SolidityNodeHttpApiService httpApiService = context.getBean(SolidityNodeHttpApiService.class);
    appT.addService(httpApiService);

    appT.initServices(cfgArgs);
    appT.startServices();
//    appT.startup();

    //Disable peer discovery for solidity node
    DiscoverServer discoverServer = context.getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = context.getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = context.getBean(NodeManager.class);
    nodeManager.close();

    StateCheckNode node = new StateCheckNode();
    node.setDbManager(appT.getDbManager());

    node.start(cfgArgs);

    rpcApiService.blockUntilShutdown();
  }

  class ConsolePrint {
    private long startBlockNumber;
    private long endBlockNumber;
    private long currentBlockNumber;
    private long tipsLength = 100;
    private char showTips = '>';
    private char hiddenTips = '-';
    private DecimalFormat formater = new DecimalFormat("0.00%");

    public ConsolePrint(long startBlockNumber, long endBlockNumber) {
      this.startBlockNumber = startBlockNumber;
      this.endBlockNumber = endBlockNumber;
    }

    public void show(long value, long total, long useTime, long remainTime) {
      if (value < startBlockNumber || value > endBlockNumber) {
        return;
      }

      System.out.print('\r');
      currentBlockNumber = value;
      float rate = (float) (currentBlockNumber * 1.0 / endBlockNumber);
      long len = (long) (rate * tipsLength);
      draw(len, rate, value, total, useTime, remainTime);
      if (currentBlockNumber == endBlockNumber) {
        System.out.println();
      }
    }

    private void draw(long len, float rate, long value, long total, long useTime, long remainTime) {
      System.out.print("Progress: ");
      for (int i = 0; i < len; i++) {
        System.out.print(showTips);
      }

      for (long i = len; i < tipsLength; i++) {
        System.out.print(hiddenTips);
      }

      System.out.print(' ');
      System.out.print(formater.format(rate));
      System.out.print(" Current: " + value);
      System.out.print(" Total: " + total);
      System.out.print(" Time: " + (useTime / 1000) + "s");
      System.out.print(" Remain: " + (remainTime / 1000) + "s");
    }
  }
}
