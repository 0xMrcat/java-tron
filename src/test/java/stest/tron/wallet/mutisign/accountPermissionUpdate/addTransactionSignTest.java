package stest.tron.wallet.mutisign.accountPermissionUpdate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.ENOUGH_PERMISSION;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.NOT_ENOUGH_PERMISSION;
import static org.tron.api.GrpcAPI.TransactionSignWeight.Result.response_code.PERMISSION_ERROR;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class addTransactionSignTest {

    private final String testKey002 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

    private final String witnessKey001 = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

    private final String contractTRONdiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    private ECKey ecKey2 = new ECKey(Utils.getRandom());
    private byte[] normalAddr001 = ecKey2.getAddress();
    private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    private ECKey tmpECKey01 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr01 = tmpECKey01.getAddress();
    private String tmpKey01 = ByteArray.toHexString(tmpECKey01.getPrivKeyBytes());

    private ECKey tmpECKey02 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr02 = tmpECKey02.getAddress();
    private String tmpKey02 = ByteArray.toHexString(tmpECKey02.getPrivKeyBytes());

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private String fullnode = Configuration.getByPath("testng.conf")
        .getStringList("fullnode.ip.list").get(0);
    private long maxFeeLimit = Configuration.getByPath("testng.conf")
        .getLong("defaultParameter.maxFeeLimit");

    private static final long now = System.currentTimeMillis();
    private static String tokenName = "testAssetIssue_" + Long.toString(now);
    private static ByteString assetAccountId = null;
    private static final long TotalSupply = 1000L;
    private byte[] transferTokenContractAddress = null;

    private String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
    private String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");

    private static final String AVAILABLE_OPERATION = "7fff1fc0037e0000000000000000000000000000000000000000000000000000";
    private static final String DEFAULT_OPERATION = "7fff1fc0033e0000000000000000000000000000000000000000000000000000";


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
      PublicMethed.sendcoin(ownerAddress, 10_000_000, fromAddress, testKey002, blockingStubFull);
    }

    private List<String> getStrings(byte[] data){
      int index = 0;
      List<String> ret = new ArrayList<>();
      while(index < data.length){
        ret.add(byte2HexStr(data, index, 32));
        index += 32;
      }
      return ret;
    }

    public static String byte2HexStr(byte[] b, int offset, int length) {
      String stmp="";
      StringBuilder sb = new StringBuilder("");
      for (int n= offset; n<offset + length && n < b.length; n++) {
        stmp = Integer.toHexString(b[n] & 0xFF);
        sb.append((stmp.length()==1)? "0"+stmp : stmp);
      }
      return sb.toString().toUpperCase().trim();
    }

  @Test
  public void test01BroadcastMultiSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
        + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
        + "\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);
    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress, blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign.addTransactionSignWithPermissionId(
        transaction, tmpKey02, 2, blockingStubFull);

    Transaction transaction2 = PublicMethedForMutiSign.addTransactionSignWithPermissionId(
        transaction1, witnessKey001, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));

    PublicMethedForMutiSign.recoverAccountPermission(ownerKey, ownerPermissionKeys, blockingStubFull);

    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);
  }


  @Test
  public void test02BroadcastMultiSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.AccountPermissionUpdateContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
        + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
        + "\"operations\":\""+ operations +"\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
        + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction2, blockingStubFull));
  }

  @Test
  public void test03BroadcastSingleSignNormalTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    Integer[] ints = {ContractType.TransferContract_VALUE};
    String operations = PublicMethedForMutiSign.getOperations(ints);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\""+ operations +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }

  @Test (enabled = false)
  public void test04BroadcastNotSignPermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\""+ AVAILABLE_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());


    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    TransactionSignWeight txWeight = PublicMethedForMutiSign.getTransactionSignWeight(
        transaction, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction, blockingStubFull));
  }

  @Test (enabled = true)
  public void test05BroadcastMultiSignNotCompletePermissionTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\""+ AVAILABLE_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Transaction transaction = PublicMethedForMutiSign.accountPermissionUpdateWithoutSign(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }

  @Test
  public void test06BroadcastSignFailedTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\""+ AVAILABLE_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, testKey002, 2, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }

  @Test
  public void test07BroadcastTimeoutTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\""+ AVAILABLE_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    try {
      Thread.sleep(70000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    TransactionSignWeight txWeight = PublicMethedForMutiSign.getTransactionSignWeight(
        transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }

  @Test
  public void test08BroadcastEmptyTransaction() {

    PublicMethed.printAddress(ownerKey);

    logger.info("** created an empty transaction");

    Contract.AccountPermissionUpdateContract.Builder builder =
        Contract.AccountPermissionUpdateContract.newBuilder();

    Contract.AccountPermissionUpdateContract contract = builder.build();
    TransactionExtention transactionExtention =
        blockingStubFull.accountPermissionUpdate(contract);
    Transaction transaction = transactionExtention.getTransaction();

    Transaction transaction1 = PublicMethed
        .addTransactionSign(transaction, ownerKey, blockingStubFull);

    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("TransactionSignWeight info : " + txWeight);

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }

  @Test
  public void test09BroadcastErrorTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);
    activePermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\""+ AVAILABLE_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());


    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    ownerPermissionKeys.add(testKey002);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a fake transaction");
    Transaction transaction = createFakeTransaction(ownerAddress, 1_000_000L, ownerAddress);
    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign
        .getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast permission TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(transaction1, blockingStubFull));
  }


  @Test
  public void test10BroadcastMultiSignNormalTransactionWithMixOrder() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\""+ DEFAULT_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(3, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight = PublicMethedForMutiSign.getTransactionSignWeight(
        transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(
        transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast1 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Transaction transaction3 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction2, witnessKey001, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction3.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction3, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(3, txWeight.getCurrentWeight());

    Assert.assertTrue(PublicMethedForMutiSign.broadcastTransaction(transaction3, blockingStubFull));

  }

  @Test
  public void test11BroadcastMultiSignNormalTransactionBySameAccount() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\""+ DEFAULT_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(3, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction, tmpKey02, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast1 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(1, txWeight.getCurrentWeight());

    Transaction transaction2 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction1, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction2.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction2, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(2, txWeight.getCurrentWeight());

    Transaction transaction3 = PublicMethedForMutiSign
        .addTransactionSignWithPermissionId(transaction2, ownerKey, 2, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction3.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction3, blockingStubFull);
    logger.info("Before broadcast2 TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("has signed twice!"));

    Assert.assertFalse(PublicMethedForMutiSign.broadcastTransaction(
        transaction3, blockingStubFull));
  }

  @Test
  public void test12BroadcastMultiSignNormalTransactionByNullKey() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000,
        fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner1\",\"threshold\":5,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":2},"
            + "{\"address\":\"" + PublicMethed.getAddressString(testKey002) + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":3,"
            + "\"operations\":\""+ DEFAULT_OPERATION +"\",\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.add(testKey002);

    Assert.assertEquals(3, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(2, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission());

    logger.info("** trigger a normal transaction");
    Transaction transaction = PublicMethedForMutiSign
        .sendcoin2(fromAddress, 1000_000, ownerAddress, ownerKey, blockingStubFull);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction.toByteArray()));
    TransactionSignWeight txWeight =
        PublicMethedForMutiSign.getTransactionSignWeight(transaction, blockingStubFull);
    logger.info("Before Sign TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(NOT_ENOUGH_PERMISSION, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());

    Transaction transaction1 = null;
    boolean ret = false;
    try {
      transaction1 = PublicMethedForMutiSign
          .addTransactionSignWithPermissionId(transaction, null, 2, blockingStubFull);
    } catch (NullPointerException e){
      logger.info("java.lang.NullPointerException");
      ret = true;
    }
    Assert.assertTrue(ret);

    ret = false;
    try {
      transaction1 = PublicMethedForMutiSign
          .addTransactionSignWithPermissionId(transaction, "", 2, blockingStubFull);
    } catch (NumberFormatException e){
      logger.info("NumberFormatException: Zero length BigInteger");
      ret = true;
    } catch (NullPointerException e){
      logger.info("NullPointerException");
      ret = true;
    }
    Assert.assertTrue(ret);

    ret = false;
    try {
      transaction1 = PublicMethedForMutiSign
          .addTransactionSignWithPermissionId(transaction, "abcd1234", 2, blockingStubFull);
    } catch (Exception e){
      logger.info("Exception!!");
      ret = true;
    }
    Assert.assertFalse(ret);

    logger.info("transaction hex string is " + ByteArray.toHexString(transaction1.toByteArray()));
    txWeight = PublicMethedForMutiSign.getTransactionSignWeight(transaction1, blockingStubFull);
    logger.info("Before broadcast TransactionSignWeight info :\n" + txWeight);
    Assert.assertEquals(PERMISSION_ERROR, txWeight.getResult().getCode());
    Assert.assertEquals(0, txWeight.getCurrentWeight());
    Assert.assertThat(txWeight.getResult().getMessage(),
        containsString("but it is not contained of permission"));
  }


  public Protocol.Transaction createFakeTransaction(byte[] toAddrss, Long amount, byte[] fromAddress){

    Contract.TransferContract contract = Contract.TransferContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(fromAddress))
        .setToAddress(ByteString.copyFrom(toAddrss))
        .setAmount(amount)
        .build();
    Protocol.Transaction transaction = createTransaction(contract, ContractType.TransferContract);

    return transaction;
  }

  private Transaction setReference(Transaction transaction, long blockNum,
      byte[] blockHash) {
    byte[] refBlockNum = ByteArray.fromLong(blockNum);
    Transaction.raw rawData = transaction.getRawData().toBuilder()
        .setRefBlockHash(ByteString.copyFrom(blockHash))
        .setRefBlockBytes(ByteString.copyFrom(refBlockNum))
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public Transaction setExpiration(Transaction transaction, long expiration) {
    Transaction.raw rawData = transaction.getRawData().toBuilder().setExpiration(expiration)
        .build();
    return transaction.toBuilder().setRawData(rawData).build();
  }

  public Transaction createTransaction(com.google.protobuf.Message message,
      ContractType contractType) {
    Transaction.raw.Builder transactionBuilder = Transaction.raw.newBuilder().addContract(
        Transaction.Contract.newBuilder().setType(contractType).setParameter(
            Any.pack(message)).build());

    Transaction transaction = Transaction.newBuilder().setRawData(transactionBuilder.build())
        .build();

    long time = System.currentTimeMillis();
    AtomicLong count = new AtomicLong();
    long gTime = count.incrementAndGet() + time;
    String ref = "" + gTime;

    transaction = setReference(transaction, gTime, ByteArray.fromString(ref));

    transaction = setExpiration(transaction, gTime);

    return transaction;
  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
