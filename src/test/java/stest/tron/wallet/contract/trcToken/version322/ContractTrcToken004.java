package stest.tron.wallet.contract.trcToken.version322;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class   ContractTrcToken004 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountDev = null;
  private static ByteString assetAccountUser = null;
  private static final long TotalSupply = 1000L;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(dev001Key);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 1100_000_000L, fromAddress,
        testKey002, blockingStubFull));
  }

  @AfterClass(enabled = true)
  public void afterClass() {

    Assert.assertTrue(PublicMethed.unFreezeBalance(fromAddress, testKey002, 1,
        dev001Address, blockingStubFull));
    Assert.assertTrue(PublicMethed.unFreezeBalance(fromAddress, testKey002, 0,
        dev001Address, blockingStubFull));
//    Assert.assertTrue(PublicMethed.unFreezeBalance(fromAddress, testKey002, 1,
//        user001Address, blockingStubFull));
  }


  public static long getFreezeBalanceCount(byte[] accountAddress, String ecKey, Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull, String msg) {
    if(msg != null) {
      logger.info(msg);
    }
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);

    Account info = PublicMethed.queryAccount(accountAddress, blockingStubFull);

    Account getAccount = PublicMethed.queryAccount(ecKey, blockingStubFull);

    long balance = info.getBalance();
    long frozenBalance = info.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long totalEnergyLimit = resourceInfo.getTotalEnergyLimit();
    long totalEnergyWeight = resourceInfo.getTotalEnergyWeight();
    long energyUsed = resourceInfo.getEnergyUsed();
    long energyLimit = resourceInfo.getEnergyLimit();

    logger.info("Balance:" + balance);
    logger.info("frozenBalance: " + frozenBalance);
    logger.info("totalEnergyLimit: " + totalEnergyLimit);
    logger.info("totalEnergyWeight: " + totalEnergyWeight);
    logger.info("energyUsed: " + energyUsed);
    logger.info("energyLimit: " + energyLimit);

    if (energyUsed > energyLimit) {
      targetEnergy = energyUsed - energyLimit + targetEnergy;
    }

    logger.info("targetEnergy: " + targetEnergy);
    if (totalEnergyWeight == 0) {
      return 1000_000L;
    }

    // totalEnergyLimit / (totalEnergyWeight + needBalance) = needEnergy / needBalance
    BigInteger totalEnergyWeightBI = BigInteger.valueOf(totalEnergyWeight);
    long needBalance = totalEnergyWeightBI.multiply(BigInteger.valueOf(1_000_000))
        .multiply(BigInteger.valueOf(targetEnergy))
        .divide(BigInteger.valueOf(totalEnergyLimit - targetEnergy)).longValue();

    logger.info("[Debug]getFreezeBalanceCount, needBalance: " + needBalance);

    if (needBalance < 1000000L) {
      needBalance = 1000000L;
      logger.info("[Debug]getFreezeBalanceCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }
    return needBalance;
  }

  public static Long getAssetIssueValue(byte[] dev001Address, ByteString assetIssueId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long assetIssueCount = 0L;
    Account contractAccount = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
    for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
      if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
        assetIssueCount = entry.getValue();
      }
    }
    return assetIssueCount;
  }

  private ByteString testCreateAssetIssue(byte[] accountAddress, String priKey) {
    ByteString assetAccountId = null;
    ByteString addressBS1 = ByteString.copyFrom(accountAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {

      long start = System.currentTimeMillis() + 2000;
      long end = System.currentTimeMillis() + 1000000000;

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(accountAddress, tokenName, TotalSupply, 1,
          10000, start, end, 1, description, url, 100000L,100000L,
          1L,1L, priKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(accountAddress,blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

      logger.info("The token name: " + tokenName);
      logger.info("The token ID: " + assetAccountId.toStringUtf8());

    } else {
      logger.info("This account already create an assetisue");
      Optional<AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
    return  assetAccountId;
  }

  @Test
  public void deployTransferTokenContract() {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 50000L, blockingStubFull, null),
        0, 1, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    assetAccountUser = testCreateAssetIssue(user001Address, user001Key);
    assetAccountDev = testCreateAssetIssue(dev001Address, dev001Key);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
    Long userAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before dev has AssetId: " + assetAccountDev.toStringUtf8() +
        ", devAssetCountBefore: " + devAssetCountBefore);
    logger.info("before dev has AssetId: " + assetAccountUser.toStringUtf8() +
        ", userAssetCountBefore: " + userAssetCountBefore);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    String contractName = "transferTokenContract";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c01000000"
        + "000000000000000000000000000000000000000000000000006000350416633be9ece781146043575b600080"
        + "fd5b606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b604051"
        + "73ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8a"
        + "d094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a723058200ba246bdb58b"
        + "e0f221ad07e1b19de843ab541150b329ddd01558c2f1cefe1e270029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},"
        + "{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],"
        + "\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"constructor\"}]";

    // the tokenId is not exist
    String fakeTokenId = assetAccountDev.toStringUtf8();
    Long fakeTokenValue = devAssetCountBefore + 100;

    GrpcAPI.Return response = PublicMethed
        .deployContractAndGetResponse(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key,
            dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : assetBalance is not sufficient.",
        response.getMessage().toStringUtf8());

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
    Long userAssetCountAfter = getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after dev has AssetId: " + assetAccountDev.toStringUtf8() +
        ", devAssetCountAfter: " + devAssetCountAfter);
    logger.info("after user has AssetId: " + assetAccountDev.toStringUtf8() +
        ", userAssetCountAfter: " + userAssetCountAfter);

//    Assert.assertTrue(energyLimit > 0);
//    Assert.assertTrue(energyUsage == 0);
//    Assert.assertEquals(balanceBefore, balanceAfter);
    Assert.assertEquals(devAssetCountBefore, devAssetCountAfter);
    Assert.assertEquals(userAssetCountBefore, userAssetCountAfter);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


